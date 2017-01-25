package org.jivesoftware.openfire.mix.policy;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.openfire.mix.model.MixChannelJidMapNodeItem;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;

public class MixChannelJidMapNodeItemPermissionPolicy
		implements PermissionPolicy<MixChannelParticipant, MixChannelJidMapNodeItem> {

	VoterBasedPermissionPolicy<MixChannelParticipant, MixChannelJidMapNodeItem> delegate;

	public MixChannelJidMapNodeItemPermissionPolicy() {
		List<PermissionVoter<MixChannelParticipant, MixChannelJidMapNodeItem>> voters = new ArrayList<>();

		voters.add(new AdminCanDoAnythingPermissionVoter<MixChannelParticipant, MixChannelJidMapNodeItem>());

		delegate = new VoterBasedPermissionPolicy<>(voters);
	}

	@Override
	public boolean checkPermission(MixChannelParticipant actor, MixChannelJidMapNodeItem subject,
			org.jivesoftware.openfire.mix.policy.PermissionPolicy.Action action) {
		return delegate.checkPermission(actor, subject, action);
	}
}
