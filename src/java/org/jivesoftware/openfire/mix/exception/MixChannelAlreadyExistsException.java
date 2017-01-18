package org.jivesoftware.openfire.mix.exception;

import org.jivesoftware.openfire.mix.model.MixChannel;

public class MixChannelAlreadyExistsException extends Exception {
	private static final long serialVersionUID = 1L;

	private String newChannelName;
	
	private MixChannel existingChannel;
	
	public MixChannelAlreadyExistsException(String newChannelName, MixChannel existingChannel) {
		this.setNewChannelName(newChannelName);
		this.setExistingChannel(existingChannel);
	}

	public String getNewChannelName() {
		return newChannelName;
	}

	public void setNewChannelName(String newChannelName) {
		this.newChannelName = newChannelName;
	}

	public MixChannel getExistingChannel() {
		return existingChannel;
	}

	public void setExistingChannel(MixChannel existingChannel) {
		this.existingChannel = existingChannel;
	}

}
