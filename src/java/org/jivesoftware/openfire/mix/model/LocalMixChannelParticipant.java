package org.jivesoftware.openfire.mix.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.digest.Md5Crypt;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.MixPersistenceManager;
import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityPreference;
import org.jivesoftware.openfire.mix.exception.CannotUpdateMixChannelSubscriptionException;
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

	private MixPersistenceManager subscriptionsRepository;
	
	public LocalMixChannelParticipant(JID proxyJid, JID jid, MixChannel channel, Set<String> requestedSubscriptions, MixPersistenceManager mpm) {
		this(proxyJid, jid, channel, mpm);
		
		this.subscriptions = new HashSet<String>(requestedSubscriptions);

		// Only retain the subscriptions in the request that are in the supported set
		this.subscriptions.retainAll(channel.getNodesAsStrings());
	}
	
	/**
	 * Constructor that allows a participant to subscribe to all supported nodes
	 * 
	 * @param proxyJid
	 * @param jid
	 * @param channel
	 */
	public LocalMixChannelParticipant(JID proxyJid, JID jid, MixChannel channel, MixPersistenceManager mpm) {
		this.proxyJid = proxyJid;
		this.jid = jid;
		this.channel = channel;
		this.nick = Md5Crypt.md5Crypt(jid.toBareJID().getBytes()); // Not sure what this should be yet
		this.subscriptionsRepository = mpm;
		
		this.subscriptions = new HashSet<String>(channel.getNodesAsStrings());		
	}


	/**
	 * Constructor to be used when serialising form the database.
	 * 
	 * @param id
	 * @param proxyJid2
	 * @param realJid
	 * @param channel2
	 * @param mpm
	 */
	public LocalMixChannelParticipant(long id, JID proxyJid, JID realJid, MixChannel channel, MixPersistenceManager mpm) {
		this(proxyJid, realJid, channel, mpm);
		this.id = id;
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
	public Role getRole() {
		if(getRealJid().asBareJID().equals(channel.getOwner().asBareJID())) {
			return Role.OWNER;
		}
		
		return Role.PARTICIPANT;
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

	/**
	 * Allows a user to update the nodes they subscribe to.
	 * 
	 * @see org.jivesoftware.openfire.mix.model.MixChannelParticipant#updateSubscriptions(java.util.Set)
	 */
	@Override
	public void updateSubscriptions(Set<String> subs) throws CannotUpdateMixChannelSubscriptionException {
		
		Set<String> tmpCopy = new HashSet<String>(subs);

		// Only retain the subscriptions in the request that are in the supported set
		tmpCopy.retainAll(channel.getNodesAsStrings());
		
		if (tmpCopy.isEmpty()) {
			throw new CannotUpdateMixChannelSubscriptionException(channel.getName(), "Not valid subscription list");
		} else {
			this.setSubscriptions(tmpCopy);
			try {
				subscriptionsRepository.update(this);
			} catch (MixPersistenceException e) {
				throw new CannotUpdateMixChannelSubscriptionException(channel.getName(), e.getMessage());
			}
		}
	}

}
