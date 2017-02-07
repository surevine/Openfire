package org.jivesoftware.openfire.mix.model;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.openfire.mix.model.MixChannel.MixChannelParticipantsListener;
import org.xmpp.packet.JID;

public class MixChannelJidMapNodeItemsProvider implements MixChannelNodeItemsProvider<MixChannelJidMapNodeItem> {

	List<ItemsListener<MixChannelJidMapNodeItem>> itemsListeners;
	
	MixChannel channel;
	
	public MixChannelJidMapNodeItemsProvider(MixChannel channel) {
		this.channel = channel;
		
		itemsListeners = new ArrayList<>();
		
		channel.addParticipantsListener(new MixChannelParticipantsListener() {
			
			@Override
			public void onParticipantAdded(MixChannelParticipant participant) {
				for(ItemsListener<MixChannelJidMapNodeItem> listener : itemsListeners) {
					listener.publishItem(new MixChannelJidMapNodeItem(participant));
				}
			}

			@Override
			public void onParticipantRemoved(MixChannelParticipant mcp) {
				for(ItemsListener<MixChannelJidMapNodeItem> listener : itemsListeners) {
					listener.retractItem(mcp.getJid());
				}
			}

			@Override
			public void onMessageReceived(MixChannelMessage mcMessage) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	@Override
	public List<MixChannelJidMapNodeItem> getItems() {
		List<MixChannelJidMapNodeItem> items = new ArrayList<>();
		
		for(MixChannelParticipant participant : channel.getParticipants()) {
			items.add(new MixChannelJidMapNodeItem(participant));
		}
		
		return items;
	}

	@Override
	public void addItemsListener(ItemsListener<MixChannelJidMapNodeItem> listener) {
		itemsListeners.add(listener);
	}

	@Override
	public MixChannelJidMapNodeItem getItem(String itemId) {
		MixChannelParticipant participant = channel.getParticipantByProxyJID(new JID(itemId));
		
		if(participant != null) {
			return new MixChannelJidMapNodeItem(participant);
		}
		
		return null;
	}
	
}
