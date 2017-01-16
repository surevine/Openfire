package org.jivesoftware.openfire.mix.model;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;

import org.dom4j.DocumentFactory;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.mix.MixService;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Message.Type;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;

public class LocalMixChannelTest {
	
	private static final String TEST_USER = "hag66";

	private final static String TEST_MIX_CHANNEL_NAME = "coven";
	
	private final static String TEST_SERVICE_DOMAIN = "shakespeare.example";
	
	private final static String TEST_MIX_DOMAIN = "mix." + TEST_SERVICE_DOMAIN;
	
	private final static JID TEXT_MIX_SERVICE_JID = new JID(TEST_MIX_CHANNEL_NAME, TEST_MIX_DOMAIN, null);

	private static final JID TEST_USER1_JID = new JID(TEST_USER, TEST_SERVICE_DOMAIN, null);
    private static final JID TEST_USER2_JID = new JID(TEST_USER + 6, TEST_SERVICE_DOMAIN, null);
    private static final JID TEST_USER3_JID = new JID(TEST_USER + 66, TEST_SERVICE_DOMAIN, null);
    
	private static final String []PARTIAL_NODE_SET = {"urn:xmpp:mix:nodes:messages", "urn:xmpp:mix:nodes:participants", "urn:xmpp:mix:nodes:subject", "urn:xmpp:mix:nodes:config"};
	
	private static final DocumentFactory docFactory = DocumentFactory.getInstance();

	
	Mockery context = new Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};
    
	final MixService mockMixService = context.mock(MixService.class); 
	
	final Packet mockPacket = context.mock(Packet.class);
	
	final PacketRouter mockRouter = context.mock(PacketRouter.class);
	
	private LocalMixChannel fixture = new LocalMixChannel(mockMixService, TEST_MIX_CHANNEL_NAME, mockRouter);
	
	States test = context.states("test").startsAs("setting up");
	
	@Before
	public void setUp() {
        context.checking(new Expectations() {{
            allowing(mockMixService).getServiceDomain();
            will(returnValue(TEST_MIX_DOMAIN));
            when(test.isNot("set up"));
            allowing(mockRouter).route(with(any(Packet.class)));
            when(test.isNot("set up"));
            allowing(mockRouter).route(with(any(Message.class)));
            when(test.isNot("set up"));
            allowing(mockRouter).route(with(any(IQ.class)));
            when(test.isNot("set up"));
            allowing(mockRouter).route(with(any(Presence.class)));
            when(test.isNot("set up"));
        }});
        
        test.is("set up");
	}
	
	@Test
	public void thatFirstUserCanJoinChannelAndSubscribeToNodes() {	    
		
		// Expect that a single message is sent to the participant that has just signed up 
	    context.checking(new Expectations() {{
	    	one(mockRouter);
	    }});
		
		MixChannelParticipant mcp = fixture.addParticipant(TEST_USER1_JID, new HashSet<String>(Arrays.asList(PARTIAL_NODE_SET)));
		
		assertEquals(mcp.getRealJid(), TEST_USER1_JID);

	}
	
	@Test
	public void thatSecondUserJoiningTriggersTwoParticipantUpdates() {
		
		context.checking(new Expectations() {{
			// One for the first participant, and two for the second participant
	    	exactly(3).of(mockRouter).route(with(any(Message.class)));
	    }});
		
		fixture.addParticipant(TEST_USER1_JID, new HashSet<String>(Arrays.asList(PARTIAL_NODE_SET)));
		fixture.addParticipant(TEST_USER2_JID, new HashSet<String>(Arrays.asList(PARTIAL_NODE_SET)));
		
	}
	
	@Test
	public void testMessageToGroupIsReflectedToOtherParticipants() {
		test.is("setting up");
		
		// We have three participants
		final MixChannelParticipant sender = fixture.addParticipant(TEST_USER1_JID, new HashSet<String>(Arrays.asList(PARTIAL_NODE_SET)));
		fixture.addParticipant(TEST_USER2_JID, new HashSet<String>(Arrays.asList(PARTIAL_NODE_SET)));
		fixture.addParticipant(TEST_USER3_JID, new HashSet<String>(Arrays.asList(PARTIAL_NODE_SET)));
		
		// Particiant 1 sends a message to the channel
		final String testMessageID = "1234ABCD";
		final String testBody = "Harpier cries: 'tis time, 'tis time.";
		
		Message message = new Message();
		message.setFrom(sender.getJid());
		message.setTo(TEXT_MIX_SERVICE_JID);
		message.setID(testMessageID);
		message.setType(Type.groupchat);
		message.setBody(testBody);
		
		MixChannelMessage mcMessage = new MixChannelMessageImpl(message, sender);
		
		test.is("set up");
		
		// We expect messages to the other two participants and one to the sender
		context.checking(new Expectations() {{
			// One for the first participant, and two for the second participant
	    	one(mockRouter).route(with(Matchers.<Message>allOf(
	    			Matchers.hasProperty("body", equal(testBody)),
	    			Matchers.hasProperty("to", equal(TEST_USER1_JID)),
	    			PacketMatchers.hasTextChild("submission-id", equal(testMessageID)),
	    			PacketMatchers.hasTextChild("jid", equal(sender.getJid().toBareJID()))
	    			)));
	    	one(mockRouter).route(with(Matchers.<Message>allOf(
	    			Matchers.hasProperty("body", equal(testBody)),
	    			Matchers.hasProperty("to", equal(TEST_USER2_JID)),
	    			PacketMatchers.hasTextChild("jid", equal(sender.getJid().toBareJID())),
	    			PacketMatchers.hasNoChild("submission-id")
	    		)));
	    	one(mockRouter).route(with(Matchers.<Message>allOf(
	    			Matchers.hasProperty("body", equal(testBody)),
	    			Matchers.hasProperty("to", equal(TEST_USER3_JID)),
	    			PacketMatchers.hasTextChild("jid", equal(sender.getJid().toBareJID())),
	    			PacketMatchers.hasNoChild("submission-id")
	    		)));
	    }});
		
		fixture.receiveMessage(mcMessage);
	}
	
	static class PacketMatchers {
		public static <K extends Packet> Matcher<K> hasTextChild(final String childName, final Matcher<String> textValueMatcher) {
			return new TypeSafeMatcher<K>() {

				@Override
				public void describeTo(Description arg0) {
					arg0.appendText("has child " + childName + " with text value " + textValueMatcher);
				}

				@Override
				protected boolean matchesSafely(K arg0) {
					return (arg0.getElement().element(childName) != null)
							&& (textValueMatcher.matches(arg0.getElement().element(childName).getText()));
				}
				
			};
		}
		
		public static <K extends Packet> Matcher<K> hasNoChild(final String childName) {
			return new TypeSafeMatcher<K>() {

				@Override
				public void describeTo(Description arg0) {
					arg0.appendText("doesn't have child " + childName);
				}

				@Override
				protected boolean matchesSafely(K arg0) {
					return (arg0.getElement().element(childName) == null);
				}
			};
		}
	}
}