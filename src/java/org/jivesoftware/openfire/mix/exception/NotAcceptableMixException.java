package org.jivesoftware.openfire.mix.exception;

import org.xmpp.packet.PacketError.Condition;

public class NotAcceptableMixException extends MixException {
	private static final long serialVersionUID = 1L;

	public NotAcceptableMixException() {
		super(Condition.not_acceptable);
	}

	public NotAcceptableMixException(String message, Throwable cause) {
		super(Condition.not_acceptable, message, cause);
	}

	public NotAcceptableMixException(String message) {
		super(Condition.not_acceptable, message);
	}

	public NotAcceptableMixException(Throwable cause) {
		super(Condition.not_acceptable, cause);
	}

	
}
