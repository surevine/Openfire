package org.jivesoftware.openfire.mix.exception;

public class CannotUpdateMixChannelSubscriptionException extends Exception {
	private static final long serialVersionUID = 1L;

	private String channelName;
	
	public CannotUpdateMixChannelSubscriptionException(String channelName, String reason) {
		super(reason);
		this.setChannelName(channelName);
	}

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}
}
