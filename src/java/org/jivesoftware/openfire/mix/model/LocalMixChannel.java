package org.jivesoftware.openfire.mix.model;

import java.util.Date;
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

	public LocalMixChannel(PacketRouter packetRouter, MixService service, String name) {
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
		MixChannelParticipant participant = new LocalMixChannelParticipant(jid, this, subscribeNodes);

		this.participants.put(jid, participant);

		for (MixChannelParticipantsListener listener : participantsListeners) {
			listener.onParticipantAdded(participant);
		}

		return participant;
	}

	public void setID(long newID) {
		this.id = newID;
	}

	public void setCreationDate(Date date) {
		this.creationDate = new Date(date.getTime());
	}

}
