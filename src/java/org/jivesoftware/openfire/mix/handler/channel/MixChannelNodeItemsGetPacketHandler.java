package org.jivesoftware.openfire.mix.handler.channel;

import org.dom4j.Element;
import org.dom4j.QName;
import org.jivesoftware.openfire.mix.MixChannelNode;
import org.jivesoftware.openfire.mix.handler.MixRequestContext;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelNodeItem;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketError.Condition;
import org.xmpp.packet.Presence;

public class MixChannelNodeItemsGetPacketHandler implements MixChannelPacketHandler {

	public static final QName QNAME = QName.get("pubsub", "http://jabber.org/protocol/pubsub");

	@Override
	public IQ processIQ(MixRequestContext context, MixChannel actor, IQ iq) throws Exception {
		IQ reply = IQ.createResultIQ(iq);
		
		Element el = iq.getChildElement();
		
		if ((el == null) || (!el.getQName().equals(QNAME))) {
			return null;
		}
		
		Element itemsEl = el.element("items");
		
		if(itemsEl == null) {
			return null;
		}
		
		Element replyItemsEl = itemsEl.createCopy();
		
		reply.setChildElement(replyItemsEl);
		
		String nodeName = itemsEl.attributeValue("node");
		
		MixChannelNode<? extends MixChannelNodeItem> node = actor.getNodeByName(nodeName);
		
		if(node == null) {
			reply.setError(Condition.item_not_found);
			return reply;
		}
		
		node.appendAllItems(context, replyItemsEl);
		
		return reply;
	}

	@Override
	public boolean processPresence(MixRequestContext context, MixChannel actor, Presence presence) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean processMessage(MixRequestContext context, MixChannel actor, Message message) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

}
