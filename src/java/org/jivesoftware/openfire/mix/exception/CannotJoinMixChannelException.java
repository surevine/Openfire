package org.jivesoftware.openfire.mix.exception;

public class CannotJoinMixChannelException  extends Exception {
	private static final long serialVersionUID = 1L;

	private String newChannelName;
	
	public CannotJoinMixChannelException(String channelName, String reason) {
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
