package org.jivesoftware.openfire.mix.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.openfire.mix.MixChannelNode;
import org.xmpp.packet.JID;

public class LocalMixChannel implements MixChannel {
	private Map<JID, MixChannelParticipant> participants;

	private Set<MixChannelNode> nodes;

	private List<MixChannelParticipantsListener> participantsListeners;
	
	public LocalMixChannel() {
		nodes.add(new MixChannelNodeImpl("urn:xmpp:mix:nodes:participants",
				new MixChannelParticipantsNodeItemsProvider(this)));
	}

	@Override
	public void addParticipantsListener(MixChannelParticipantsListener listener) {
		participantsListeners.add(listener);
	}

	@Override
	public MixChannelParticipant addParticipant(JID jid, Set<String> subscribeNodes) {
		MixChannelParticipant participant = new LocalMixChannelParticipant(jid, this, subscribeNodes);
		
		for(MixChannelParticipantsListener listener : participantsListeners) {
			listener.onParticipantAdded(participant);
		}
		
		return participant;
	}
	
}
