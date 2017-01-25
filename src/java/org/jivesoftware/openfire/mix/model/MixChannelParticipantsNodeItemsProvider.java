package org.jivesoftware.openfire.mix.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jivesoftware.openfire.mix.model.MixChannel.MixChannelParticipantsListener;

public class MixChannelParticipantsNodeItemsProvider implements MixChannelNodeItemsProvider<MixChannelParticipantNodeItem> {

	List<ItemsListener<MixChannelParticipantNodeItem>> itemsListeners;
	
	public MixChannelParticipantsNodeItemsProvider(MixChannel channel) {
		itemsListeners = new ArrayList<>();
		
		channel.addParticipantsListener(new MixChannelParticipantsListener() {
			
			@Override
			public void onParticipantAdded(MixChannelParticipant participant) {
				for(ItemsListener<MixChannelParticipantNodeItem> listener : itemsListeners) {
					listener.publishItem(new MixChannelParticipantNodeItem(participant));
				}
			}

			@Override
			public void onParticipantRemoved(MixChannelParticipant leaver) {
				for(ItemsListener listener : itemsListeners) {
					listener.retractItem(leaver.getJid());
				}
			}
		});
	}
	
	@Override
	public List<MixChannelParticipantNodeItem> getItems() {
		return Collections.emptyList();
	}

	@Override
	public void addItemsListener(ItemsListener<MixChannelParticipantNodeItem> listener) {
		itemsListeners.add(listener);
	}
	
}
