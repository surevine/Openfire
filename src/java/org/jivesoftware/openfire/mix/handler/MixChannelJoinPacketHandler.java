package org.jivesoftware.openfire.mix.handler;

import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelNodeType;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;

public class MixChannelJoinPacketHandler implements MixChannelPacketHandler {

	@Override
	public IQ processIQ(MixChannel channel, IQ iq) {
		// Unpack packet
		channel.addParticipant(null, null);
		return null;
	}

	@Override
	public void processPresence(MixChannel channel, Presence presence) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processMessage(MixChannel channel, Message message) {
		// TODO Auto-generated method stub
		
	}
	
}
