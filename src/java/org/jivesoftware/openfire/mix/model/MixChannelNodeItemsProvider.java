package org.jivesoftware.openfire.mix.model;

import java.util.List;

public interface MixChannelNodeItemsProvider {
	public interface ItemsListener {
		void publishItem(MixChannelNodeItem item);
	}
	
	List<MixChannelNodeItem> getItems();
	
	void addItemsListener(ItemsListener listener);
}
