package org.jivesoftware.openfire.mix.policy;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.openfire.mix.handler.MixRequestContext;
import org.jivesoftware.openfire.mix.model.MixChannel;

public class MixChannelStandardPermissionPolicy implements PermissionPolicy<MixChannel> {

	VoterBasedPermissionPolicy<MixChannel> delegate;

	public MixChannelStandardPermissionPolicy() {
		List<PermissionVoter<MixChannel>> voters = new ArrayList<>();

		voters.add(new AllowAdminPermissionVoter<MixChannel>());

		delegate = new VoterBasedPermissionPolicy<>(voters);
	}

	@Override
	public boolean checkPermission(MixRequestContext context, MixChannel subject, Action action) {
		return delegate.checkPermission(context, subject, action);
	}

}
