package org.jivesoftware.openfire.mix.model;

import java.util.Arrays;

import org.jivesoftware.openfire.mix.model.MixChannel.MixChannelParticipantsListener;
import org.jivesoftware.openfire.pubsub.LeafNode;

public class MixChannelMessagesNode extends LeafNode {

	public static final String NODE_ID = "urn:xmpp:mix:nodes:messages";
	
	private MixChannel mixChannel;
	
	public MixChannelMessagesNode(final MixChannel mixChannel) {
		super(mixChannel, mixChannel.getRootCollectionNode(), NODE_ID, mixChannel.getJID());
		
		this.mixChannel = mixChannel;
		
		setNotifyPublish(false);
		
		mixChannel.addParticipantsListener(new MixChannelParticipantsListener() {
			
			@Override
			public void onParticipantRemoved(MixChannelParticipant mcp) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onParticipantAdded(MixChannelParticipant participant) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onMessageReceived(MixChannelMessage mcMessage) {
				publishItems(mixChannel.getJID(), Arrays.asList(mcMessage.getMessage().getElement()));
			}
		});
	}
}
