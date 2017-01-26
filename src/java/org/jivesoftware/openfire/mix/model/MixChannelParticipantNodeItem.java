package org.jivesoftware.openfire.mix.model;

import org.dom4j.Element;

public class MixChannelParticipantNodeItem implements MixChannelNodeItem {
	private MixChannelParticipant participant;

	public MixChannelParticipantNodeItem(MixChannelParticipant participant) {
		this.participant = participant;
	}

	@Override
	public String getUID() {
		return participant.getJid().toBareJID();

	}

	@Override
	public Element appendPayload(Element container) {
		Element participantEl = container.addElement("participant", "urn:xmpp:mix:0");
		participantEl.addAttribute("nick", participant.getNick());
		return participantEl;
	}
	
}
