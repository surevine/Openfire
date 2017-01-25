package org.jivesoftware.openfire.mix.policy;

import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityMode;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;

public class MandatoryChannelJidVisibilityVoter<A extends MixChannelParticipant, T extends MixChannel> implements PermissionVoter<A, T> {

	@Override
	public PolicyVoterOutcome vote(A actor, T subject, PermissionPolicy.Action action) {
		if(subject.getJidVisibilityMode() == ChannelJidVisibilityMode.MANDATORY_HIDDEN) {
			return PolicyVoterOutcome.DENY;
		}
		
		return PolicyVoterOutcome.ALLOW;
	}
	
}
