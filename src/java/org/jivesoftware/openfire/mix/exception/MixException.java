package org.jivesoftware.openfire.mix.exception;

import org.xmpp.packet.PacketError;

public class MixException extends Exception {
	private static final long serialVersionUID = 1L;
	
	private PacketError.Condition condition;
	private PacketError.Type type;
	
	public MixException(PacketError.Condition condition, PacketError.Type type) {
		super();
		this.condition = condition;
		this.type = type;
	}

	public MixException(PacketError.Condition condition, PacketError.Type type, String message, Throwable cause) {
		super(message, cause);
		this.condition = condition;
		this.type = type;
	}

	public MixException(PacketError.Condition condition, PacketError.Type type, String message) {
		super(message);
		this.condition = condition;
		this.type = type;
	}

	public MixException(PacketError.Condition condition, PacketError.Type type, Throwable cause) {
		super(cause);
		this.condition = condition;
		this.type = type;
	}
	
	public MixException(PacketError.Condition condition) {
		this(condition, condition.getDefaultType());
	}

	public MixException(PacketError.Condition condition, String message, Throwable cause) {
		this(condition, condition.getDefaultType(), message, cause);
	}

	public MixException(PacketError.Condition condition, String message) {
		this(condition, condition.getDefaultType(), message);
	}

	public MixException(PacketError.Condition condition, Throwable cause) {
		this(condition, condition.getDefaultType(), cause);
	}

	public PacketError getPacketError() {
		return new PacketError(condition, type);
	}
}
