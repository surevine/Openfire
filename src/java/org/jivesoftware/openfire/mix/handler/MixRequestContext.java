package org.jivesoftware.openfire.mix.handler;

import org.jivesoftware.openfire.mix.MixService;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.xmpp.packet.JID;

public interface MixRequestContext {
	JID getRecipient();
	
	MixService getMixService();
	
	MixChannel getMixChannel();
	
	MixChannelParticipant getMixChannelParticipant();
}
