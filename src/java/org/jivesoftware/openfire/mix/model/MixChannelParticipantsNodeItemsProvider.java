package org.jivesoftware.openfire.mix.model;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;
import org.jivesoftware.openfire.mix.model.MixChannel.MixChannelParticipantsListener;
import org.xmpp.packet.JID;

public class MixChannelParticipantsNodeItemsProvider implements MixChannelNodeItemsProvider<MixChannelParticipantNodeItem> {

	List<ItemsListener<MixChannelParticipantNodeItem>> itemsListeners;

	MixChannel channel;
	
	public MixChannelParticipantsNodeItemsProvider(MixChannel channel) {
		this.channel = channel;
		
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
				for(ItemsListener<MixChannelParticipantNodeItem> listener : itemsListeners) {
					listener.retractItem(leaver.getJid());
				}
			}
		});
	}
	
	@Override
	public List<MixChannelParticipantNodeItem> getItems() {
		List<MixChannelParticipantNodeItem> items = new ArrayList<>();
		
		for(MixChannelParticipant participant : channel.getParticipants()) {
			items.add(new MixChannelParticipantNodeItem(participant));
		}
		
		return items;
	}

	@Override
	public void addItemsListener(ItemsListener<MixChannelParticipantNodeItem> listener) {
		itemsListeners.add(listener);
	}

	@Override
	public MixChannelParticipantNodeItem getItem(String itemId) {
		MixChannelParticipant participant = channel.getParticipantByRealJID(new JID(itemId));
		
		if(participant != null) {
			return new MixChannelParticipantNodeItem(participant);
		}
		
		return null;
	}

	@Override
	public MixChannelParticipantNodeItem receiveItem(Element itemElement) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
