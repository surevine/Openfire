package org.jivesoftware.openfire.mix.exception;

public class CannotCreateMixChannelException extends Exception {
	private static final long serialVersionUID = 1L;

	private String newChannelName;
	
	public CannotCreateMixChannelException(String newChannelName) {
		super("Cannot create " + newChannelName);
		this.setNewChannelName(newChannelName);
	}

	public String getNewChannelName() {
		return newChannelName;
	}

	public void setNewChannelName(String newChannelName) {
		this.newChannelName = newChannelName;
	}
}
