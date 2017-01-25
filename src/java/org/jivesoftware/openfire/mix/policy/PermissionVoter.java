package org.jivesoftware.openfire.mix.policy;

public interface PermissionVoter<A, T> {
	public enum PolicyVoterOutcome {
		FORCE_ALLOW,
		ALLOW,
		DENY
	}
	
	PolicyVoterOutcome vote(A actor, T subject, PermissionPolicy.Action action);
}
