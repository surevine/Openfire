package org.jivesoftware.openfire.mix.model;

import java.util.List;

import org.xmpp.packet.JID;

public interface MixChannelNodeItemsProvider {
	public interface ItemsListener {
		void publishItem(MixChannelNodeItem item);
		
		void retractItem(JID jid);
	}
	
	List<MixChannelNodeItem> getItems();
	
	void addItemsListener(ItemsListener listener);
}
