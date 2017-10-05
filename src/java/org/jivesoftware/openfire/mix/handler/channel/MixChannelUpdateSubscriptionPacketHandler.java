package org.jivesoftware.openfire.mix.handler.channel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.QName;
import org.jivesoftware.openfire.mix.MixManager;
import org.jivesoftware.openfire.mix.exception.CannotJoinMixChannelException;
import org.jivesoftware.openfire.mix.exception.CannotUpdateMixChannelSubscriptionException;
import org.jivesoftware.openfire.mix.handler.MixRequestContext;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;

public class MixChannelUpdateSubscriptionPacketHandler implements MixChannelPacketHandler  {
	
	private static final String TARGET_ELEM_NAME = "update-subscription";
	private static final QName TARGET_QNAME = QName.get(TARGET_ELEM_NAME, MixManager.MIX_NAMESPACE);
	
	private static final String SUBSCRIBE_ELEM_NAME = "subscribe";
	private static final String NODE_ATTR_NAME = "node";

	@Override
	public IQ processIQ(MixRequestContext context, MixChannel channel, IQ iq) throws Exception {
		// Unpack packet
		Element updateSubsNode = iq.getChildElement();

		if ((updateSubsNode == null) || (!updateSubsNode.getQName().equals(TARGET_QNAME))) {
			return null;
		}

		@SuppressWarnings("unchecked")
		List<Element> selectedSubscriptions = updateSubsNode.elements(SUBSCRIBE_ELEM_NAME);

		Set<String> subscriptionRequests = new HashSet<>();

		for (Node subscription : selectedSubscriptions) {
			if (subscription.getNodeType() == Node.ELEMENT_NODE) {
				Element elem = (Element) subscription;
				subscriptionRequests.add(elem.attributeValue(NODE_ATTR_NAME));
			}
		}

		MixChannelParticipant mcp = null;

		IQ result = IQ.createResultIQ(iq);

		Element joinElement = result.setChildElement(TARGET_ELEM_NAME, MixManager.MIX_NAMESPACE);

		try {
			channel.updateSubscriptions(iq.getFrom(), subscriptionRequests);
			mcp = channel.getParticipantByRealJID(iq.getFrom());

			for (String subscription : mcp.getSubscriptions()) {
				Element current = joinElement.addElement(SUBSCRIBE_ELEM_NAME);
				current.addAttribute(NODE_ATTR_NAME, subscription);
			}
		} catch (CannotUpdateMixChannelSubscriptionException e) {
			result.setType(IQ.Type.error);
		}

		return result;
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
