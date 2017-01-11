package org.jivesoftware.openfire.mix.model;

import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.Test;
import org.xmpp.packet.JID;

public class LocalMixChannelParticipantTest {
	
	private static final String TEST_USER = "hag66";

	private final static String TEST_MIX_CHANNEL_NAME = "coven";
	
	private final static String PROXY_MIX_CHANNEL_NAME = TEST_MIX_CHANNEL_NAME + "+12345";
	
	private final static String TEST_SERVICE_DOMAIN = "shakespeare.example";
	
	private final static String TEST_MIX_DOMAIN = "mix." + TEST_SERVICE_DOMAIN;

	private static final JID TEST_USERS_JID = new JID(TEST_USER, TEST_SERVICE_DOMAIN, null);
    
	private static final JID PROXY_CHANNEL_JID = new JID(PROXY_MIX_CHANNEL_NAME, TEST_MIX_DOMAIN, null);
	
	@Test
	public void thatProxyJidIsReturnedByDefault() {
		LocalMixChannelParticipant fixture = new LocalMixChannelParticipant(PROXY_CHANNEL_JID, TEST_USERS_JID, null, Collections.<String> emptySet());
		
		assertEquals(PROXY_CHANNEL_JID, fixture.getJid());
	}

}
