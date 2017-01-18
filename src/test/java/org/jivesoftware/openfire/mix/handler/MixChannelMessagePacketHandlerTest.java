package org.jivesoftware.openfire.mix.handler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.hamcrest.Matchers;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.mix.handler.channel.MixChannelMessagePacketHandler;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelMessage;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.jivesoftware.openfire.testutil.PacketMatchers;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Message.Type;
import org.xmpp.packet.PacketError;
import org.xmpp.packet.Presence;

public class MixChannelMessagePacketHandlerTest {

	Mockery context = new Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};
    
    private static final JID TEST_CHANNEL_JID = new JID("coven", "mix.shakespeare.example", null);
    
	/**
	 * The class under test
	 */
	MixChannelMessagePacketHandler handler;
	
	MixChannel channel;
	
	PacketRouter router;
	
	@Before
	public void setUp() throws Exception {
		channel = context.mock(MixChannel.class);
		
		router = context.mock(PacketRouter.class);

		context.checking(new Expectations() {{
			allowing(channel).getJID(); will(returnValue(TEST_CHANNEL_JID));
		}});
		
		handler = new MixChannelMessagePacketHandler(router);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testProcessIQReturnsNull() {
		IQ result = handler.processIQ(channel, new IQ());
		
		assertNull("processIQ always returns null", result);
	}

	@Test
	public void testProcessPresence() {
		boolean result = handler.processPresence(channel, new Presence());
		
		assertFalse("processPresence always returns false", result);
	}

	@Test
	public void testProcessMessageReturnsFalseIfNotGroupchat() {
		Message message = getTestMessage();
		message.setType(Type.chat);
		
		boolean result = handler.processMessage(channel, message);
		
		assertFalse("Non-groupchat returns false", result);		
	}

	@Test
	public void testProcessMessageSendsNotAcceptableIfNotParticipant() {
		final Message message = getTestMessage();

		context.checking(new Expectations() {{
			{
				one(channel).getParticipantByJID(with(equal(message.getFrom().asBareJID())));
				will(returnValue(null));
			}
			
			one(router).route(with(PacketMatchers.<Message>hasErrorCondition(PacketError.Condition.not_acceptable)));
		}});
		
		boolean result = handler.processMessage(channel, message);
		
		assertTrue("Not participant returns true", result);
	}
	
	@Test
	public void testProcessMessage() {
		final Message message = getTestMessage();
		final MixChannelParticipant sender = context.mock(MixChannelParticipant.class);
		
		context.checking(new Expectations() {{
			{
				one(channel).getParticipantByJID(with(equal(message.getFrom().asBareJID())));
				will(returnValue(sender));
			}
			
			one(channel).receiveMessage(with(Matchers.<MixChannelMessage>allOf(
					Matchers.hasProperty("message", equal(message)),
					Matchers.hasProperty("sender", equal(sender))
				)));
		}});
		
		boolean result = handler.processMessage(channel, message);
		
		assertTrue("Success returns true", result);
	}

	private Message getTestMessage() {
		Message message = new Message();
		message.setType(Type.groupchat);
		message.setFrom(new JID("hag66@shakespeare.example/pda"));
		message.setTo(new JID("coven@mix.shakespeare.example"));
		message.setID("12345abcde");
		
		return message;
	}
}
