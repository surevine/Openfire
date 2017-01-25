package org.jivesoftware.openfire.mix.handler;

import org.jivesoftware.openfire.mix.MixService;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.xmpp.packet.JID;

public class MixRequestContextImpl implements MixRequestContext {
	private JID actor;
	
	private MixService mixService;
	
	private MixChannel mixChannel;
	
	private boolean isParticipantChecked = false;
	
	private MixChannelParticipant participant = null;

	public MixRequestContextImpl(JID recipient, MixService mixService, MixChannel mixChannel) {
		this.actor = recipient;
		this.mixService = mixService;
		this.mixChannel = mixChannel;
	}

	public JID getRecipient() {
		return actor;
	}

	public MixService getMixService() {
		return mixService;
	}

	public MixChannel getMixChannel() {
		return mixChannel;
	}

	@Override
	public MixChannelParticipant getMixChannelParticipant() {
		if(isParticipantChecked) {
			return participant;
		}
		
		isParticipantChecked = true;
		
		if(mixChannel == null) {
			return null;
		}
		
		participant = mixChannel.getParticipantByJID(actor);
		
		return participant;
	}
}
