package org.jivesoftware.openfire.mix.handler.service;

import org.jivesoftware.openfire.mix.MixService;
import org.jivesoftware.openfire.mix.exception.MixChannelAlreadyExistsException;
import org.jivesoftware.openfire.mix.handler.MixRequestContext;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketError.Condition;
import org.xmpp.packet.Presence;
import org.xmpp.packet.IQ.Type;

public class MixServiceChannelCreatePacketHandler implements MixServicePacketHandler {

	@Override
	public IQ processIQ(MixRequestContext context, MixService service, IQ iq) throws Exception {

		final IQ reply = IQ.createResultIQ(iq);
		reply.setChildElement(iq.getChildElement().createCopy());

		if ("create".equals(iq.getChildElement().getName())) {
			try {
				service.createChannel(iq.getFrom(), iq.getChildElement().attributeValue("channel"));
			} catch(MixChannelAlreadyExistsException e) {
				reply.setType(Type.error);
				reply.setError(Condition.conflict);
			}
			
			return reply;
		}
		
		return null;
	}

	@Override
	public boolean processPresence(MixRequestContext context, MixService service, Presence presence) throws Exception {
		return false;
	}

	@Override
	public boolean processMessage(MixRequestContext context, MixService service, Message message) throws Exception {
		return false;
	}

}
