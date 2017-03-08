package org.jivesoftware.openfire.mix.handler.channel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.QName;
import org.jivesoftware.openfire.mix.MixManager;
import org.jivesoftware.openfire.mix.MixPersistenceManager;
import org.jivesoftware.openfire.mix.exception.CannotJoinMixChannelException;
import org.jivesoftware.openfire.mix.handler.MixRequestContext;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;

public class MixChannelJoinPacketHandler implements MixChannelPacketHandler {

	private static final Logger Log = LoggerFactory.getLogger(MixChannelJoinPacketHandler.class);

	@Override
	public IQ processIQ(MixRequestContext context, MixChannel channel, IQ iq) {
		// Unpack packet
		Element joinNode = iq.getChildElement();

		if ((joinNode == null) || (!joinNode.getQName().equals(QName.get("join", MixManager.MIX_NAMESPACE)))) {
			return null;
		}

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

		Element joinElement = result.setChildElement("join", MixManager.MIX_NAMESPACE);

		try {
			mcp = channel.addParticipant(iq.getFrom(), subscriptionRequests);

			for (String subscription : mcp.getSubscriptions()) {
				Element current = joinElement.addElement("node");
				current.addAttribute("node", subscription);
			}

			//joinElement.addAttribute("jid", mcp.getJid().toBareJID());
		} catch (CannotJoinMixChannelException e) {
		    Log.error(e.toString());
			result.setType(IQ.Type.error);
		}

		return result;
	}

	@Override
	public boolean processPresence(MixRequestContext context, MixChannel channel, Presence presence) {
		return false;
	}

	@Override
	public boolean processMessage(MixRequestContext context, MixChannel channel, Message message) {
		return false;
	}
}
