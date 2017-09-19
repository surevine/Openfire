package org.jivesoftware.openfire.mix.constants;

/**
 * The JID Visibility Mode for the channel.
 */
public enum ChannelJidVisibilityMode {
	VISIBLE(0), HIDDEN(1), MANDATORY_VISIBLE(2), MANDATORY_HIDDEN(3);

	private int id;

	private ChannelJidVisibilityMode(final int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}
	
	public static ChannelJidVisibilityMode fromId(int id) {
		for(ChannelJidVisibilityMode jvm : values()) {
			if(jvm.id == id) {
				return jvm;
			}
		}
		throw new IndexOutOfBoundsException("ID " + id + " not found");
	}
}