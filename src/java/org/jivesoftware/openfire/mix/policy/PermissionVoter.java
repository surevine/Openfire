package org.jivesoftware.openfire.mix.policy;

import org.jivesoftware.openfire.mix.handler.MixRequestContext;

public interface PermissionVoter<T> {
	public enum PolicyVoterOutcome {
		FORCE_ALLOW,
		ALLOW,
		DENY
	}
	
	PolicyVoterOutcome vote(MixRequestContext context, T subject, PermissionPolicy.Action action);
}
