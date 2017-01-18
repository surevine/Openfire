package org.jivesoftware.openfire.mix.handler;

import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;

public interface MixPacketHandler<T> {
	IQ processIQ(T actor, IQ iq) throws Exception;
	
	boolean processPresence(T actor, Presence presence) throws Exception;
	
	boolean processMessage(T actor, Message message) throws Exception;
}
