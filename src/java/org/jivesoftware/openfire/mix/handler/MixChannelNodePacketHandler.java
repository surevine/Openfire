package org.jivesoftware.openfire.mix.handler;

import org.jivesoftware.openfire.mix.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelNodeType;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;

public interface MixChannelNodePacketHandler {
	MixChannelNodeType getNodeType();
	
	IQ processIQ(MixChannel channel, IQ iq);
	
	void processPresence(MixChannel channel, Presence presence);
	
	void processMessage(MixChannel channel, Message message);
}
