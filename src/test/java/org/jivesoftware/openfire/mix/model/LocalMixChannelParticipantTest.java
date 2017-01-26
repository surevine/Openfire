package org.jivesoftware.openfire.mix.model;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.jmock.Expectations;
import org.jmock.Mockery;
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
	
	private static final String[] SUPPORTED_SUBSCRIPTIONS = {LocalMixChannel.NODE_MESSAGES, LocalMixChannel.NODE_PARTICIPANTS};
	
	private static final String[] EXTENDED_SUBSCRIPTIONS = {LocalMixChannel.NODE_PARTICIPANTS, "ignore", "ignore_also"};
	
	private Mockery mockContext = new Mockery();

	private MixChannel mockMixChannel = mockContext.mock(MixChannel.class);
	
	public LocalMixChannelParticipantTest() {
		

	}
	
	@Test
	public void thatProxyJidIsReturnedByDefault() {

		mockContext.checking(new Expectations() {{
	    	one(mockMixChannel).getNodesAsStrings();
	    	will(returnValue(new HashSet<String>(Arrays.asList(SUPPORTED_SUBSCRIPTIONS))));
	    }});
		
		LocalMixChannelParticipant fixture = new LocalMixChannelParticipant(PROXY_CHANNEL_JID, TEST_USERS_JID, mockMixChannel);
		
		assertEquals(PROXY_CHANNEL_JID, fixture.getJid());
	}
	
	@Test
	public void thatOnlySupportedSubscriptionsAreKept() {
		
		mockContext.checking(new Expectations() {{
	    	exactly(2).of(mockMixChannel).getNodesAsStrings();
	    	will(returnValue(new HashSet<String>(Arrays.asList(SUPPORTED_SUBSCRIPTIONS))));
	    }});
		
		LocalMixChannelParticipant fixture = new LocalMixChannelParticipant(PROXY_CHANNEL_JID, TEST_USERS_JID, mockMixChannel, new HashSet<String>(Arrays.asList(EXTENDED_SUBSCRIPTIONS)));
		
		assertEquals(1, fixture.getSubscriptions().size());
	}

}
