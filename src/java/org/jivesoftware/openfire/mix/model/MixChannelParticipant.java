package org.jivesoftware.openfire.mix.model;

import java.util.Set;

import org.xmpp.packet.JID;

public interface MixChannelParticipant {
	JID getJid();
	
	String getNick();
	
	MixChannel getChannel();
	
	Set<String> getSubscriptions();
}
