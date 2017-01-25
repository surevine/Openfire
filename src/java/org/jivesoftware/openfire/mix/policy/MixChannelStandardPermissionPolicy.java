package org.jivesoftware.openfire.mix.policy;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;

public class MixChannelStandardPermissionPolicy implements PermissionPolicy<MixChannelParticipant, MixChannel> {

	VoterBasedPermissionPolicy<MixChannelParticipant, MixChannel> delegate;

	public MixChannelStandardPermissionPolicy() {
		List<PermissionVoter<MixChannelParticipant, MixChannel>> voters = new ArrayList<>();

		voters.add(new AdminCanDoAnythingPermissionVoter<MixChannelParticipant, MixChannel>());

		delegate = new VoterBasedPermissionPolicy<>(voters);
	}

	@Override
	public boolean checkPermission(MixChannelParticipant actor, MixChannel subject, Action action) {
		return delegate.checkPermission(actor, subject, action);
	}

}
