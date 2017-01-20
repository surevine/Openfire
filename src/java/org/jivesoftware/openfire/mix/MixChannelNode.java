package org.jivesoftware.openfire.mix;

import org.dom4j.Element;
import org.jivesoftware.openfire.mix.model.MixChannelNodeItem;
import org.xmpp.packet.JID;

public interface MixChannelNode {
	String getName();

	void appendAllItems(Element itemsEl);
}
