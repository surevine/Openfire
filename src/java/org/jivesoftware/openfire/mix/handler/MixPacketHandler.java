package org.jivesoftware.openfire.mix.handler;

import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;

public interface MixPacketHandler<T> {
	IQ processIQ(MixRequestContext context, T actor, IQ iq) throws Exception;
	
	boolean processPresence(MixRequestContext context, T actor, Presence presence) throws Exception;
	
	boolean processMessage(MixRequestContext context, T actor, Message message) throws Exception;
}
