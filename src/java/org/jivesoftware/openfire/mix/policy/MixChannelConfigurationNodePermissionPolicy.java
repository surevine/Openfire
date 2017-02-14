package org.jivesoftware.openfire.mix.policy;

import org.jivesoftware.openfire.mix.MixChannelNode;
import org.jivesoftware.openfire.mix.model.MixChannelConfigurationNodeItem;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant.Role;

public class MixChannelConfigurationNodePermissionPolicy extends VoterBasedPermissionPolicy<MixChannelNode<MixChannelConfigurationNodeItem>> {
	public MixChannelConfigurationNodePermissionPolicy() {
		addVoter(new OnlyAllowRoleToDoActionPermissionVoter<MixChannelNode<MixChannelConfigurationNodeItem>>(Role.OWNER, Action.PUBLISH));
		addVoter(new OnlyAllowRoleToDoActionPermissionVoter<MixChannelNode<MixChannelConfigurationNodeItem>>(Role.PARTICIPANT, Action.VIEW));
	}
}
