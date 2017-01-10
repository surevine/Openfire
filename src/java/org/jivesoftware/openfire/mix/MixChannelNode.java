package org.jivesoftware.openfire.mix;

import org.xmpp.packet.JID;

public interface MixChannelNode {
	String getName();
	
	void subscribe(JID jid);
}
