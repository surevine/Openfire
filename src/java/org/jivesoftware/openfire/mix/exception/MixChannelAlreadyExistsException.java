package org.jivesoftware.openfire.mix.exception;

public class MixChannelAlreadyExistsException extends Exception {
	private static final long serialVersionUID = 1L;

	private String newChannelName;
	
	public MixChannelAlreadyExistsException(String newChannelName) {
		super("Channel " + newChannelName + " already exists");
		this.setNewChannelName(newChannelName);
	}

	public String getNewChannelName() {
		return newChannelName;
	}

	public void setNewChannelName(String newChannelName) {
		this.newChannelName = newChannelName;
	}
}
