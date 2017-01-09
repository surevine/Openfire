package org.jivesoftware.openfire.mix;

import java.util.Date;

import org.jivesoftware.database.JiveID;
import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityMode;
import org.jivesoftware.util.JiveConstants;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.resultsetmanagement.Result;

@JiveID(JiveConstants.MIX_CHANNEL)
public interface MixChannel extends Result {
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
			
	void process(Packet packet) throws IllegalArgumentException;
}
