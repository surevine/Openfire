package org.jivesoftware.openfire.mix.policy;

import org.jivesoftware.openfire.mix.handler.MixRequestContext;

public class AlwaysAllowPermissionPolicy<T> implements PermissionPolicy<T> {

	@Override
	public boolean checkPermission(MixRequestContext context, T subject, Action action) {
		return true;
	}

}
