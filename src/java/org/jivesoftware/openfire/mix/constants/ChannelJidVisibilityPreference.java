package org.jivesoftware.openfire.mix.constants;

/**
 * The JID Visibility Preference for the user on the channel.
 */
public enum ChannelJidVisibilityPreference {
	NO_PREFERENCE(0), PREFER_HIDDEN(1), ENFORCE_HIDDEN(2), ENFORCE_VISIBLE(3);

	private int id;

	private ChannelJidVisibilityPreference(final int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public static ChannelJidVisibilityPreference fromId(int id) {
		for (ChannelJidVisibilityPreference jvp : values()) {
			if (jvp.id == id) {
				return jvp;
			}
		}
		
		throw new IndexOutOfBoundsException("ID " + id + " not found");
	}
}