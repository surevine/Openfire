package org.jivesoftware.openfire.mix.model;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.jivesoftware.database.JiveID;
import org.jivesoftware.openfire.mix.MixChannelNode;
import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityMode;
import org.jivesoftware.util.JiveConstants;
import org.xmpp.packet.JID;

@JiveID(JiveConstants.MIX_CHANNEL)
public interface MixChannel {
	public interface MixChannelParticipantsListener {
		void onParticipantAdded(MixChannelParticipant participant);
	}

	public interface MixChannelMessageListener {
		/**
		 * Triggered when a message is received by the channel
		 * @param message The received message
		 */
		void onMessageReceived(MixChannelMessage message);
	}

	/**
	 * Get the full JID of this channel.
	 *
	 * @return the JID for this channel.
	 */
	JID getJID();

	/**
	 * Obtain a unique numerical id for this channel. Useful for storing
	 * channels in databases. If the room is persistent or is logging the
	 * conversation then the returned ID won't be -1.
	 *
	 * @return The unique id for this channel or <code>null</code> if the
	 *         channel is temporary and is not logging the conversation.
	 */
	Long getID();

	String getName();
	
	ChannelJidVisibilityMode getJidVisibilityMode();

	Date getCreationDate();

	void addParticipantsListener(MixChannelParticipantsListener listener);

	void addMessageListener(MixChannelMessageListener listener);
	
	MixChannelParticipant addParticipant(JID jid, Set<String> subscribeNodes);

	Collection<MixChannelNode> getNodes();

	MixChannelParticipant getParticipantByJID(JID from);

	void receiveMessage(MixChannelMessage mcMessage);
	
	Set<MixChannelParticipant> getNodeSubscribers(String node);

	void setID(long ID);
}
