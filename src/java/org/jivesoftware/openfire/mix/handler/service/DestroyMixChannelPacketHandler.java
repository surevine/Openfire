package org.jivesoftware.openfire.mix.handler.service;

import org.jivesoftware.openfire.mix.MixService;
import org.jivesoftware.openfire.mix.handler.MixRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.IQ.Type;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketError.Condition;
import org.xmpp.packet.Presence;

public class DestroyMixChannelPacketHandler implements MixServicePacketHandler {

	private static final Logger LOG = LoggerFactory.getLogger(DestroyMixChannelPacketHandler.class);

	private static final String DESTROY_ELEM_NAME = "destroy";

	@Override
	public IQ processIQ(MixRequestContext context, MixService service, IQ iq) throws Exception {

		LOG.info("PROCESS IQ", iq.toString());

		final IQ reply = IQ.createResultIQ(iq);
		if (DESTROY_ELEM_NAME.equals(iq.getChildElement().getName())) {

			reply.setChildElement(iq.getChildElement().createCopy());

			try {
				service.destroyChannel(iq.getFrom(), iq.getChildElement().attributeValue("channel"));
			} catch (Exception  e) {
				LOG.error("Unable to destroy mix channel + " + iq.getTo().getNode(), e);
				reply.setType(Type.error);
				reply.setError(Condition.conflict);
			}

			return reply;
		}
		return null;
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
