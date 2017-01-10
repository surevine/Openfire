package org.jivesoftware.openfire.mix.handler;

import org.jivesoftware.openfire.mix.model.MixChannel;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;

public interface MixChannelPacketHandler {
	IQ processIQ(MixChannel channel, IQ iq);
	
	void processPresence(MixChannel channel, Presence presence);
	
	void processMessage(MixChannel channel, Message message);
}
