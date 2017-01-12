package org.jivesoftware.openfire.mix.handler;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jivesoftware.openfire.mix.model.LocalMixChannelParticipant;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;

public class MixChannelJoinPacketHandlerTest {
	
    private static final String TEST_USER = "hag66";

    private final static String TEST_MIX_CHANNEL_NAME = "coven";
    
    private final static String TEST_SERVICE_DOMAIN = "shakespeare.example";
    
    private final static String TEST_MIX_DOMAIN = "mix." + TEST_SERVICE_DOMAIN;

    private static final JID TEST_USERS_JID = new JID(TEST_USER, TEST_SERVICE_DOMAIN, null);
    private static final JID MIX_CHANNEL_JID = new JID(TEST_MIX_CHANNEL_NAME, TEST_MIX_DOMAIN, null);
    
    private static final String []PARTIAL_NODE_SET = {"urn:xmpp:mix:nodes:messages", "urn:xmpp:mix:nodes:participants", "urn:xmpp:mix:nodes:subject", "urn:xmpp:mix:nodes:config"};
    
    private static final DocumentFactory docFactory = DocumentFactory.getInstance();
	
	Mockery context = new Mockery();
    
    private MixChannel mockMixChannel; 
	
	private MixChannelJoinPacketHandler fixture = new MixChannelJoinPacketHandler();
	
	public MixChannelJoinPacketHandlerTest() {
		mockMixChannel = context.mock(MixChannel.class);
	}
	
	@Before
	public void setUp() {
        context.checking(new Expectations() {{
            allowing(mockMixChannel);
        }});
	}
	
	@Test
	public void testSuccessfulJoinRequest() {
		
		final Set<String> subscriptions = new HashSet<String>(Arrays.asList(PARTIAL_NODE_SET));
		
		final MixChannelParticipant mcp = new LocalMixChannelParticipant(null, TEST_USERS_JID, mockMixChannel, subscriptions);
		
        // Create valid ID, with subscriptions
        IQ join = createJoinRequest(PARTIAL_NODE_SET);
		
        context.checking(new Expectations() {{
            one(mockMixChannel).addParticipant(with(equal(TEST_USERS_JID)), with(equal(subscriptions)));
            will(returnValue(mcp));
        }});

        IQ response = fixture.processIQ(mockMixChannel, join);
        
        // There should be the same number of subscribe elements in the response as there were in the request.
        assertEquals(PARTIAL_NODE_SET.length, response.getChildElement().elements().size());
		
	}
	
	@Test(expected=NotImplementedException.class)
	public void processPresence() {
		fixture.processPresence(mockMixChannel, null);
	}
	
	@Test(expected=NotImplementedException.class)
	public void processMessagwe() {
		fixture.processMessage(mockMixChannel, null);
	}
	
    private IQ createJoinRequest(String []nodes) {
        IQ joinRequest = new IQ(IQ.Type.set);
        
        joinRequest.setTo(MIX_CHANNEL_JID);
        joinRequest.setFrom(TEST_USERS_JID);
        
        Element join = docFactory.createElement("join", "urn:xmpp:mix:0");
        
        for (int i = 0; i < nodes.length; i++) {
            Element sub = join.addElement("subscribe");
            sub.addAttribute("node", nodes[i]);
        }
        
        joinRequest.setChildElement(join);
        
        return joinRequest;
    }
	
	static class IQMatcher extends TypeSafeMatcher<IQ> {

		private IQ expectation;
		
		public IQMatcher(IQ expectation) {
			this.expectation = expectation;
		}
		
		@Override
		public void describeTo(Description arg0) {
			// TODO Implement
		}

		@Override
		protected boolean matchesSafely(IQ result) {
			// First check the types
			if (expectation.getType().equals(result.getType())) {
				Element expectedElement = expectation.getChildElement();
				Element resultElement = result.getChildElement();
				
				// Then check the child XML.
				if (expectedElement.asXML().equals(resultElement.asXML())) {
					return true;					
				}
			}
			
			return false;

		}
		
		@Factory
		public static Matcher<IQ> iqMatcher(IQ expection) {
		    return new IQMatcher(expection);
		}

	}

}
