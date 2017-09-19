package org.jivesoftware.openfire.mix.model;

import org.xmpp.packet.Message;

public interface MixChannelMessage {

	Message getMessage();

	String getId();

	MixChannelParticipant getSender();

}