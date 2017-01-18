package org.jivesoftware.openfire.mix;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.mix.handler.channel.MixChannelPacketHandler;
import org.jivesoftware.openfire.mix.handler.service.MixServicePacketHandler;
import org.jivesoftware.openfire.testutil.ElementMatchers;
import org.jivesoftware.openfire.testutil.PacketMatchers;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError.Condition;
import org.xmpp.packet.IQ.Type;

public class MixXmppServiceImplTest {

	private static final JID TEST_SENDER = new JID("hag66@shakespeare.example");
	
	private static final String TEST_CHANNEL_NAME = "coven";
	private static final JID TEST_CHANNEL_JID = new JID("coven@mix.shakespeare.example");
	
	Mockery mockery;
	
	PacketRouter mockRouter;
	
	MixServicePacketHandler mockServiceHandler1;
	MixServicePacketHandler mockServiceHandler2;
	
	MixChannelPacketHandler mockChannelHandler1;
	MixChannelPacketHandler mockChannelHandler2;
	
	List<MixServicePacketHandler> mockServiceHandlers;
	
	List<MixChannelPacketHandler> mockChannelHandlers;

	MixService mockMixService;
	
	/**
	 * The class under test
	 */
	MixXmppServiceImpl xmppService;
	
	@Before
	public void setUp() throws Exception {
		mockery = new Mockery();
		
		mockRouter = mockery.mock(PacketRouter.class);
		
		mockServiceHandlers = Arrays.asList(mockServiceHandler1, mockServiceHandler2);
		mockChannelHandlers = Arrays.asList(mockChannelHandler1, mockChannelHandler2);
		
		xmppService = new MixXmppServiceImpl(mockRouter, mockServiceHandlers, mockChannelHandlers);
		
		mockMixService = mockery.mock(MixService.class);
		
		mockery.checking(new Expectations() {{
			allowing(mockMixService).isServiceEnabled(); will(returnValue(true));
		}});
	}

	@Test
	public void testReceivePacketIfServiceDisabled() {
		mockery.checking(new Expectations() {{
			one(mockMixService).isServiceEnabled(); will(returnValue(false));
		}});
		
		xmppService.processReceivedPacket(mockMixService, new IQ());
	}

	@Test
	public void testNullChannelReturnsIQError() {
		IQ request = new IQ(Type.set);
		request.setFrom(TEST_SENDER);
		request.setTo(TEST_CHANNEL_JID);
		
		mockery.checking(new Expectations() {{
			one(mockMixService).getChannel(TEST_CHANNEL_NAME); will(returnValue(null));
			
			one(mockRouter).route(with(Matchers.allOf(
					PacketMatchers.isType(Type.error),
					PacketMatchers.hasErrorCondition(Condition.item_not_found)
				)));
		}});
		
		xmppService.processReceivedPacket(mockMixService, request);
	}
}
