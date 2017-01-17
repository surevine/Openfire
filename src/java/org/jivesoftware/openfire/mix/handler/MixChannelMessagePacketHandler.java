package org.jivesoftware.openfire.mix.handler;

import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelMessage;
import org.jivesoftware.openfire.mix.model.MixChannelMessageImpl;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketError.Condition;
import org.xmpp.packet.Presence;

public class MixChannelMessagePacketHandler implements MixChannelPacketHandler {

	private PacketRouter router;

	public MixChannelMessagePacketHandler(PacketRouter router) {
		this.router = router;
	}
	
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
			Message error = message.createCopy();
			error.setFrom(channel.getJID());
			error.setTo(message.getFrom());
			error.setError(Condition.not_acceptable);
			
			router.route(error);
			
			return true;
		}
		
		MixChannelMessage mcMessage = new MixChannelMessageImpl(message, participant);
		
		channel.receiveMessage(mcMessage);
		
		return true;
	}

}
