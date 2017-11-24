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

	public MixRequestContextImpl(JID actor, MixService mixService, MixChannel mixChannel) {
		this.actor = actor;
		this.mixService = mixService;
		this.mixChannel = mixChannel;
	}
	
	public MixRequestContextImpl(MixChannelParticipant recipient, MixService mixService, MixChannel mixChannel) {
		this.actor = recipient.getRealJid();
		this.participant = recipient;
		this.mixService = mixService;
		this.mixChannel = mixChannel;
		this.isParticipantChecked = true;
	}

	public JID getActor() {
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
		
		participant = mixChannel.getParticipantByRealJID(actor);
		
		return participant;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((actor == null) ? 0 : actor.hashCode());
		result = prime * result + ((mixChannel == null) ? 0 : mixChannel.hashCode());
		result = prime * result + ((mixService == null) ? 0 : mixService.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MixRequestContextImpl other = (MixRequestContextImpl) obj;
		if (actor == null) {
			if (other.actor != null)
				return false;
		} else if (!actor.equals(other.actor))
			return false;
		if (mixChannel == null) {
			if (other.mixChannel != null)
				return false;
		} else if (!mixChannel.equals(other.mixChannel))
			return false;
		if (mixService == null) {
			if (other.mixService != null)
				return false;
		} else if (!mixService.equals(other.mixService))
			return false;
		return true;
	}
}
