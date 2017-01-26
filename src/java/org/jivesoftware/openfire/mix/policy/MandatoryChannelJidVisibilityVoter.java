package org.jivesoftware.openfire.mix.policy;

import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityMode;
import org.jivesoftware.openfire.mix.handler.MixRequestContext;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.policy.PermissionPolicy.Action;

public class MandatoryChannelJidVisibilityVoter<T extends MixChannel> implements PermissionVoter<T> {

	@Override
	public org.jivesoftware.openfire.mix.policy.PermissionVoter.PolicyVoterOutcome vote(MixRequestContext context,
			T subject, Action action) {
		if(subject.getJidVisibilityMode() == ChannelJidVisibilityMode.MANDATORY_HIDDEN) {
			return PolicyVoterOutcome.DENY;
		}
		
		return PolicyVoterOutcome.ALLOW;
	}
	
}
