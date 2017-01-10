package org.jivesoftware.openfire.mix.model;

import org.dom4j.Element;

public interface MixChannelNodeItem {
	String getId();
	
	Element appendPayload(Element container);
}
