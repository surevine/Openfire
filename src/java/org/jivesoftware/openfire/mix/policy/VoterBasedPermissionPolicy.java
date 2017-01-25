package org.jivesoftware.openfire.mix.policy;

import java.util.List;

import org.jivesoftware.openfire.mix.policy.PermissionVoter.PolicyVoterOutcome;

public class VoterBasedPermissionPolicy<A, T> implements PermissionPolicy<A, T> {
	List<PermissionVoter<A, T>> voters;
	
	public VoterBasedPermissionPolicy(List<PermissionVoter<A, T>> voters) {
		this.voters = voters;
	}

	@Override
	/**
	 * Runs through the voters in turn applying the following logic:
	 * <ul>
	 *   <li>If one votes DENY then the decision is <code>false</code> for DENY and no further voters are evaluated
	 *   <li>If one votes FORCE_ALLOW then the decision is <code>true</code> for ALLOW and no further voters are evaluated
	 *   <li>If all voters are evaluated and vote ALLOW then the decision is <code>true</code> for ALLOW
	 * </ul>
	 */
	public boolean checkPermission(A actor, T subject,
			PermissionPolicy.Action action) {
		for(PermissionVoter<A, T> voter : voters) {
			PermissionVoter.PolicyVoterOutcome outcome = voter.vote(actor, subject, action);
			
			if(outcome == PolicyVoterOutcome.DENY) {
				return false;
			}
			
			if(outcome == PolicyVoterOutcome.FORCE_ALLOW) {
				return true;
			}
		}
		
		return true;
	}
}
