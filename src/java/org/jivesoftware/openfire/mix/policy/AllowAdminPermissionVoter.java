package org.jivesoftware.openfire.mix.policy;

import org.jivesoftware.openfire.mix.handler.MixRequestContext;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant.Role;

public class AllowAdminPermissionVoter<T extends Object> implements PermissionVoter<T> {

	@Override
	public PolicyVoterOutcome vote(MixRequestContext context, T subject, PermissionPolicy.Action action){
		MixChannelParticipant participant = context.getMixChannelParticipant();
		
		if ((participant != null) && participant.getRole().canPerformRoleOf(Role.ADMINISTRATOR)) {
			return PolicyVoterOutcome.FORCE_ALLOW;
		}
		
		return PolicyVoterOutcome.ALLOW;
	}
}
