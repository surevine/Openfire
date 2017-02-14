package org.jivesoftware.openfire.mix.constants;

/**
 * The JID Visibility Mode for the channel.
 */
public enum ChannelJidVisibilityMode {
	HIDDEN(0, "jid-hidden"),
	VISIBLE(1, "jid-visible"),
	MANDATORY_HIDDEN(2, "jid-mandatory-hidden"),
	MANDATORY_VISIBLE(3, "jid-mandatory-visible");

	private int id;
	private String xmppValue;

	private ChannelJidVisibilityMode(final int id, final String xmppValue) {
		this.id = id;
		this.xmppValue = xmppValue;
	}

	public int getId() {
		return id;
	}

	public String getXmppValue() {
		return xmppValue;
	}
	
	public static ChannelJidVisibilityMode fromId(int id) {
		for(ChannelJidVisibilityMode jvm : values()) {
			if(jvm.id == id) {
				return jvm;
			}
		}
		throw new IndexOutOfBoundsException("ID " + id + " not found");
	}
	
	public static ChannelJidVisibilityMode fromXmppValue(String xmppValue) {
		for(ChannelJidVisibilityMode jvm : values()) {
			if(jvm.xmppValue.equals(xmppValue)) {
				return jvm;
			}
		}
		throw new IndexOutOfBoundsException("XMPP Value " + xmppValue + " not found");
		
	}
}