package org.jivesoftware.openfire.mix.exception;

public class CannotLeaveMixChannelException  extends Exception {
	private static final long serialVersionUID = 1L;

	private String newChannelName;
	
	public CannotLeaveMixChannelException(String channelName, String reason) {
		super(reason);
		this.setNewChannelName(newChannelName);
	}

	public String getNewChannelName() {
		return newChannelName;
	}

	public void setNewChannelName(String newChannelName) {
		this.newChannelName = newChannelName;
	}
}
