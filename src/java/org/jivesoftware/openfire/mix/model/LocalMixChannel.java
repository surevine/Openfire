package org.jivesoftware.openfire.mix.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.tree.BaseElement;
import org.dom4j.tree.DefaultElement;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.commands.AdHocCommandManager;
import org.jivesoftware.openfire.entitycaps.EntityCapabilities;
import org.jivesoftware.openfire.entitycaps.EntityCapabilitiesManager;
import org.jivesoftware.openfire.mix.MixChannelNode;
import org.jivesoftware.openfire.mix.MixManager;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.MixPersistenceManager;
import org.jivesoftware.openfire.mix.MixService;
import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityMode;
import org.jivesoftware.openfire.mix.exception.CannotJoinMixChannelException;
import org.jivesoftware.openfire.mix.exception.CannotLeaveMixChannelException;
import org.jivesoftware.openfire.mix.exception.CannotUpdateMixChannelSubscriptionException;
import org.jivesoftware.openfire.mix.model.MixChannel.MixChannelParticipantsListener;
import org.jivesoftware.openfire.mix.policy.AlwaysAllowPermissionPolicy;
import org.jivesoftware.openfire.mix.policy.MixChannelJidMapNodeItemPermissionPolicy;
import org.jivesoftware.openfire.mix.policy.MixChannelStandardPermissionPolicy;
import org.jivesoftware.openfire.mix.policy.PermissionPolicy;
import org.jivesoftware.openfire.pubsub.CollectionNode;
import org.jivesoftware.openfire.pubsub.DefaultNodeConfiguration;
import org.jivesoftware.openfire.pubsub.LeafNode;
import org.jivesoftware.openfire.pubsub.Node;
import org.jivesoftware.openfire.pubsub.NodeSubscription;
import org.jivesoftware.openfire.pubsub.NodeSubscription.State;
import org.jivesoftware.openfire.pubsub.PubSubPersistenceManager;
import org.jivesoftware.openfire.pubsub.PubSubService;
import org.jivesoftware.openfire.pubsub.PublishedItem;
import org.jivesoftware.openfire.pubsub.models.AccessModel;
import org.jivesoftware.openfire.pubsub.models.PublisherModel;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketExtension;

public class LocalMixChannel implements MixChannel, PubSubService {
	private static final Logger LOG = LoggerFactory.getLogger(LocalMixChannel.class);

	public static final String NODE_PARTICIPANTS = "urn:xmpp:mix:nodes:participants";
	public static final String NODE_MESSAGES = "urn:xmpp:mix:nodes:messages";	
	public static final String NODE_JIDMAP = "urn:xmpp:mix:nodes:jidmap";

	private PacketRouter packetRouter;
	
	private Map<JID, MixChannelParticipant> participantsByRealJID;

	private Map<JID, MixChannelParticipant> participantsByProxyJid;

	private List<MixChannelParticipantsListener> participantsListeners;

	private Long id;

    /**
     * Used to handle filtered-notifications.
     */
    private EntityCapabilitiesManager entityCapsManager = EntityCapabilitiesManager.getInstance();
	
	/**
	 * The {@link PermissionPolicy} to apply to this channel
	 */
	private PermissionPolicy<MixChannel> permissionPolicy;

	/**
	 * This {@link MixService} to which this channel is attached.
	 */
	private MixService mixService;
	
	private MixPersistenceManager channelRepository;

	/**
	 * The name of the channel.
	 */
	private String name;

	/**
	 * The date when the channel was created.
	 */
	private Date creationDate;

	// Default to visible
	private ChannelJidVisibilityMode jidVisibilityMode = ChannelJidVisibilityMode.VISIBLE;

	private JID owner;
	
	/**
     * Collection node that acts as the root node of the entire node hierarchy.
     */
    private CollectionNode rootCollectionNode = null;
    
	/**
	 * Constructor to be used when serialising from the database.  The id on the constructor is key as it will be known when constructing as part of a query.
	 * @param id
	 * @param service
	 * @param name
	 * @param owner
	 * @param packetRouter
	 * @param mpm
	 * @param creationDate
	 * @throws MixPersistenceException 
	 */
	public LocalMixChannel(long id, MixService service, String name, JID owner, PacketRouter packetRouter, MixPersistenceManager mpm, Date creationDate) throws MixPersistenceException {
		initialise(service, name, owner, packetRouter, mpm);
		
		this.id = id;
		this.creationDate = new Date(creationDate.getTime());
		
		Collection<MixChannelParticipant> fromDB = mpm.findByChannel(this);
		
		for (MixChannelParticipant mcp : fromDB) {
			mcp.setSubscriptions(mpm.findByParticipant(mcp));
			participantsByRealJID.put(mcp.getRealJid(), mcp);
			participantsByProxyJid.put(mcp.getJid(), mcp);
		}
		
	}

	public LocalMixChannel(MixService service, String name, JID owner, PacketRouter packetRouter, MixPersistenceManager mpm) {
		initialise(service, name, owner, packetRouter, mpm);

		this.setCreationDate(new Date());
	}

	private void initialise(MixService service, String name, JID owner, PacketRouter packetRouter,
			MixPersistenceManager mpm) {
		this.packetRouter = packetRouter;
		this.mixService = service;
		this.channelRepository = mpm;
		this.owner = owner;
		this.name = name;
		
		this.participantsListeners = new ArrayList<>();
		this.participantsByRealJID = new HashMap<>();
		this.participantsByProxyJid = new HashMap<>();

		this.setCreationDate(new Date());
		
        // Create root collection node
        rootCollectionNode = new CollectionNode(this, null, "/", getJID());
        // Add the creator as the node owner
        rootCollectionNode.addOwner(getJID());
        // Save new root node
        rootCollectionNode.saveToDB();

        xnodes.put("/", rootCollectionNode);
		
        MixChannelParticipantsNode participantsNode = new MixChannelParticipantsNode(this);      
        xnodes.put(MixChannelParticipantsNode.NODE_ID, participantsNode);
        
        MixChannelJidMapNode jidMapNode = new MixChannelJidMapNode(this);      
        xnodes.put(MixChannelJidMapNode.NODE_ID, jidMapNode);

        MixChannelMessagesNode messagesNode = new MixChannelMessagesNode(this);      
        xnodes.put(MixChannelMessagesNode.NODE_ID, messagesNode);

	}
/*
	private void setupNodes() {
		nodes.put(NODE_PARTICIPANTS, new MixChannelNodeImpl<>(packetRouter, this, NODE_PARTICIPANTS,
				new MixChannelParticipantsNodeItemsProvider(this)));

		nodes.put(NODE_MESSAGES, new MixChannelNodeImpl<>(packetRouter, this, NODE_MESSAGES,
				null));

		nodes.put(NODE_JIDMAP, new MixChannelNodeImpl<>(packetRouter, this, NODE_JIDMAP,
				new MixChannelJidMapNodeItemsProvider(this), new MixChannelJidMapNodeItemPermissionPolicy(), new AlwaysAllowPermissionPolicy<MixChannelNode<MixChannelJidMapNodeItem>>()));
	}
*/	
	@Override
	public JID getJID() {
		return new JID(getName(), mixService.getServiceDomain(), null);
	}

	@Override
	public Long getID() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public JID getOwner() {
		return this.owner;
	}

	@Override
	public ChannelJidVisibilityMode getJidVisibilityMode() {
		return jidVisibilityMode;
	}

	@Override
	public Date getCreationDate() {
		return new Date(creationDate.getTime());
	}

	@Override
	public void addParticipantsListener(MixChannelParticipantsListener listener) {
		participantsListeners.add(listener);
	}

	@Override
	public MixChannelParticipant addParticipant(JID jid, Set<String> subscribeNodes) throws CannotJoinMixChannelException {

		JID bareJoinerJID = jid.asBareJID();
		
		if (!this.participantsByRealJID.containsKey(bareJoinerJID)) {
			JID proxyJid = this.getNewProxyJID();
			
			MixChannelParticipant participant = null;
			
			if (subscribeNodes.isEmpty()) {
				participant = new LocalMixChannelParticipant(proxyJid, bareJoinerJID, this, this.channelRepository);
			} else {
				participant = new LocalMixChannelParticipant(proxyJid, bareJoinerJID, this, subscribeNodes, this.channelRepository);				
			}


			this.participantsByRealJID.put(bareJoinerJID, participant);
			
			// Trigger the participant added event.
			for (MixChannelParticipantsListener listener : participantsListeners) {
				listener.onParticipantAdded(participant);
			}
			
			try {
				channelRepository.save(participant);
			} catch (MixPersistenceException e) {
				LOG.error("Persistence exception adding participant " + jid + " to " + this.getName(), e);
				throw new CannotJoinMixChannelException(this.getName(), e.getMessage());
			}
			
			for(String nodeId : participant.getSubscriptions()) {
				Node node = xnodes.get(nodeId);
				node.addSubscription(new NodeSubscription(node, jid.asBareJID(), getJID(), State.subscribed, participant.getJid().toBareJID()));
			}
			
			return participant;
		} else {
			throw new CannotJoinMixChannelException(this.getName(), "Already a member");
		}
	}
	

	@Override
	public void updateSubscriptions(JID from, Set<String> subscriptionRequests)
			throws CannotUpdateMixChannelSubscriptionException {
		JID requestorBareJid = from.asBareJID();
		
		if (participantsByRealJID.containsKey(requestorBareJid)) {
			MixChannelParticipant participant = participantsByRealJID.get(requestorBareJid);
			participant.updateSubscriptions(subscriptionRequests);
		} else {
			throw new CannotUpdateMixChannelSubscriptionException(this.getName(), "Not a participant");
		}
		
	}
	
	@Override
	public void removeParticipant(JID jid) throws CannotLeaveMixChannelException {

		JID bareJid = jid.asBareJID();
		
		if (participantsByRealJID.containsKey(bareJid)) {
			MixChannelParticipant mcp = participantsByRealJID.remove(bareJid);
			participantsByProxyJid.remove(mcp.getJid());
			
			// Let all listeners know that the participant has left
			for (MixChannelParticipantsListener listener : participantsListeners) {
				listener.onParticipantRemoved(mcp);
			}
			try {
				this.channelRepository.delete(mcp);
				this.participantsByRealJID.remove(bareJid);
			} catch (MixPersistenceException e) {
				LOG.error(e.getMessage());
				throw new CannotLeaveMixChannelException(this.getName(), e.getMessage());
			}
		} else {
			throw new CannotLeaveMixChannelException(this.getName(), "Not a participant");
		}
		
		
		return;
	}
	

	private JID getNewProxyJID() {
		return new JID(this.name + "+" + this.getNextProxyNodePart(), this.getJID().getDomain(), "", false);
	}

	public void setID(long newID) {
		this.id = newID;
	}

	public void setCreationDate(Date date) {
		this.creationDate = new Date(date.getTime());
	}
/*	
	@Override
	public Collection<MixChannelNode<? extends MixChannelNodeItem>> getNodes() {
		return Collections.unmodifiableCollection(nodes.values());
	}
*/
	
	@Override
	public Set<String> getNodesAsStrings() {
		return xnodes.keySet();
	}

	private int proxyNodeNamePart = 0;

	private String getNextProxyNodePart() {
		// TODO - temporary implementation
		return Integer.toString(proxyNodeNamePart++);
	}

	@Override
	public MixChannelParticipant getParticipantByRealJID(JID from) {
		return participantsByRealJID.get(from.asBareJID());
	}

	@Override
	public MixChannelParticipant getParticipantByProxyJID(JID jid) {
		return participantsByProxyJid.get(jid.asBareJID());
	}	

	@Override
	public Collection<MixChannelParticipant> getParticipants() {
		return Collections.unmodifiableCollection(participantsByRealJID.values());
	}

	@Override
	public void receiveMessage(MixChannelMessage mcMessage) {
		Set<MixChannelParticipant> subscribers = getNodeSubscribers(NODE_MESSAGES);
		
		MixChannelParticipant sender = mcMessage.getSender();
		
		Message templateMessage = mcMessage.getMessage().createCopy();
		templateMessage.setFrom(getJID()); // Message is from the channel
		templateMessage.setID(mcMessage.getId());
		templateMessage.addChildElement("nick", MixManager.MIX_NAMESPACE_STR).addText(sender.getNick());
		templateMessage.addChildElement("jid", MixManager.MIX_NAMESPACE_STR).addText(sender.getJid().toBareJID());
		
		for(MixChannelParticipant subscriber : subscribers) {
			Message thisMessage = templateMessage.createCopy();
			thisMessage.setTo(subscriber.getRealJid());
			
			if(subscriber.equals(sender)) {
				thisMessage.addChildElement("submission-id", MixManager.MIX_NAMESPACE_STR).addText(mcMessage.getMessage().getID());
			}
			
			packetRouter.route(thisMessage);
		}
		
		for (MixChannelParticipantsListener listener : participantsListeners) {
			listener.onMessageReceived(mcMessage);
		}		
	}
	
	public Set<MixChannelParticipant> getNodeSubscribers(String node) {
		Collection<MixChannelParticipant> allParticipants = participantsByRealJID.values();

		if (!xnodes.containsKey(node)) {
			return Collections.emptySet();
		} else {
			Set<MixChannelParticipant> subscribers = new HashSet<>();
			
			for (MixChannelParticipant mcp : allParticipants) {
				if (mcp.subscribesTo(node)) {
					subscribers.add(mcp);
				}
			}
			return subscribers;
		}
	}

	public void setName(String string) {
		this.name = string;
	}

	public void setJidVisibilityMode(ChannelJidVisibilityMode jidVisibilityMode) {
		this.jidVisibilityMode = jidVisibilityMode;
	}

	/**
	 * Cascade deletion of channel participants
	 * @throws MixPersistenceException 
	 * 
	 * @see org.jivesoftware.openfire.mix.model.MixChannel#destroy()
	 */
	@Override
	public void destroy() throws MixPersistenceException {
		for (MixChannelParticipant participant : participantsByRealJID.values()) {
			this.channelRepository.delete(participant);	
		}
		
	}

	@Override
	public boolean isDestructable(JID requestor) {
		return this.getOwner().toBareJID().equals(requestor.toBareJID()) && this.getParticipants().size() == 1 ? true : false;

	}

	@Override
	public MixService getMixService() {
		return mixService;
	}

	@Override
	public MixChannelNode<?> getNodeByName(String nodeName) {
		return null;
	}

	private Map<String, Node> xnodes = new ConcurrentHashMap<>();
	
	@Override
	public JID getAddress() {
		return getJID();
	}

	@Override
	public String getServiceID() {
		return getJID().toBareJID();
	}

	@Override
	public Map<String, Map<String, String>> getBarePresences() {
		return null;
	}

	@Override
	public boolean canCreateNode(JID creator) {
		return false;
	}

	@Override
	public boolean isServiceAdmin(JID user) {
		// TODO Auto-generated method stub
		return user.asBareJID().equals(owner.toBareJID());
	}

	@Override
	public boolean isInstantNodeSupported() {
		return false;
	}

	@Override
	public boolean isCollectionNodesSupported() {
		return false;
	}

	@Override
	public CollectionNode getRootCollectionNode() {
		return null;
	}

	@Override
	public Node getNode(String nodeID) {
		return xnodes.get(nodeID);
	}

	@Override
	public void addNode(Node node) {
		xnodes.put(node.getNodeID(), node);
	}

	@Override
	public void removeNode(String nodeID) {
		xnodes.remove(nodeID);
	}

	@Override
	public void broadcast(Node node, Message msg, Collection<JID> jids) {
        for (JID jid : jids) {
        	Message message = msg.createCopy();
        	message.setFrom(getAddress());
            message.setTo(jid);
            message.setID(UUID.randomUUID().toString());
            packetRouter.route(message);
        }
	}

	@Override
	public void send(Packet packet) {
        packetRouter.route(packet);
	}

	@Override
	public void sendNotification(Node node, Message msg, JID recipientJID) {
		Message message = msg.createCopy();
		
        message.setTo(recipientJID);
        message.setFrom(getAddress());
        message.setID(UUID.randomUUID().toString());

        packetRouter.route(message);
	}

 	@Override
	public DefaultNodeConfiguration getDefaultNodeConfiguration(boolean leafType) {
		if(leafType) {
			DefaultNodeConfiguration leafDefaultConfiguration = new DefaultNodeConfiguration(true);
            leafDefaultConfiguration.setAccessModel(AccessModel.open);
            leafDefaultConfiguration.setPublisherModel(PublisherModel.publishers);
            leafDefaultConfiguration.setDeliverPayloads(true);
            leafDefaultConfiguration.setLanguage("English");
            leafDefaultConfiguration.setMaxPayloadSize(5120);
            leafDefaultConfiguration.setNotifyConfigChanges(true);
            leafDefaultConfiguration.setNotifyDelete(true);
            leafDefaultConfiguration.setNotifyRetract(true);
            leafDefaultConfiguration.setPersistPublishedItems(false);
            leafDefaultConfiguration.setMaxPublishedItems(1);
            leafDefaultConfiguration.setPresenceBasedDelivery(false);
            leafDefaultConfiguration.setSendItemSubscribe(true);
            leafDefaultConfiguration.setSubscriptionEnabled(true);
            leafDefaultConfiguration.setReplyPolicy(null);
            return leafDefaultConfiguration;
		} else {
			DefaultNodeConfiguration collectionDefaultConfiguration = new DefaultNodeConfiguration(false);
            collectionDefaultConfiguration.setAccessModel(AccessModel.open);
            collectionDefaultConfiguration.setPublisherModel(PublisherModel.publishers);
            collectionDefaultConfiguration.setDeliverPayloads(false);
            collectionDefaultConfiguration.setLanguage("English");
            collectionDefaultConfiguration.setNotifyConfigChanges(true);
            collectionDefaultConfiguration.setNotifyDelete(true);
            collectionDefaultConfiguration.setNotifyRetract(true);
            collectionDefaultConfiguration.setPresenceBasedDelivery(false);
            collectionDefaultConfiguration.setSubscriptionEnabled(true);
            collectionDefaultConfiguration.setReplyPolicy(null);
            collectionDefaultConfiguration.setAssociationPolicy(CollectionNode.LeafNodeAssociationPolicy.all);
            collectionDefaultConfiguration.setMaxLeafNodes(-1);
            return collectionDefaultConfiguration;
		}
	}

	@Override
	public Collection<String> getShowPresences(JID subscriber) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void presenceSubscriptionRequired(Node node, JID user) {
		// TODO Auto-generated method stub
	}

	@Override
	public void presenceSubscriptionNotRequired(Node node, JID user) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isMultipleSubscriptionsEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AdHocCommandManager getManager() {
		// TODO Auto-generated method stub
		return null;
	}
/*
	@Override
	public Collection<MixChannelNode<? extends MixChannelNodeItem>> getNodes() {
		return nodes.values();
	}
*/

	@Override
	public Collection<Node> getNodes() {
		return xnodes.values();
	}
}
