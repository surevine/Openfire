package org.jivesoftware.openfire.mix.handler.channel;

import org.jivesoftware.openfire.mix.exception.CannotLeaveMixChannelException;
import org.jivesoftware.openfire.mix.model.LocalMixChannel;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketError.Condition;
import org.xmpp.packet.Presence;

public class MixChannelLeavePacketHandler implements MixChannelPacketHandler {
	
	private static final Logger LOG = LoggerFactory.getLogger(LocalMixChannel.class);
	
	private static final String LEAVE_ELEM_NAME = "leave";

	@Override
	public IQ processIQ(MixChannel channel, IQ leaveRequest) {

		if (LEAVE_ELEM_NAME.equals(leaveRequest.getChildElement().getName())) {
			IQ leaveResponse = IQ.createResultIQ(leaveRequest);
			
			try {
				channel.removeParticipant(leaveRequest.getFrom());
				leaveResponse.setChildElement(leaveRequest.getChildElement().createCopy());
			} catch (CannotLeaveMixChannelException e) {
				LOG.warn("Unable to remove user from channel", e);
				leaveResponse.setError(Condition.conflict);
			}
			
			return leaveResponse;

		} else {
			return null;
		}
	}

	@Override
	public boolean processPresence(MixChannel actor, Presence presence) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean processMessage(MixChannel actor, Message message) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}
}
