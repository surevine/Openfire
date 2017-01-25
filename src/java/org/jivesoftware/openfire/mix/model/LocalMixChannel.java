package org.jivesoftware.openfire.mix.model;

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
import org.jivesoftware.openfire.mix.exception.CannotLeaveMixChannelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

public class LocalMixChannel implements MixChannel {

	public static final String NODE_MESSAGES = "urn:xmpp:mix:nodes:messages";
	
	private static final Logger LOG = LoggerFactory.getLogger(LocalMixChannel.class);

	private PacketRouter packetRouter;
	
	private Map<JID, MixChannelParticipant> participants;

	private Map<String, MixChannelNode> nodes;

	private List<MixChannelParticipantsListener> participantsListeners;

	private Long id;

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

		this.id = id;
		this.creationDate = new Date(creationDate.getTime());
		this.packetRouter = packetRouter;
		this.mixService = service;
		this.channelRepository = mpm;
		this.owner = owner;
		this.name = name;
		
		this.participantsListeners = new ArrayList<>();
		this.participants = new HashMap<>();
		this.nodes = new HashMap<>();
		
		// TODO these shouldn't be hardcoded
		nodes.put("urn:xmpp:mix:nodes:participants", new MixChannelNodeImpl(packetRouter, this, "urn:xmpp:mix:nodes:participants",
				new MixChannelParticipantsNodeItemsProvider(this)));

		nodes.put(NODE_MESSAGES, new MixChannelNodeImpl(packetRouter, this, NODE_MESSAGES,
				null));
		
		Collection<MixChannelParticipant> fromDB = mpm.findByChannel(this);
		
		for (MixChannelParticipant mcp : fromDB) {
			mcp.setSubscriptions(mpm.findByParticipant(mcp));
			participants.put(mcp.getJid(), mcp);
		}
		
	}

	public LocalMixChannel(MixService service, String name, JID owner, PacketRouter packetRouter, MixPersistenceManager mpm) {
		this.packetRouter = packetRouter;
		this.mixService = service;
		this.channelRepository = mpm;
		this.owner = owner;
		this.name = name;
		
		this.participantsListeners = new ArrayList<>();
		this.participants = new HashMap<>();
		this.nodes = new HashMap<>();
		
		nodes.put("urn:xmpp:mix:nodes:participants", new MixChannelNodeImpl(packetRouter, this, "urn:xmpp:mix:nodes:participants",
				new MixChannelParticipantsNodeItemsProvider(this)));

		nodes.put(NODE_MESSAGES, new MixChannelNodeImpl(packetRouter, this, NODE_MESSAGES,
				null));

		this.setCreationDate(new Date());
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
	public MixChannelParticipant addParticipant(JID jid, Set<String> subscribeNodes) throws MixPersistenceException {
		JID proxyJid = this.getNewProxyJID();
		MixChannelParticipant participant = new LocalMixChannelParticipant(proxyJid, jid, this, subscribeNodes);

		this.participants.put(jid, participant);
		
		// Trigger the participant added event.
		for (MixChannelParticipantsListener listener : participantsListeners) {
			listener.onParticipantAdded(participant);
		}
		
		channelRepository.save(participant);

		return participant;
	}
	

	/**
	 * Add the participant and subscribe to all nodes
	 * 
	 * @param owner
	 * @return
	 * @throws MixPersistenceException 
	 */
	@Override
	public MixChannelParticipant addParticipant(JID owner) throws MixPersistenceException {
		return this.addParticipant(owner, this.nodes.keySet());
	}
	
	@Override
	public void removeParticipant(JID jid) throws CannotLeaveMixChannelException {

		JID bareJid = jid.asBareJID();
		
		if (participants.containsKey(bareJid)) {
			MixChannelParticipant mcp = participants.get(bareJid);
			
			// Let all listeners know that the participant has left
			for (MixChannelParticipantsListener listener : participantsListeners) {
				listener.onParticipantRemoved(mcp);
			}
			try {
				this.channelRepository.delete(mcp);
				this.participants.remove(bareJid);
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
	
	@Override
	public Collection<MixChannelNode> getNodes() {
		return Collections.unmodifiableCollection(nodes.values());
	}

	private int proxyNodeNamePart = 0;

	private String getNextProxyNodePart() {
		// TODO - temporary implementation
		return Integer.toString(proxyNodeNamePart++);
	}

	@Override
	public MixChannelParticipant getParticipantByJID(JID from) {
		return participants.get(from);
	}
	

	@Override
	public Collection<MixChannelParticipant> getParticipants() {
		return Collections.unmodifiableCollection(participants.values());
	}

	@Override
	public void receiveMessage(MixChannelMessage mcMessage) {
		Set<MixChannelParticipant> subscribers = getNodeSubscribers(NODE_MESSAGES);
		
		MixChannelParticipant sender = mcMessage.getSender();
		
		Message templateMessage = mcMessage.getMessage().createCopy();
		templateMessage.setFrom(getJID()); // Message is from the channel
		templateMessage.setID(mcMessage.getId());
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
		Collection<MixChannelParticipant> allParticipants = participants.values();

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
		for (MixChannelParticipant participant : participants.values()) {
			this.channelRepository.delete(participant);	
		}
		
	}

	@Override
	public boolean isDestructable(JID requestor) {
		return this.getOwner().toBareJID().equals(requestor.toBareJID()) && this.getParticipants().size() == 1 ? true : false;

	}
}
