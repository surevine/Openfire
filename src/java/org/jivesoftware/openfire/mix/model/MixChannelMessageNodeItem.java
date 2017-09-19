package org.jivesoftware.openfire.mix.model;

import org.dom4j.Element;

public class MixChannelMessageNodeItem implements MixChannelNodeItem {

	Element payload;
	
	@Override
	public String getUID() {
		return "0";
	}

	@Override
	public Element appendPayload(Element container) {
		Element newEl = payload.createCopy();
		
		container.add(newEl);
		
		return newEl;
	}

}
