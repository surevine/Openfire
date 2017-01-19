package org.jivesoftware.openfire.mix.handler.service;

import org.jivesoftware.openfire.mix.MixService;
import org.jivesoftware.openfire.mix.exception.MixChannelAlreadyExistsException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketError.Condition;
import org.xmpp.packet.Presence;
import org.xmpp.packet.IQ.Type;

public class MixServiceChannelCreatePacketHandler implements MixServicePacketHandler {

	@Override
	public IQ processIQ(MixService service, IQ iq) throws Exception {

		final IQ reply = IQ.createResultIQ(iq);
		reply.setChildElement(iq.getChildElement().createCopy());
		
		try {
			service.createChannel(iq.getFrom(), iq.getChildElement().attributeValue("channel"));
		} catch(MixChannelAlreadyExistsException e) {
			reply.setType(Type.error);
			reply.setError(Condition.conflict);
		}
		
		return reply;
	}

	@Override
	public boolean processPresence(MixService service, Presence presence) throws Exception {
		return false;
	}

	@Override
	public boolean processMessage(MixService service, Message message) throws Exception {
		return false;
	}

}
