package org.jivesoftware.openfire.mix.handler.service;

import org.dom4j.Element;
import org.dom4j.QName;
import org.jivesoftware.openfire.mix.MixService;
import org.jivesoftware.openfire.mix.handler.MixRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.IQ.Type;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketError.Condition;
import org.xmpp.packet.Presence;

public class MamQueryPacketHandler implements MixServicePacketHandler {

	private static final Logger Log = LoggerFactory.getLogger(MamQueryPacketHandler.class);

	@Override
	public IQ processIQ(MixRequestContext context, MixService service, IQ iq) throws Exception {

		Log.info("MQPH Got IQ: " + iq.toString());

		if (iq.getType() != IQ.Type.set) {
			Log.info("Not a set query for mam");
			return null;
		}

		Element iqChild = iq.getChildElement();

		Log.info("Channel query");
		Log.info("Got IQ: " + iq.toString());
		Log.info("Qname: " + iqChild.getQName() + " " + QName.get("query", "urn:xmpp:mam:2"));

		if ((iqChild == null) || (!iqChild.getQName().equals(QName.get("query", "urn:xmpp:mam:2")))) {
			Log.info("Not a query for mam");
			return null;
		}

		Log.info("Querying mam");
		return service.getArchive().query(iq);
	}

	@Override
	public boolean processPresence(MixRequestContext context, MixService actor, Presence presence) throws Exception {
		return false;
	}

	@Override
	public boolean processMessage(MixRequestContext context, MixService actor, Message message) throws Exception {
		return false;
	}
}
