package org.jivesoftware.openfire.mix.model;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.openfire.mix.model.MixChannel.MixChannelParticipantsListener;

public class MixChannelJidMapNodeItemsProvider implements MixChannelNodeItemsProvider {

	List<ItemsListener> itemsListeners;
	
	MixChannel channel;
	
	public MixChannelJidMapNodeItemsProvider(MixChannel channel) {
		this.channel = channel;
		
		itemsListeners = new ArrayList<>();
		
		channel.addParticipantsListener(new MixChannelParticipantsListener() {
			
			@Override
			public void onParticipantAdded(MixChannelParticipant participant) {
				for(ItemsListener listener : itemsListeners) {
					listener.publishItem(new MixChannelJidMapNodeItem(participant));
				}
			}
		});
	}
	
	@Override
	public List<MixChannelNodeItem> getItems() {
		List<MixChannelNodeItem> items = new ArrayList<>();
		
		for(MixChannelParticipant participant : channel.getParticipants()) {
			items.add(new MixChannelJidMapNodeItem(participant));
		}
		
		return items;
	}

	@Override
	public void addItemsListener(ItemsListener listener) {
		itemsListeners.add(listener);
	}
	
}
