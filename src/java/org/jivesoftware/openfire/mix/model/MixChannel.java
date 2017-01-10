package org.jivesoftware.openfire.mix.model;

import java.util.Set;

import org.xmpp.packet.JID;

public interface MixChannel {
	public interface MixChannelParticipantsListener {
		void onParticipantAdded(MixChannelParticipant participant);
	}

	void addParticipantsListener(MixChannelParticipantsListener listener);
	
	MixChannelParticipant addParticipant(JID jid, Set<String> subscribeNodes);
}
