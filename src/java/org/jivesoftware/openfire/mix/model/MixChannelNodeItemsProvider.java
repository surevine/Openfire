package org.jivesoftware.openfire.mix.model;

import java.util.List;

import org.xmpp.packet.JID;

public interface MixChannelNodeItemsProvider<T extends MixChannelNodeItem> {
	public interface ItemsListener<T extends MixChannelNodeItem> {
		void publishItem(T item);
		
		void retractItem(JID jid);
	}
	
	List<T> getItems();
	
	void addItemsListener(ItemsListener<T> listener);
}
