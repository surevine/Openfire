package org.jivesoftware.openfire.mix.policy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.openfire.mix.handler.MixRequestContext;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant.Role;
import org.jivesoftware.openfire.mix.policy.PermissionPolicy.Action;

public class OnlyAllowRoleToDoActionPermissionVoter<T extends Object> implements PermissionVoter<T> {

	private Role role;
	
	private Set<Action> actions;
	
	public OnlyAllowRoleToDoActionPermissionVoter(Role role, Action...actions) {
		this.role = role;
		this.actions = new HashSet<>(Arrays.asList(actions));
	}
	
	@Override
	public PolicyVoterOutcome vote(MixRequestContext context,
			T subject, Action action) {
		if(!actions.contains(action)) {
			return PolicyVoterOutcome.ALLOW;
		}
		
		Role actorRole = context.getMixChannelParticipant().getRole();
		
		if(actorRole.canPerformRoleOf(role)) {
			return PolicyVoterOutcome.ALLOW;
		} else {
			return PolicyVoterOutcome.DENY;
		}
	}

}
