package org.jivesoftware.openfire.mix.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jivesoftware.openfire.mix.model.MixChannel.MixChannelParticipantsListener;
import org.jivesoftware.openfire.mix.model.MixChannelNodeItemsProvider.ItemsListener;

public class MixChannelMessagesNodeItemsProvider implements MixChannelNodeItemsProvider {

	List<ItemsListener> itemsListeners;
	
	public MixChannelMessagesNodeItemsProvider(MixChannel channel) {
		itemsListeners = new ArrayList<>();
		
		channel.addMessagesListener(new MixChannelMessagesListener() {
			
			@Override
			public void onMessage(MixChannelMessage message) {
				for(ItemsListener listener : itemsListeners) {
					listener.publishItem(new MixChannelParticipantNodeItem(participant));
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
