package org.jivesoftware.openfire.mix.model;

import java.util.Set;

import org.dom4j.Element;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.mix.MixChannelNode;
import org.jivesoftware.openfire.mix.handler.MixRequestContext;
import org.jivesoftware.openfire.mix.handler.MixRequestContextImpl;
import org.jivesoftware.openfire.mix.model.MixChannelNodeItemsProvider.ItemsListener;
import org.jivesoftware.openfire.mix.policy.AlwaysAllowPermissionPolicy;
import org.jivesoftware.openfire.mix.policy.PermissionPolicy;
import org.jivesoftware.openfire.mix.policy.PermissionPolicy.Action;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

public class MixChannelNodeImpl<T extends MixChannelNodeItem> implements MixChannelNode<T> {

	private MixChannel mixChannel;
	
	private final String name;
	
	private MixChannelNodeItemsProvider<T> itemsProvider;

	private PacketRouter packetRouter;
	
	private PermissionPolicy<MixChannelNode<T>> nodePermissionPolicy;	
	
	private PermissionPolicy<T> itemPermissionPolicy;
	
	public MixChannelNodeImpl(final PacketRouter packetRouter, final MixChannel mixChannel, final String name, MixChannelNodeItemsProvider<T> itemsProvider) {
		this(packetRouter, mixChannel, name, itemsProvider, new AlwaysAllowPermissionPolicy<T>(), new AlwaysAllowPermissionPolicy<MixChannelNode<T>>());
	}
	
	public MixChannelNodeImpl(final PacketRouter packetRouter, final MixChannel mixChannel, final String name, MixChannelNodeItemsProvider<T> itemsProvider, final PermissionPolicy<T> itemPermissionPolicy, PermissionPolicy<MixChannelNode<T>> nodePermissionPolicy) {
		this.mixChannel = mixChannel;
		this.name = name;
		this.itemsProvider = itemsProvider;
		this.packetRouter = packetRouter;
		this.itemPermissionPolicy = itemPermissionPolicy;
		this.nodePermissionPolicy = nodePermissionPolicy;
		
		if(itemsProvider != null) {
			itemsProvider.addItemsListener(new ItemsListener<T>() {
				
				@Override
				public void publishItem(T item) {
					// Create a base message which we then clone for each subscriber
					Message baseMessage = new Message();
					baseMessage.setFrom(mixChannel.getJID());
					
					addItemElement(addItemsElement(addEventElement(baseMessage)), item);
					
					Set<MixChannelParticipant> subscribers = mixChannel.getNodeSubscribers(name);
	
					for(MixChannelParticipant subscriber : subscribers) {
						MixRequestContext context = new MixRequestContextImpl(subscriber, mixChannel.getMixService(), mixChannel);
								
						if(!itemPermissionPolicy.checkPermission(context, item, Action.VIEW)) {
							// Skip the notification if they can't see the item
							continue;
						}
						
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
	public void appendAllItems(MixRequestContext context, Element parent) {
		// If they don't have permission on the node then just do nothing
		if(!nodePermissionPolicy.checkPermission(context, this, Action.VIEW)) {
			return;
		}
		
		for(T item : itemsProvider.getItems()) {
			if((itemPermissionPolicy == null) || (itemPermissionPolicy.checkPermission(context, item, Action.VIEW))) {
				addItemElement(parent, item);
			}
		}
	}

	@Override
	public void appendItemByID(MixRequestContext context, Element parent, String itemId) {
		// If they don't have permission on the node then just do nothing
		if(!nodePermissionPolicy.checkPermission(context, this, Action.VIEW)) {
			return;
		}
		
		T item = itemsProvider.getItem(itemId);
		
		if(item != null) {
			if((itemPermissionPolicy == null) || (itemPermissionPolicy.checkPermission(context, item, Action.VIEW))) {
				addItemElement(parent, item);
			}
		}
	}

}
