package org.jivesoftware.openfire.mix.handler.channel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.jivesoftware.openfire.mix.MixPersistenceException;
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
		
		if ("join".equals(joinNode.getName())) {
			@SuppressWarnings("unchecked")
			List<Element> selectedSubscriptions = joinNode.elements("subscribe");

			Set<String> subscriptionRequests = new HashSet<>();

			for (Node subscription : selectedSubscriptions) {
				if (subscription.getNodeType() == Node.ELEMENT_NODE) {
					Element elem = (Element) subscription;
					subscriptionRequests.add(elem.attributeValue("node"));
				}
			}

			MixChannelParticipant mcp = null;
			

			IQ result = IQ.createResultIQ(iq);
			
			Element joinElement = result.setChildElement("join", "urn:xmpp:mix:0");
			
			try {
				mcp = channel.addParticipant(iq.getFrom().asBareJID(), subscriptionRequests);

				for (String subscription : mcp.getSubscriptions()) {
					Element current = joinElement.addElement("node");
					current.addAttribute("node", subscription);
				}
			} catch (MixPersistenceException e) {
				result.setType(IQ.Type.error);
			}
			return result;
		}

		return null;
	}

	@Override
	public boolean processPresence(MixChannel channel, Presence presence) {
		// TODO - not implemented yet
		return false;
	}

	@Override
	public boolean processMessage(MixChannel channel, Message message) {
		// TODO - not implemented yet
		return false;
	}
}
