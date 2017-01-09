package org.jivesoftware.openfire.mix.spi;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.mix.MixService;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;

public class LocalMixChannelTest {
	
	private static final String TEST_USER = "hag66";

	private final static String TEST_MIX_CHANNEL_NAME = "coven";
	
	private final static String TEST_SERVICE_DOMAIN = "shakespeare.example";
	
	private final static String TEST_MIX_DOMAIN = "mix." + TEST_SERVICE_DOMAIN;

	private static final JID TEST_USERS_JID = new JID(TEST_USER, TEST_SERVICE_DOMAIN, null);
    private static final JID MIX_CHANNEL_JID = new JID(TEST_MIX_CHANNEL_NAME, TEST_MIX_DOMAIN, null);
    
	private static final String []PARTIAL_NODE_SET = {"urn:xmpp:mix:nodes:messages", "urn:xmpp:mix:nodes:participants", "urn:xmpp:mix:nodes:subject", "urn:xmpp:mix:nodes:config"};
	
	private static final DocumentFactory docFactory = DocumentFactory.getInstance();
	
	Mockery context = new Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};
    
	final MixService mockMixService = context.mock(MixService.class); 
	
	final Packet mockPacket = context.mock(Packet.class);
	
	final PacketRouter mockRouter = context.mock(PacketRouter.class);
	
	private LocalMixChannel fixture = new LocalMixChannel(mockMixService, TEST_MIX_CHANNEL_NAME, mockRouter);
	
	@Before
	public void setUp() {
        context.checking(new Expectations() {{
            allowing(mockMixService).getServiceDomain();
            will(returnValue(TEST_MIX_DOMAIN));
        }});
	}

	@Test(expected=IllegalArgumentException.class)
	public void thatInvalidPacketDestinationThrowsException() {

		// expectations
        context.checking(new Expectations() {{
            allowing(mockPacket).getTo();
            will(returnValue(new JID("wrongMixChannelName")));
        }});

		fixture.process(mockPacket);
		
		context.assertIsSatisfied();
	}
	
	@Test
	public void thatUserCanJoinChannelAndSubscribeToNodes() {	    
		
		// Create valid ID, with subscriptions
		IQ join = createJoinRequest(PARTIAL_NODE_SET);
	    
	    // Create the valid response
	    final IQ result = new IQ(IQ.Type.result, join.getID());
	    result.setFrom(MIX_CHANNEL_JID);
	    result.setTo(TEST_USERS_JID);
	    
	    Element joinRequestCopy = join.getChildElement().createCopy();
	    joinRequestCopy.addAttribute("jid", TEST_USERS_JID.toBareJID());
	    result.setChildElement(joinRequestCopy);
	
		// Expect that a single IQ is sent back from the router, with valid subscriptions
	    context.checking(new Expectations() {{
	    	one(mockRouter).route(with(IQMatcher.iqMatcher(result)));
	    }});
		
		// Process the IQ
		fixture.process(join);
	}
	
	
	@Test
	public void thatPartialJoinRequestSuccessful() {
		// Setup special participants only mix channel
		LocalMixChannel participantsOnlySupportedChannel = new LocalMixChannel(mockMixService, TEST_MIX_CHANNEL_NAME, mockRouter);
		MixChannelNode participants = new MixChannelNodeImpl(MixChannelNode.NodeType.participants);
		participantsOnlySupportedChannel.setSupportedNodes(Arrays.asList(participants));
		
		// Create request to join more nodes than supported
		IQ join = createJoinRequest(PARTIAL_NODE_SET);
		
	    // The resultant IQ should only contain the participants
	    final IQ partialJoinResult = new IQ(IQ.Type.result, join.getID());
	    partialJoinResult.setFrom(MIX_CHANNEL_JID);
	    partialJoinResult.setTo(TEST_USERS_JID);
	    
	    Element joinResult = docFactory.createElement("join", "urn:xmpp:mix:0");
	    joinResult.addAttribute("jid", TEST_USERS_JID.toBareJID());
	    
    	Element subscribeResultSubset = docFactory.createElement("subscribe");
    	subscribeResultSubset.addAttribute("node", MixChannelNode.NodeType.participants.toString());
    	joinResult.add(subscribeResultSubset);
    	
    	partialJoinResult.setChildElement(joinResult);
    	
		// Expect that a single IQ is sent back from the router, with subset of subscriptions
    	// And a 'participants' message is sent to each of the subscribers of the participants node.
	    context.checking(new Expectations() {{
	    	one(mockRouter).route(with(IQMatcher.iqMatcher(partialJoinResult)));
	    }});
		
		// Process the IQ
		fixture.process(join);
	    	
		// Fail this test because of the need to inform other subscribers 
		assertTrue(false);
	}
	
	private IQ createJoinRequest(String []nodes) {
	    IQ joinRequest = new IQ(IQ.Type.set);
	    
		joinRequest.setTo(MIX_CHANNEL_JID);
	    joinRequest.setFrom(TEST_USERS_JID);
	    
	    Element join = docFactory.createElement("join", "urn:xmpp:mix:0");
	    
	    for (int i = 0; i < nodes.length; i++) {
	    	Element subscribe = docFactory.createElement("subscribe");
	    	subscribe.addAttribute("node", nodes[i]);
	    	join.add(subscribe);
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
