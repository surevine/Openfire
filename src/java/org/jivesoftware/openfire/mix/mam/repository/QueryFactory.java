package org.jivesoftware.openfire.mix.mam.repository;

import org.xmpp.packet.IQ;

public interface QueryFactory {
	
	Query create(MixChannelArchiveRepository repository, IQ queryIQ);

}
