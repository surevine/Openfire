package org.jivesoftware.openfire.mix.exception;

import java.util.Map;
import java.util.Map.Entry;

import org.xmpp.packet.PacketError;

public class MixFormValidationException extends NotAcceptableMixException {
	private static final long serialVersionUID = 1L;

	private Map<String, String> messages;
	
	public MixFormValidationException(Map<String, String> messages) {
		this.messages = messages;
	}

	@Override
	public PacketError getPacketError() {
		PacketError error = super.getPacketError();
		error.setText(getMessage());
		
		return error;
	}
	
	@Override
	public String getMessage() {
		StringBuilder str = new StringBuilder("Form validation failed: [");
		
		boolean notFirst = false;
		
		for(Entry<String, String> messageEntry : messages.entrySet()) {
			if(notFirst) {
				str.append(", ");
			} else {
				notFirst = true;
			}
			
			str.append(messageEntry.getKey());
			str.append(": ");
			str.append(messageEntry.getValue());
		}
	
		str.append("]");
		
		return str.toString();
	}
}
