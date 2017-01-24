package org.jivesoftware.openfire.mix.exception;

public class CannotDestroyMixChannelException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String targetName;
	
	public CannotDestroyMixChannelException(String channelName, String reason) {
		super(reason);
		this.setNewChannelName(targetName);
	}

	public String getNewChannelName() {
		return targetName;
	}

	public void setNewChannelName(String newChannelName) {
		this.targetName = newChannelName;
	}
}
