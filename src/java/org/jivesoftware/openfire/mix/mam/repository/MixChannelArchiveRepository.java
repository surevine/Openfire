package org.jivesoftware.openfire.mix.mam.repository;

import java.util.Date;
import java.util.List;

import org.jivesoftware.openfire.mix.mam.ArchivedMixChannelMessage;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

public interface MixChannelArchiveRepository {
	
	/**
	 * Principal method for persisting in the archive, as per XEP-0369, 5.1.13 the string returned is the MAM ID, which will be sent with to the recipients.
	 * 
	 * @param archive
	 * @return MAM ID
	 */
	String archive(Message archive);
	
	/**
	 * Retrieve by the primary key.
	 * 
	 * @param id
	 * @return message with the id or null
	 */
	ArchivedMixChannelMessage findById(String id);
	
	List<ArchivedMixChannelMessage> findMessagesByChannel(String channel);
	
	/**
	 * Allows a client to achieve a 'quick resync' of a node by requesting just those changes it has not yet
	 * received.  One of a number of 'synchronisation' methods.
	 * 
	 * @param channel where the messages were sent
	 * @param after UUID of the message that the client wants messages after
	 * @return
	 */
	List<ArchivedMixChannelMessage> findMessagesByChannelAfter(String channel, String after);
	
	/**
	 * Allows a client to synchronise based on time.
	 * 
	 * @param channel where the messages were sent
	 * @param after which the messages are requested
	 * @return
	 */
	List<ArchivedMixChannelMessage> findMessagesByChannelSince(String channel, Date after);
	
	long getMessageCountByChannel(String channel);
	
	/**
	 * Allows the client to delete a message.  The ID is the MAM ID, as it was reflected to the authoring client on submission.
	 * 
	 * @param id MAM ID of message to be retracted.
	 */
	void retract(String id);

	List<ArchivedMixChannelMessage>  findMessagesByChannelWith(String mixChannelJid, String term);

	List<ArchivedMixChannelMessage> findTimeBoundMessagesByChannel(String channelName, Date start, Date end);

	List<ArchivedMixChannelMessage> findLimitedMessagesByChannelWith(String node, String term, int limit);

	/**
	 * Allows a client to request the messages from a channel but set a limit on the results returned
	 * 
	 * @param channelName where the messages were sent
	 * @param limit the size of the result set the client has requested
	 * @return
	 */
	List<ArchivedMixChannelMessage> findLimitedMessagesByChannel(String channelName, int limit);

	List<ArchivedMixChannelMessage> findLimitedTimeBoundMessagesByChannel(String channelName, Date start, Date end,
			int limit);

	List<ArchivedMixChannelMessage> findLimitedMessagesByChannelSince(String channelName, Date start, int limit);
}
