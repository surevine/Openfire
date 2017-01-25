package org.jivesoftware.openfire.mix.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.digest.Md5Crypt;
import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityPreference;
import org.xmpp.packet.JID;

public class LocalMixChannelParticipant implements MixChannelParticipant {
	
	private long id;

	private JID proxyJid;
	
	private JID jid;
	
	private MixChannel channel;
	
	private Set<String> subscriptions;
	
	private String nick;
	
	// TODO - Defaulting to ENFORCE_HIDDEN in the short-term, this will need to be fixed.
	private ChannelJidVisibilityPreference jvp = ChannelJidVisibilityPreference.ENFORCE_HIDDEN;
	
	public LocalMixChannelParticipant(JID proxyJid, JID jid, MixChannel channel, Set<String> subscriptions) {
		this(proxyJid, jid, channel);
		
		// Only retain the subscriptions that are the intersection of both the supplied set and the ones supported by the channel
		Set<String> intersection = new HashSet<String>(channel.getNodesAsStrings());
		intersection.retainAll(subscriptions);
		this.subscriptions = new HashSet<String>(intersection);
	}
	
	/**
	 * Constructor that allows a participant to subscribe to all supported nodes
	 * 
	 * @param proxyJid
	 * @param jid
	 * @param channel
	 */
	public LocalMixChannelParticipant(JID proxyJid, JID jid, MixChannel channel) {
		this.proxyJid = proxyJid;
		this.jid = jid;
		this.channel = channel;
		this.nick = Md5Crypt.md5Crypt(jid.toBareJID().getBytes()); // Not sure what this should be yet
		
		this.subscriptions = new HashSet<String>(channel.getNodesAsStrings());		
	}


	public long getID() {
		return id;
	}

	public void setID(long id) {
		this.id = id;
	}
	
	@Override
	public JID getRealJid() {
		return this.jid;
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

	@Override
	public boolean subscribesTo(String nodeName) {
		if (subscriptions.contains(nodeName)) {
			return true;
		} 
		
		return false;
	}

	@Override
	public JID getJid() {
		return proxyJid;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((proxyJid == null) ? 0 : proxyJid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LocalMixChannelParticipant other = (LocalMixChannelParticipant) obj;
		if (proxyJid == null) {
			if (other.proxyJid != null)
				return false;
		} else if (!proxyJid.equals(other.proxyJid))
			return false;
		return true;
	}


	@Override
	public void setSubscriptions(Set<String> subs) {
		this.subscriptions = subs;
	}

}
