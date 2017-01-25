package org.jivesoftware.openfire.mix.policy;

public interface PermissionPolicy<A, T> {
	public enum Action {
		VIEW
	}
	
	boolean checkPermission(A actor, T subject, Action action);
}
