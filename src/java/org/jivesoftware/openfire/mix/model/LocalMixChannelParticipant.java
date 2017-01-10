package org.jivesoftware.openfire.mix.model;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.codec.digest.Md5Crypt;
import org.xmpp.packet.JID;

public class LocalMixChannelParticipant implements MixChannelParticipant {

	private JID jid;
	
	private MixChannel channel;
	
	private Set<String> subscriptions;
	
	private String nick;
	
	public LocalMixChannelParticipant(JID jid, MixChannel channel, Set<String> subscriptions) {
		this.jid = jid;
		this.channel = channel;
		this.subscriptions = subscriptions;
		this.nick = Md5Crypt.md5Crypt(jid.toBareJID().getBytes()); // Not sure what this should be yet
	}

	@Override
	public JID getJid() {
		return jid;
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
}
