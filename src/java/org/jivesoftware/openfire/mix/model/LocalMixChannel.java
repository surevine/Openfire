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
import org.jivesoftware.openfire.mix.MixService;
import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

public class LocalMixChannel implements MixChannel {

	private static final Logger LOG = LoggerFactory.getLogger(LocalMixChannel.class);

	private Map<JID, MixChannelParticipant> participants;

	private Set<MixChannelNode> nodes;

	private List<MixChannelParticipantsListener> participantsListeners;

	private List<MixChannelMessageListener> messageListeners;

	private Long id;

	/**
	 * This {@link MixService} to which this channel is attached.
	 */
	private MixService mixService;

	/**
	 * The name of the channel.
	 */
	private String name;

	/**
	 * The date when the channel was created.
	 */
	private Date creationDate;

	private ChannelJidVisibilityMode jidVisibilityMode;

	public LocalMixChannel(MixService service, String name, PacketRouter packetRouter) {
		this.participantsListeners = new ArrayList<>();
		this.messageListeners = new ArrayList<>();
		this.participants = new HashMap<>();
		this.nodes = new HashSet<>();

		this.mixService = service;
		this.name = name;
		nodes.add(new MixChannelNodeImpl(packetRouter, this, "urn:xmpp:mix:nodes:participants",
				new MixChannelParticipantsNodeItemsProvider(this)));
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
	public ChannelJidVisibilityMode getJidVisibilityMode() {
		return jidVisibilityMode;
	}

	@Override
	public Date getCreationDate() {
		return creationDate;
	}

	@Override
	public void addParticipantsListener(MixChannelParticipantsListener listener) {
		participantsListeners.add(listener);
	}

	@Override
	public MixChannelParticipant addParticipant(JID jid, Set<String> subscribeNodes) {
		JID proxyJid = this.getNewProxyJID();
		MixChannelParticipant participant = new LocalMixChannelParticipant(proxyJid, jid, this, subscribeNodes);

		this.participants.put(jid, participant);
		
		// Trigger the participant added event.
		for (MixChannelParticipantsListener listener : participantsListeners) {
			listener.onParticipantAdded(participant);
		}

		return participant;
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
	public Set<MixChannelNode> getNodes() {
		return Collections.unmodifiableSet(nodes);
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
	public void receiveMessage(MixChannelMessage mcMessage) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addMessageListener(MixChannelMessageListener listener) {
		// TODO Auto-generated method stub
	}
	
	public Set<MixChannelParticipant> getNodeSubscribers(String node) {
		Collection<MixChannelParticipant> allParticipants = participants.values();
		
		if (!nodes.contains(node)) {
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

}
