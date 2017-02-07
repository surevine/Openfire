package org.jivesoftware.openfire.mix.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;
import org.jivesoftware.openfire.mix.MixManager;
import org.jivesoftware.openfire.mix.model.MixChannel.MixChannelParticipantsListener;
import org.jivesoftware.openfire.pubsub.LeafNode;
import org.jivesoftware.openfire.pubsub.PublishedItem;
import org.xmpp.packet.JID;

public class MixChannelParticipantsNode extends LeafNode {

	public static final String NODE_ID = "urn:xmpp:mix:nodes:participants";
	
	private MixChannel mixChannel;
	
	public MixChannelParticipantsNode(final MixChannel channel) {
		super(channel, channel.getRootCollectionNode(), NODE_ID, channel.getJID());
		
		this.mixChannel = channel;
		
		channel.addParticipantsListener(new MixChannelParticipantsListener() {
			
			@Override
			public void onParticipantRemoved(MixChannelParticipant participant) {
				deleteItems(Arrays.asList(createPublishedItem(participant)));
			}
			
			@Override
			public void onParticipantAdded(MixChannelParticipant participant) {
				publishItems(channel.getJID(), Arrays.asList(mapItem(participant)));
			}

			@Override
			public void onMessageReceived(MixChannelMessage mcMessage) {
				// TODO Auto-generated method stub
				
			}
		});
	}

	@Override
	public void saveToDB() {
		// We don't persist this node
	}

	@Override
	public synchronized List<PublishedItem> getPublishedItems(int recentItems) {
		List<PublishedItem> items = new ArrayList<>();
		
		for(MixChannelParticipant participant : mixChannel.getParticipants()) {
			PublishedItem item = new PublishedItem(this, mixChannel.getJID(), participant.getJid().toBareJID(), new Date());
			
			item.setPayload(mapItem(participant));
			
			items.add(item);
		}
		
		return items;
	}
	
	private Element mapItem(MixChannelParticipant participant) {
		Element payload = new DefaultElement("participant", MixManager.MIX_NAMESPACE);
		
		payload.element("participant");
		
		return payload;
	}

	private PublishedItem createPublishedItem(MixChannelParticipant participant) {
		PublishedItem item = new PublishedItem(this, mixChannel.getJID(), participant.getJid().toBareJID(), new Date());
		
		item.setPayload(mapItem(participant));
		
		return item;
	}
	
	@Override
	public PublishedItem getPublishedItem(String itemID) {
		MixChannelParticipant participant = mixChannel.getParticipantByProxyJID(new JID(itemID));
		
		if(participant == null) {
			return null;
		}

		return createPublishedItem(participant);
	}
}
