package org.jivesoftware.openfire.mix.handler.service;

import org.dom4j.Element;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.mix.MixService;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;

/**
 * Reroutes Disco packets back through the Server
 * 
 * Note: This should really be called "MixServiceDiscoPacketHandler" but I promised I'd
 * get a "DiscoMix..." in here somewhere, so here it is!
 */
public class DiscoMixServicePacketHandler implements MixServicePacketHandler {

	private XMPPServer xmppServer;
	
	public DiscoMixServicePacketHandler(XMPPServer xmppServer) {
		this.xmppServer = xmppServer;
	}

	@Override
	public IQ processIQ(MixService actor, IQ iq) throws Exception {
		Element childElement = iq.getChildElement();
		String namespace = null;
		// Ignore IQs of type ERROR
		if (IQ.Type.error == iq.getType()) {
			return null;
		}
		if (childElement != null) {
			namespace = childElement.getNamespaceURI();
		}
		if ("http://jabber.org/protocol/disco#info".equals(namespace)) {
			return xmppServer.getIQDiscoInfoHandler().handleIQ(iq);
		} else if ("http://jabber.org/protocol/disco#items".equals(namespace)) {
			return xmppServer.getIQDiscoItemsHandler().handleIQ(iq);
		} else if ("urn:xmpp:ping".equals(namespace)) {
			return IQ.createResultIQ(iq);
		}

		return null;
	}

	@Override
	public boolean processPresence(MixService actor, Presence presence) throws Exception {
		return false;
	}

	@Override
	public boolean processMessage(MixService actor, Message message) throws Exception {
		return false;
	}

}
