package org.jivesoftware.openfire.mix;

import org.dom4j.Element;
import org.jivesoftware.openfire.mix.exception.MixException;
import org.jivesoftware.openfire.mix.handler.MixRequestContext;
import org.jivesoftware.openfire.mix.model.MixChannelNodeItem;

public interface MixChannelNode<T extends MixChannelNodeItem> {
	String getName();

	void appendAllItems(MixRequestContext context, Element itemsEl);

	void appendItemByID(MixRequestContext context, Element itemsEl, String itemId);

	T publishPayload(MixRequestContext context, Element publishedItem) throws MixException;
}
