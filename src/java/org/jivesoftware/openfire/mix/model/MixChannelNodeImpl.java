package org.jivesoftware.openfire.mix.model;

import java.util.HashSet;
import java.util.Set;

import org.dom4j.Element;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.mix.MixChannelNode;
import org.jivesoftware.openfire.mix.model.MixChannelNodeItemsProvider.ItemsListener;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

public class MixChannelNodeImpl implements MixChannelNode {

	private MixChannel mixChannel;
	
	private String name;
	
	private MixChannelNodeItemsProvider itemsProvider;
	
	private Set<JID> subscribers;	// Get rid of this!

	private PacketRouter packetRouter;
	
	public MixChannelNodeImpl(final PacketRouter packetRouter, final MixChannel mixChannel, String name, MixChannelNodeItemsProvider itemsProvider) {
		this.mixChannel = mixChannel;
		this.name = name;
		this.itemsProvider = itemsProvider;
		this.packetRouter = packetRouter;
		
		subscribers = new HashSet<>();
		
		itemsProvider.addItemsListener(new ItemsListener() {
			
			@Override
			public void publishItem(MixChannelNodeItem item) {
				// Create a base message which we then clone for each subscriber
				Message baseMessage = new Message();
				baseMessage.setFrom(mixChannel.getJID());
				
				addItemElement(addItemsElement(addEventElement(baseMessage)), item);

				for(JID subscriber : subscribers) {
					Message message = baseMessage.createCopy();
					
					message.setTo(subscriber);
					
					packetRouter.route(message);
				}
			}
		});
	}
		
	private Element addItemElement(Element parent, MixChannelNodeItem item) {
		return item.appendPayload(parent.addElement("item")
				.addAttribute("id", item.getId()));
	}
	
	private Element addItemsElement(Element parent) {
		return parent.addElement("items")
			.addAttribute("node", name);
	}
	
	private Element addEventElement(Message parent) {
		return parent.addChildElement("event", "http://jabber.org/protocol/pubsub#event");
	}

	@Override
	public String getName() {
		return name;
	}

	public void addSubscriber(JID jid) {
		subscribers.add(jid);
	}

}
