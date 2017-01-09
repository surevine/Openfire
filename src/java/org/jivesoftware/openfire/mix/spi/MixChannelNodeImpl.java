package org.jivesoftware.openfire.mix.spi;

public class MixChannelNodeImpl implements MixChannelNode {

	private NodeType type;

	public MixChannelNodeImpl(NodeType type) {
		this.type = type;
	}

	@Override
	public NodeType getType() {
		return type;
	}
}
