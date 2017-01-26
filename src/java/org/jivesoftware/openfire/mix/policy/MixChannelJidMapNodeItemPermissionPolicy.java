package org.jivesoftware.openfire.mix.policy;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.openfire.mix.handler.MixRequestContext;
import org.jivesoftware.openfire.mix.model.MixChannelJidMapNodeItem;

public class MixChannelJidMapNodeItemPermissionPolicy
		implements PermissionPolicy<MixChannelJidMapNodeItem> {

	VoterBasedPermissionPolicy<MixChannelJidMapNodeItem> delegate;

	public MixChannelJidMapNodeItemPermissionPolicy() {
		List<PermissionVoter<MixChannelJidMapNodeItem>> voters = new ArrayList<>();

		voters.add(new AllowAdminPermissionVoter<MixChannelJidMapNodeItem>());

		delegate = new VoterBasedPermissionPolicy<>(voters);
	}

	@Override
	public boolean checkPermission(MixRequestContext context, MixChannelJidMapNodeItem subject, Action action) {
		return delegate.checkPermission(context, subject, action);
	}
}
