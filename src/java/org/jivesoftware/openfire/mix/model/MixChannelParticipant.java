package org.jivesoftware.openfire.mix.model;

import java.util.Set;

import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityPreference;
import org.xmpp.packet.JID;

public interface MixChannelParticipant {
	JID getRealJid();
	
	String getNick();
	
	MixChannel getChannel();
	
	Set<String> getSubscriptions();
	
	boolean subscribesTo(String nodeName);
	
	ChannelJidVisibilityPreference getJidVisibilityPreference();

	JID getJid();
}
