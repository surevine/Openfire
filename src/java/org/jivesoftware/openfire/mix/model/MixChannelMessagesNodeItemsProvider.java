package org.jivesoftware.openfire.mix.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jivesoftware.openfire.mix.model.MixChannel.MixChannelMessageListener;
import org.jivesoftware.openfire.mix.model.MixChannel.MixChannelParticipantsListener;
import org.jivesoftware.openfire.mix.model.MixChannelNodeItemsProvider.ItemsListener;

public class MixChannelMessagesNodeItemsProvider implements MixChannelNodeItemsProvider {

	List<ItemsListener> itemsListeners;
	
	public MixChannelMessagesNodeItemsProvider(MixChannel channel) {
		itemsListeners = new ArrayList<>();
		
		channel.addMessageListener(new MixChannelMessageListener() {
			
			@Override
			public void onMessageReceived(MixChannelMessage message) {
				for(ItemsListener listener : itemsListeners) {
					listener.publishItem(new MixChannelMessageNodeItem());
				}
			}
		});
	}
	
	@Override
	public List<MixChannelNodeItem> getItems() {
		return Collections.emptyList();
	}

	@Override
	public void addItemsListener(ItemsListener listener) {
		itemsListeners.add(listener);
	}

}
