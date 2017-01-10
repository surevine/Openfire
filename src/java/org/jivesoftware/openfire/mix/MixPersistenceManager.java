package org.jivesoftware.openfire.mix;

import java.util.Collection;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.mix.model.MixChannel;

public interface MixPersistenceManager {
	Collection<MixChannel> loadChannels(MixService mixService) throws MixPersistenceException;

	Collection<MixService> loadServices(XMPPServer xmppServer) throws MixPersistenceException;
}
