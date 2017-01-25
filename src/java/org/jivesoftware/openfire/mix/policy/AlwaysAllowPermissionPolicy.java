package org.jivesoftware.openfire.mix.policy;

public class AlwaysAllowPermissionPolicy<A, T> implements PermissionPolicy<A, T> {

	@Override
	public boolean checkPermission(A actor, T subject, Action action) {
		return true;
	}

}
