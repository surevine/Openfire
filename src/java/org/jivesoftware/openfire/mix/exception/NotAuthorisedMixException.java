package org.jivesoftware.openfire.mix.exception;

import org.xmpp.packet.PacketError.Condition;

public class NotAuthorisedMixException extends MixException {
	private static final long serialVersionUID = 1L;

	public NotAuthorisedMixException(String message, Throwable cause) {
		super(Condition.not_authorized, message, cause);
	}

	public NotAuthorisedMixException(String message) {
		super(Condition.not_authorized, message);
	}

	public NotAuthorisedMixException(Throwable cause) {
		super(Condition.not_authorized, cause);
	}
}
