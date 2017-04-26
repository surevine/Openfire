package org.jivesoftware.openfire.mix.model;

import java.util.UUID;

import org.xmpp.packet.Message;

public class MixChannelMessageImpl implements MixChannelMessage {
	private Message message;
	
	private String id;
	
	private MixChannelParticipant sender;
	
	public MixChannelMessageImpl(Message message, String id, MixChannelParticipant sender) {
		this.message = message;
		this.id = id;
		this.sender = sender;
	}
	
	public MixChannelMessageImpl(Message message, MixChannelParticipant sender) {
		this(message, UUID.randomUUID().toString(), sender);
	}

	@Override
	public Message getMessage() {
		return message;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public MixChannelParticipant getSender() {
		return sender;
	}
}
