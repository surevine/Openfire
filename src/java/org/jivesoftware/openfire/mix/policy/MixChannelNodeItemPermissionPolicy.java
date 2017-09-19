package org.jivesoftware.openfire.mix.policy;

import org.jivesoftware.openfire.mix.MixChannelNode;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelNodeItem;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;

public interface MixChannelNodeItemPermissionPolicy<T extends MixChannelNodeItem> {
	boolean canViewNodeItem(MixChannelParticipant actor, MixChannel channel, MixChannelNode<T> node, T item);
}
