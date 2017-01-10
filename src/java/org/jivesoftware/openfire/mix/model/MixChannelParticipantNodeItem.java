package org.jivesoftware.openfire.mix.model;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;

public class MixChannelParticipantNodeItem implements MixChannelNodeItem {
	private MixChannelParticipant participant;

	public MixChannelParticipantNodeItem(MixChannelParticipant participant) {
		super();
		this.participant = participant;
	}

	@Override
	public String getId() {
		return participant.getNick(); // This isn't right
	}

	@Override
	public Element appendPayload(Element container) {
		return container.addElement("participant", "urn:xmpp:mix:0");
	}
	
}
