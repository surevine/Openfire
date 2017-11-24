package org.jivesoftware.openfire.mix.model;

import org.dom4j.Element;
import org.xmpp.resultsetmanagement.Result;

public interface MixChannelNodeItem extends Result {
	Element appendPayload(Element container);
}
