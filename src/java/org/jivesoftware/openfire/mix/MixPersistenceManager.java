package org.jivesoftware.openfire.mix;

import java.util.Collection;

import org.jivesoftware.openfire.XMPPServer;

public interface MixPersistenceManager {
	Collection<MixChannel> loadChannels(MixService mixService) throws MixPersistenceException;

	Collection<MixService> loadServices(XMPPServer xmppServer) throws MixPersistenceException;
}
