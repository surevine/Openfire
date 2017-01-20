package org.jivesoftware.openfire.mix.model;

import org.dom4j.Element;

public class MixChannelJidMapNodeItem implements MixChannelNodeItem {
	private MixChannelParticipant participant;

	public MixChannelJidMapNodeItem(MixChannelParticipant participant) {
		this.participant = participant;
	}

	@Override
	public String getUID() {
		return participant.getJid().toBareJID();
	}

	@Override
	public Element appendPayload(Element container) {
		Element participantEl = container.addElement("participant", "urn:xmpp:mix:0");
		participantEl.addAttribute("jid", participant.getRealJid().toBareJID());
		return participantEl;
	}
	
}
