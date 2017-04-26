package org.jivesoftware.openfire.mix.model;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.mix.MixChannelNode;
import org.jivesoftware.openfire.mix.MixManager;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.MixPersistenceManager;
import org.jivesoftware.openfire.mix.MixService;
import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityMode;
import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityPreference;
import org.jivesoftware.openfire.mix.exception.CannotJoinMixChannelException;
import org.jivesoftware.openfire.mix.exception.CannotLeaveMixChannelException;
import org.jivesoftware.openfire.mix.exception.CannotUpdateMixChannelSubscriptionException;
import org.jivesoftware.openfire.mix.mam.MessageArchiveService;
import org.jivesoftware.openfire.mix.policy.AlwaysAllowPermissionPolicy;
import org.jivesoftware.openfire.mix.policy.MixChannelJidMapNodeItemPermissionPolicy;
import org.jivesoftware.openfire.mix.policy.MixChannelStandardPermissionPolicy;
import org.jivesoftware.openfire.mix.policy.PermissionPolicy;
import org.jivesoftware.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class LocalMixChannel implements MixChannel {
	private static final Logger LOG = LoggerFactory.getLogger(LocalMixChannel.class);

	public static final String NODE_PARTICIPANTS = "urn:xmpp:mix:nodes:participants";
	public static final String NODE_MESSAGES = "urn:xmpp:mix:nodes:messages";	
	public static final String NODE_JIDMAP = "urn:xmpp:mix:nodes:jidmap";

	private PacketRouter packetRouter;
	
	private Map<JID, MixChannelParticipant> participantsByRealJID;

	private Map<JID, MixChannelParticipant> participantsByProxyJid;

	private Map<String, MixChannelNode<? extends MixChannelNodeItem>> nodes;

	private List<MixChannelParticipantsListener> participantsListeners;

	private Long id;
	
	/**
	 * The {@link PermissionPolicy} to apply to this channel
	 */
	private PermissionPolicy<MixChannel> permissionPolicy;

	/**
	 * This {@link MixService} to which this channel is attached.
	 */
	private MixService mixService;
	
	private MixPersistenceManager channelRepository;

	private MessageArchiveService archive;

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
	public LocalMixChannel(long id, MixService service, String name, JID owner, PacketRouter packetRouter, MixPersistenceManager mpm, MessageArchiveService archive, Date creationDate) throws MixPersistenceException {
		initialise(service, name, owner, packetRouter, mpm, archive);
		
		this.id = id;
		this.creationDate = new Date(creationDate.getTime());
		
		Collection<MixChannelParticipant> fromDB = mpm.findByChannel(this);
		
		for (MixChannelParticipant mcp : fromDB) {
			mcp.setSubscriptions(mpm.findByParticipant(mcp));
			participantsByRealJID.put(mcp.getRealJid(), mcp);
			participantsByProxyJid.put(mcp.getJid(), mcp);
		}
		
	}

	public LocalMixChannel(MixService service, String name, JID owner, PacketRouter packetRouter, MixPersistenceManager mpm, MessageArchiveService archive) {
		initialise(service, name, owner, packetRouter, mpm, archive);

		this.setCreationDate(new Date());
	}

	private void initialise(MixService service, String name, JID owner, PacketRouter packetRouter,
							MixPersistenceManager mpm, MessageArchiveService archive) {
		this.packetRouter = packetRouter;
		this.mixService = service;
		this.channelRepository = mpm;
		this.owner = owner;
		this.name = name;
		this.archive = archive;
		
		this.participantsListeners = new ArrayList<>();
		this.participantsByRealJID = new HashMap<>();
		this.participantsByProxyJid = new HashMap<>();
		this.nodes = new HashMap<>();
		
		this.setCreationDate(new Date());
		
		setupNodes();
		setupPermissionPolicy();
	}

	private void setupNodes() {
		nodes.put(NODE_PARTICIPANTS, new MixChannelNodeImpl<>(packetRouter, this, NODE_PARTICIPANTS,
				new MixChannelParticipantsNodeItemsProvider(this)));

		nodes.put(NODE_MESSAGES, new MixChannelNodeImpl<>(packetRouter, this, NODE_MESSAGES,
				null));

		nodes.put(NODE_JIDMAP, new MixChannelNodeImpl<>(packetRouter, this, NODE_JIDMAP,
				new MixChannelJidMapNodeItemsProvider(this), new MixChannelJidMapNodeItemPermissionPolicy(), new AlwaysAllowPermissionPolicy<MixChannelNode<MixChannelJidMapNodeItem>>()));
	}
	
	private void setupPermissionPolicy() {
		permissionPolicy = new MixChannelStandardPermissionPolicy();
	}
	
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
	public MixChannelParticipant addParticipant(JID jid, Set<String> subscribeNodes, ChannelJidVisibilityPreference jidVisibilityPreference) throws CannotJoinMixChannelException {

		JID bareJoinerJID = jid.asBareJID();
		
		if (!this.participantsByRealJID.containsKey(bareJoinerJID)) {
			JID proxyJid = this.createProxyJID(bareJoinerJID.toString(), jidVisibilityPreference.getId());
			
			MixChannelParticipant participant;
			
			if (subscribeNodes.isEmpty()) {
				participant = new LocalMixChannelParticipant(proxyJid, bareJoinerJID, this, jidVisibilityPreference, this.channelRepository);
			} else {
				participant = new LocalMixChannelParticipant(proxyJid, bareJoinerJID, this, subscribeNodes, jidVisibilityPreference, this.channelRepository);
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

	private SecretKeySpec key = new SecretKeySpec("some secret".getBytes(), "HmacMD5");

	private JID createProxyJID(String userJID, Integer jidVisibilityPreference) {
	    try {
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(key);
			mac.update(this.getJID().toString().getBytes());
            mac.update(userJID.getBytes());
            mac.update(jidVisibilityPreference.toString().getBytes());

            String hash = StringUtils.encodeHex(mac.doFinal()).substring(0, 10);

		    return new JID(hash + "#" + this.getJID().getNode(), this.getJID().getDomain(), "", false);
	    } catch (NoSuchAlgorithmException e) {
	        LOG.info("Failed to create proxy jid", e);
		} catch (InvalidKeyException e) {
			LOG.info("Failed to create proxy jid", e);
		}

		throw new RuntimeException("Failed to create proxy jid");
	}

	public void setID(long newID) {
		this.id = newID;
	}

	public void setCreationDate(Date date) {
		this.creationDate = new Date(date.getTime());
	}
	
	@Override
	public Collection<MixChannelNode<? extends MixChannelNodeItem>> getNodes() {
		return Collections.unmodifiableCollection(nodes.values());
	}

	@Override
	public Set<String> getNodesAsStrings() {
		return nodes.keySet();
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

		String mamId = archive.archive(mcMessage);
		
		MixChannelParticipant sender = mcMessage.getSender();
		
		Message templateMessage = mcMessage.getMessage().createCopy();
		templateMessage.setFrom(this.getJID()); // Message is from the channel
		templateMessage.setID(mamId);
		templateMessage.addChildElement("nick", MixManager.MIX_NAMESPACE).addText(sender.getNick());
		templateMessage.addChildElement("jid", MixManager.MIX_NAMESPACE).addText(sender.getJid().toBareJID());
		
		for(MixChannelParticipant subscriber : subscribers) {
			Message thisMessage = templateMessage.createCopy();
			thisMessage.setTo(subscriber.getRealJid());
			
			if(subscriber.equals(sender)) {
				thisMessage.addChildElement("submission-id", MixManager.MIX_NAMESPACE).addText(mcMessage.getMessage().getID());
			}
			
			packetRouter.route(thisMessage);
		}
	}
	
	public Set<MixChannelParticipant> getNodeSubscribers(String node) {
		Collection<MixChannelParticipant> allParticipants = participantsByRealJID.values();

		if (!nodes.containsKey(node)) {
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
		return nodes.get(nodeName);
	}
}
