package org.jivesoftware.openfire.mix.handler.channel;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;
import org.dom4j.QName;
import org.jivesoftware.openfire.mix.MixChannelNode;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelNodeItem;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketError.Condition;
import org.xmpp.packet.Presence;
import org.xmpp.resultsetmanagement.Result;
import org.xmpp.resultsetmanagement.ResultSet;
import org.xmpp.resultsetmanagement.ResultSetImpl;

public class MixChannelNodeItemsGetPacketHandler implements MixChannelPacketHandler {

	public static final QName QNAME = QName.get("pubsub", "http://jabber.org/protocol/pubsub");

	@Override
	public IQ processIQ(MixChannel actor, IQ iq) throws Exception {
		IQ reply = IQ.createResultIQ(iq);
		
		Element el = iq.getChildElement();
		
		if ((el == null) || (el.getQName() != QNAME)) {
			return null;
		}
		
		Element itemsEl = el.element("items");
		
		if(itemsEl == null) {
			return null;
		}
		
		Element replyItemsEl = itemsEl.createCopy();
		
		reply.setChildElement(replyItemsEl);
		
		String nodeName = itemsEl.attributeValue("node");
		
		MixChannelNode node = actor.getNodeByName(nodeName);
		
		if(node == null) {
			reply.setError(Condition.item_not_found);
			return reply;
		}
		
		node.appendAllItems(itemsEl);
		
		return null;
	}

	@Override
	public boolean processPresence(MixChannel actor, Presence presence) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean processMessage(MixChannel actor, Message message) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

}
