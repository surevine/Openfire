package org.jivesoftware.openfire.mix.handler;

import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelMessage;
import org.jivesoftware.openfire.mix.model.MixChannelMessageImpl;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;

public class MixChannelMessagePacketHandler implements MixChannelPacketHandler {

	@Override
	public IQ processIQ(MixChannel channel, IQ iq) {
		return null;
	}

	@Override
	public boolean processPresence(MixChannel channel, Presence presence) {
		return false;
	}

	@Override
	public boolean processMessage(MixChannel channel, Message message) {
		if(message.getType() != Message.Type.groupchat) {
			// We only deal with groupchat messages
			return false;
		}
		
		MixChannelParticipant participant = channel.getParticipantByJID(message.getFrom().asBareJID());
		
		if(participant == null) {
			// Silently drop the message - Is this right?
			return true;
		}
		
		MixChannelMessage mcMessage = new MixChannelMessageImpl(message, participant);
		
		channel.receiveMessage(mcMessage);
		
		return true;
	}

}