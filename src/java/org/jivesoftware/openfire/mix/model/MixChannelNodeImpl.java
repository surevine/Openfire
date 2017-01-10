package org.jivesoftware.openfire.mix.model;

import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.openfire.mix.MixChannelNode;
import org.jivesoftware.openfire.mix.model.MixChannelNodeItemsProvider.ItemsListener;
import org.xmpp.packet.JID;

public class MixChannelNodeImpl implements MixChannelNode {

	private String name;
	
	private MixChannelNodeItemsProvider itemsProvider;
	
	private Set<JID> subscribers;	// Get rid of this!
	
	public MixChannelNodeImpl(String name, MixChannelNodeItemsProvider itemsProvider) {
		this.name = name;
		this.itemsProvider = itemsProvider;
		subscribers = new HashSet<>();
		
		itemsProvider.addItemsListener(new ItemsListener() {
			
			@Override
			public void publishItem(MixChannelNodeItem item) {
				for(JID subscriber : subscribers) {
					// Publish the item
				}
			}
		});
	}

	@Override
	public String getName() {
		return name;
	}

	public void subscribe(JID jid) {
		subscribers.add(jid);
	}

}
