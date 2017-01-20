package org.jivesoftware.openfire.mix.model;

import java.util.Set;

import org.dom4j.Element;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.mix.MixChannelNode;
import org.jivesoftware.openfire.mix.model.MixChannelNodeItemsProvider.ItemsListener;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

public class MixChannelNodeImpl implements MixChannelNode {

	private MixChannel mixChannel;
	
	private final String name;
	
	private MixChannelNodeItemsProvider itemsProvider;

	private PacketRouter packetRouter;
	
	public MixChannelNodeImpl(final PacketRouter packetRouter, final MixChannel mixChannel, final String name, MixChannelNodeItemsProvider itemsProvider) {
		this.mixChannel = mixChannel;
		this.name = name;
		this.itemsProvider = itemsProvider;
		this.packetRouter = packetRouter;
		
		if(itemsProvider != null) {
			itemsProvider.addItemsListener(new ItemsListener() {
				
				@Override
				public void publishItem(MixChannelNodeItem item) {
					// Create a base message which we then clone for each subscriber
					Message baseMessage = new Message();
					baseMessage.setFrom(mixChannel.getJID());
					
					addItemElement(addItemsElement(addEventElement(baseMessage)), item);
					
					Set<MixChannelParticipant> subscribers = mixChannel.getNodeSubscribers(name);
	
					for(MixChannelParticipant subscriber : subscribers) {
						Message message = baseMessage.createCopy();
						
						message.setTo(subscriber.getRealJid());
						
						packetRouter.route(message);
					}
				}

				@Override
				public void retractItem(JID rectract) {
					Message baseMessage = new Message();
					baseMessage.setFrom(mixChannel.getJID());
					
					addRetractElement(addItemsElement(addEventElement(baseMessage)), rectract);
					
					Set<MixChannelParticipant> subscribers = mixChannel.getNodeSubscribers(name);
	
					for(MixChannelParticipant subscriber : subscribers) {
						Message message = baseMessage.createCopy();
						
						message.setTo(subscriber.getRealJid());
						
						packetRouter.route(message);
					}
				}
			});
		}
	}
		
	private Element addItemElement(Element parent, MixChannelNodeItem item) {
		return item.appendPayload(parent.addElement("item")
				.addAttribute("id", item.getUID()));
	}

	private Element addRetractElement(Element parent, JID jid) {
		return parent.addElement("retract")
				.addAttribute("id", jid.toString());
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

	@Override
	public void appendAllItems(Element parent) {
		for(MixChannelNodeItem item : itemsProvider.getItems()) {
			addItemElement(parent, item);
		}
	}

}
