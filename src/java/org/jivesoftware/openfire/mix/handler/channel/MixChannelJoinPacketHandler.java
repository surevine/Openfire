package org.jivesoftware.openfire.mix.handler.channel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.xmpp.packet.IQ;
import org.xmpp.packet.IQ.Type;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;

public class MixChannelJoinPacketHandler implements MixChannelPacketHandler {

	@Override
	public IQ processIQ(MixChannel channel, IQ iq) {
		// Unpack packet
		Element joinNode = iq.getChildElement();

		@SuppressWarnings("unchecked")
		List<Element> selectedSubscriptions = joinNode.elements("subscribe");

		Set<String> subscriptionRequests = new HashSet<>();

		for (Node subscription : selectedSubscriptions) {
			if (subscription.getNodeType() == Node.ELEMENT_NODE) {
				Element elem = (Element) subscription;
				subscriptionRequests.add(elem.attributeValue("node"));
			}
		}

		MixChannelParticipant mcp = channel.addParticipant(iq.getFrom().asBareJID(), subscriptionRequests);

		IQ result = new IQ(Type.result, iq.getID());
		result.setFrom(channel.getJID());
		result.setTo(iq.getFrom());
		result.setChildElement("join", "urn:xmpp:mix:0");
		Element joinElement = result.getChildElement();
		joinElement.addAttribute("jid", iq.getFrom().toBareJID());

		for (String subscription : mcp.getSubscriptions()) {
			Element current = joinElement.addElement("node");
			current.addAttribute("node", subscription);
		}
		
		return result;
	}

	@Override
	public boolean processPresence(MixChannel channel, Presence presence) {
		throw new NotImplementedException();
	}

	@Override
	public boolean processMessage(MixChannel channel, Message message) {
		throw new NotImplementedException();
	}
}
