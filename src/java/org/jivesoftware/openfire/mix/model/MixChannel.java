package org.jivesoftware.openfire.mix.model;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.jivesoftware.database.JiveID;
import org.jivesoftware.openfire.mix.MixChannelNode;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityMode;
import org.jivesoftware.openfire.mix.exception.CannotLeaveMixChannelException;
import org.jivesoftware.util.JiveConstants;
import org.xmpp.packet.JID;

@JiveID(JiveConstants.MIX_CHANNEL)
public interface MixChannel {
	public interface MixChannelParticipantsListener {
		void onParticipantAdded(MixChannelParticipant participant);

		void onParticipantRemoved(MixChannelParticipant mcp);
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
	
	MixChannelParticipant addParticipant(JID jid, Set<String> subscribeNodes) throws MixPersistenceException;

	MixChannelParticipant addParticipant(JID owner) throws MixPersistenceException;
	
	void removeParticipant(JID jid) throws CannotLeaveMixChannelException;

	Collection<MixChannelNode> getNodes();
	
	Collection<MixChannelParticipant> getParticipants();

	MixChannelParticipant getParticipantByJID(JID from);
	
	Collection<MixChannelParticipant> getParticipants();

	void receiveMessage(MixChannelMessage mcMessage);
	
	Set<MixChannelParticipant> getNodeSubscribers(String node);

	void setID(long ID);

	JID getOwner();

	MixChannelNode getNodeByName(String nodeName);
	
	void destroy() throws MixPersistenceException;

	boolean isDestructable(JID requestor);


}
