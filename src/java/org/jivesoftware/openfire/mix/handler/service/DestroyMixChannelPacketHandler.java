package org.jivesoftware.openfire.mix.handler.service;

import org.jivesoftware.openfire.mix.MixService;
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
	public IQ processIQ(MixService service, IQ iq) throws Exception {

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

		}
		return reply;
	}

	@Override
	public boolean processPresence(MixService actor, Presence presence) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean processMessage(MixService actor, Message message) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}
}
