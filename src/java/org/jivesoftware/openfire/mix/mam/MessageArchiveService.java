package org.jivesoftware.openfire.mix.mam;

import org.jivesoftware.openfire.mix.model.MixChannelMessage;
import org.xmpp.packet.IQ;

public interface MessageArchiveService {
	
	String MAM_NAMESPACE = "urn:xmpp:mam:1";
	
	String RSM_NAMESPACE = "http://jabber.org/protocol/rsm";
	
	IQ query(IQ query);
	
	String archive(MixChannelMessage archive);

}
