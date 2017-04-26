package org.jivesoftware.openfire.mix;

import java.util.Collection;
import java.util.Set;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;

public interface MixPersistenceManager {
	Collection<MixChannel> loadChannels(MixService mixService) throws MixPersistenceException;

	Collection<MixService> loadServices(XMPPServer xmppServer) throws MixPersistenceException;
	
	MixChannel save(MixChannel toPersist) throws MixPersistenceException;
	
	boolean delete(MixChannel toDelete) throws MixPersistenceException;

	MixChannelParticipant save(MixChannelParticipant mcp) throws MixPersistenceException;
	
	MixChannelParticipant update(MixChannelParticipant mcp) throws MixPersistenceException;
	
	boolean delete(MixChannelParticipant toDelete) throws MixPersistenceException;
	
	Collection<MixChannelParticipant> findByChannel(MixChannel channel) throws MixPersistenceException;
	
	Set<String> findByParticipant(MixChannelParticipant participant) throws MixPersistenceException;
}