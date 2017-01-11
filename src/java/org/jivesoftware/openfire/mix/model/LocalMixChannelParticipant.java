package org.jivesoftware.openfire.mix.model;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.codec.digest.Md5Crypt;
import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityPreference;
import org.xmpp.packet.JID;

public class LocalMixChannelParticipant implements MixChannelParticipant {

	private JID proxyJid;
	
	private JID jid;
	
	private MixChannel channel;
	
	private Set<String> subscriptions;
	
	private String nick;
	
	// TODO - Defaulting to ENFORCE_HIDDEN in the short-term, this will need to be fixed.
	private ChannelJidVisibilityPreference jvp = ChannelJidVisibilityPreference.ENFORCE_HIDDEN;
	
	public LocalMixChannelParticipant(JID proxyJid, JID jid, MixChannel channel, Set<String> subscriptions) {
		this.proxyJid = proxyJid;
		this.jid = jid;
		this.channel = channel;
		this.subscriptions = subscriptions;
		this.nick = Md5Crypt.md5Crypt(jid.toBareJID().getBytes()); // Not sure what this should be yet
	}

	@Override
	public JID getJid() {
		
		switch (jvp) {
		case PREFER_HIDDEN:
			return this.proxyJid;
		case ENFORCE_HIDDEN:
			return this.proxyJid;
		case ENFORCE_VISIBLE:
			return this.jid;
		default:
			return this.proxyJid;
		}
	}

	@Override
	public MixChannel getChannel() {
		return channel;
	}

	@Override
	public Set<String> getSubscriptions() {
		return Collections.unmodifiableSet(subscriptions);
	}

	@Override
	public String getNick() {
		return nick;
	}

	@Override
	public ChannelJidVisibilityPreference getJidVisibilityPreference() {
		return jvp;
	}

	public void setJidVisibilityPreference(ChannelJidVisibilityPreference jvp) {
		this.jvp = jvp;
	}
}
