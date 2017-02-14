package org.jivesoftware.openfire.mix.handler.channel;

import java.util.Iterator;

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

public class MixChannelNodeItemPublishPacketHandler implements MixChannelPacketHandler {

	public static final QName QNAME = QName.get("pubsub", "http://jabber.org/protocol/pubsub");

	@Override
	public IQ processIQ(MixRequestContext context, MixChannel channel, IQ iq) throws Exception {
		IQ reply = IQ.createResultIQ(iq);
		
		Element el = iq.getChildElement();
		
		if ((el == null) || (!el.getQName().equals(QNAME))) {
			return null;
		}
		
		Element publishEl = el.element("publish");
		
		if(publishEl == null) {
			return null;
		}
		
		Element replyItemsEl = publishEl.createCopy();
		
		reply.setChildElement(replyItemsEl);
		
		String nodeName = publishEl.attributeValue("node");
		
		MixChannelNode<? extends MixChannelNodeItem> node = channel.getNodeByName(nodeName);
		
		if(node == null) {
			reply.setError(Condition.item_not_found);
			return reply;
		}
		
		@SuppressWarnings("unchecked")
		Element publishedItem = publishEl.element("item");
		
		if(publishedItem != null) {
			node.publishPayload(context, publishedItem);
		}
		
		return reply;
	}

	@Override
	public boolean processPresence(MixRequestContext context, MixChannel actor, Presence presence) throws Exception {
		return false;
	}

	@Override
	public boolean processMessage(MixRequestContext context, MixChannel actor, Message message) throws Exception {
		return false;
	}

}
