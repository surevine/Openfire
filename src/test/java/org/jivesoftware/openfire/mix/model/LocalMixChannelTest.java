package org.jivesoftware.openfire.mix.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.HashSet;

import org.hamcrest.Matchers;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.MixPersistenceManager;
import org.jivesoftware.openfire.mix.MixService;
import org.jivesoftware.openfire.mix.exception.CannotJoinMixChannelException;
import org.jivesoftware.openfire.mix.exception.CannotLeaveMixChannelException;
import org.jivesoftware.openfire.testutil.ElementMatchers;
import org.jivesoftware.openfire.testutil.PacketMatchers;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;
import org.jmock.internal.State;
import org.jmock.lib.legacy.ClassImposteriser;
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
    
	private static final String []EXTENDED_NODE_SET = {"urn:xmpp:mix:nodes:messages", "urn:xmpp:mix:nodes:participants", "urn:xmpp:mix:nodes:subject", "urn:xmpp:mix:nodes:config"};
	
	Mockery context = new Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};
    
	final MixService mockMixService = context.mock(MixService.class); 
	
	final Packet mockPacket = context.mock(Packet.class);
	
	final PacketRouter mockRouter = context.mock(PacketRouter.class);
	
	final MixPersistenceManager mockPersistenceManager = context.mock(MixPersistenceManager.class);
	
	private LocalMixChannel fixture;
	
	States test = context.states("test").startsAs("setting up");
	State settingUp = test.is("setting up");
	State setUp = test.is("set up");
	
	public LocalMixChannelTest() throws MixPersistenceException {
        context.checking(new Expectations() {{
            allowing(mockMixService).getServiceDomain();
            will(returnValue(TEST_MIX_DOMAIN));
            
            allowing(mockPersistenceManager).save(with(any(LocalMixChannel.class)));
            allowing(mockPersistenceManager).save(with(any(MixChannelParticipant.class)));
            
            allowing(mockRouter).route(with(any(Packet.class)));
            when(test.isNot("set up"));
            allowing(mockRouter).route(with(any(Message.class)));
            when(test.isNot("set up"));
            allowing(mockRouter).route(with(any(IQ.class)));
            when(test.isNot("set up"));
            allowing(mockRouter).route(with(any(Presence.class)));
            when(test.isNot("set up"));
        }});
        
		fixture = new LocalMixChannel(mockMixService, TEST_MIX_CHANNEL_NAME, TEST_USER1_JID, mockRouter, mockPersistenceManager);
	}
	
	@Test
	public void thatFirstUserCanJoinChannelAndSubscribeToNodes() throws CannotJoinMixChannelException {	    
		
		// Expect that a single message is sent to the participant that has just signed up 
	    context.checking(new Expectations() {{
	    	one(mockRouter);
	    }});
		
		MixChannelParticipant mcp = fixture.addParticipant(TEST_USER1_JID, new HashSet<String>(Arrays.asList(EXTENDED_NODE_SET)));
		
		assertEquals(mcp.getRealJid(), TEST_USER1_JID);

	}
	
	@Test
	public void thatSecondUserJoiningTriggersTwoParticipantUpdates() throws CannotJoinMixChannelException {
		
		context.checking(new Expectations() {{
			// One for the first participant, and two for the second participant
	    	exactly(3).of(mockRouter).route(with(any(Message.class)));
	    }});
		
		fixture.addParticipant(TEST_USER1_JID, new HashSet<String>(Arrays.asList(EXTENDED_NODE_SET)));
		fixture.addParticipant(TEST_USER2_JID, new HashSet<String>(Arrays.asList(EXTENDED_NODE_SET)));
		
	}
	
	@Test
	public void testMessageToGroupIsReflectedToOtherParticipants() throws CannotJoinMixChannelException {
		settingUp.activate();
		
		// We have three participants
		final MixChannelParticipant sender = fixture.addParticipant(TEST_USER1_JID, new HashSet<String>(Arrays.asList(EXTENDED_NODE_SET)));
		fixture.addParticipant(TEST_USER2_JID, new HashSet<String>(Arrays.asList(EXTENDED_NODE_SET)));
		fixture.addParticipant(TEST_USER3_JID, new HashSet<String>(Arrays.asList(EXTENDED_NODE_SET)));
		
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
		
		setUp.activate();
		
		// We expect messages to the other two participants and one to the sender
		context.checking(new Expectations() {{
			// One for the first participant, and two for the second participant
	    	one(mockRouter).route(with(Matchers.<Message>allOf(
	    			Matchers.hasProperty("body", equal(testBody)),
	    			Matchers.hasProperty("to", equal(TEST_USER1_JID)),
	    			PacketMatchers.element(ElementMatchers.hasTextChild("submission-id", equal(testMessageID))),
	    			PacketMatchers.element(ElementMatchers.hasTextChild("jid", equal(sender.getJid().toBareJID())))
	    			)));
	    	one(mockRouter).route(with(Matchers.<Message>allOf(
	    			Matchers.hasProperty("body", equal(testBody)),
	    			Matchers.hasProperty("to", equal(TEST_USER2_JID)),
	    			PacketMatchers.element(ElementMatchers.hasTextChild("jid", equal(sender.getJid().toBareJID()))),
	    			PacketMatchers.element(ElementMatchers.hasNoChild("submission-id"))
	    		)));
	    	one(mockRouter).route(with(Matchers.<Message>allOf(
	    			Matchers.hasProperty("body", equal(testBody)),
	    			Matchers.hasProperty("to", equal(TEST_USER3_JID)),
	    			PacketMatchers.element(ElementMatchers.hasTextChild("jid", equal(sender.getJid().toBareJID()))),
	    			PacketMatchers.element(ElementMatchers.hasNoChild("submission-id"))
	    		)));
	    }});
		
		fixture.receiveMessage(mcMessage);
	}
	
	@Test
	public void testParticipantSuccessfullyLeavesChannel() throws CannotJoinMixChannelException, CannotLeaveMixChannelException, MixPersistenceException {
		context.checking(new Expectations() {{
			one(mockPersistenceManager).save(with(any(MixChannelParticipant.class)));
			allowing(mockRouter).route(with(any(Message.class)));
			one(mockRouter).route(with(any(IQ.class)));
			one(mockPersistenceManager).delete(with(any(MixChannelParticipant.class)));
	    }});
		
		fixture.addParticipant(TEST_USER1_JID, new HashSet<String>(Arrays.asList(EXTENDED_NODE_SET)));
		
		assertNotNull(fixture.getParticipantByJID(TEST_USER1_JID));
		
		fixture.removeParticipant(TEST_USER1_JID);
		
		assertNull(fixture.getParticipantByJID(TEST_USER1_JID));

	}
	
	@Test(expected=CannotLeaveMixChannelException.class)
	public void testExceptionThrownWhenLeaveNotChannelParticipant() throws CannotLeaveMixChannelException {
		fixture.removeParticipant(new JID("not_participant", TEST_SERVICE_DOMAIN, null));
	}
}
