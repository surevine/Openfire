package org.jivesoftware.openfire.mix.model;

import java.util.Set;

import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityPreference;
import org.jivesoftware.openfire.mix.exception.CannotUpdateMixChannelSubscriptionException;
import org.xmpp.packet.JID;

public interface MixChannelParticipant {
	public enum Role {
		OWNER, ADMINISTRATOR, PARTICIPANT;

		public boolean canPerformRoleOf(Role role) {
			switch (this) {
			case OWNER:
				return true;

			case ADMINISTRATOR:
				return ((role == PARTICIPANT) || (role == ADMINISTRATOR));

			case PARTICIPANT:
				return (role == PARTICIPANT);

			default:
				return false;
			}
		}
	}

	JID getRealJid();

	String getNick();

	MixChannel getChannel();

	Set<String> getSubscriptions();

	boolean subscribesTo(String nodeName);

	ChannelJidVisibilityPreference getJidVisibilityPreference();

	JID getJid();

	long getID();

	void setID(long nextUniqueID);
	
	void setSubscriptions(Set<String> subs);

	Role getRole();

	void updateSubscriptions(Set<String> subs) throws CannotUpdateMixChannelSubscriptionException;
}
