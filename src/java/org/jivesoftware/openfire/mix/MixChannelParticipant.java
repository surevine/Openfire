package org.jivesoftware.openfire.mix;

import org.xmpp.packet.JID;

public interface MixChannelParticipant {

	public enum JidVisibilityPreference {
		NONE,
		PREFER_HIDDEN,
		ENFORCE_HIDDEN,
		ENFORCE_VISIBLE
	}
	
	JID getJID();
	
}
