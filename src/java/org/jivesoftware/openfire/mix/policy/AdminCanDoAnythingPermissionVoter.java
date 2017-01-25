package org.jivesoftware.openfire.mix.policy;

import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant.Role;

public class AdminCanDoAnythingPermissionVoter<A extends MixChannelParticipant, T extends Object> implements PermissionVoter<A, T> {

	@Override
	public PolicyVoterOutcome vote(A actor, T subject, PermissionPolicy.Action action){
		if ((actor != null) && actor.getRole().canPerformRoleOf(Role.ADMINISTRATOR)) {
			return PolicyVoterOutcome.FORCE_ALLOW;
		}
		
		return PolicyVoterOutcome.ALLOW;
	}
}
