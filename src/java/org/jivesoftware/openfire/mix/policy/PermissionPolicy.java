package org.jivesoftware.openfire.mix.policy;

import org.jivesoftware.openfire.mix.handler.MixRequestContext;

public interface PermissionPolicy<T> {
	public enum Action {
		VIEW,
		JOIN
	}
	
	boolean checkPermission(MixRequestContext context, T subject, Action action);
}
