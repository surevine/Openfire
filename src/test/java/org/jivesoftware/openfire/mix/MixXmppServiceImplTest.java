package org.jivesoftware.openfire.mix;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.mix.handler.channel.MixChannelPacketHandler;
import org.jivesoftware.openfire.mix.handler.service.MixServicePacketHandler;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.testutil.PacketMatchers;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.IQ.Type;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketError.Condition;

public class MixXmppServiceImplTest {

	private static final JID TEST_SENDER = new JID("hag66@shakespeare.example");
	
	private static final String TEST_CHANNEL_NAME = "coven";
	private static final JID TEST_SERVICE_JID = new JID("mix.shakespeare.example");
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
		
		mockServiceHandler1 = mockery.mock(MixServicePacketHandler.class);
		mockServiceHandler2 = mockery.mock(MixServicePacketHandler.class);
		
		mockChannelHandler1 = mockery.mock(MixChannelPacketHandler.class);
		mockChannelHandler2 = mockery.mock(MixChannelPacketHandler.class);
		
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
					PacketMatchers.isType(IQ.Type.error),
					PacketMatchers.hasErrorCondition(Condition.item_not_found)
				)));
		}});
		
		xmppService.processReceivedPacket(mockMixService, request);
	}

	@Test
	public void testNullChannelReturnsMessageError() {
		Message request = new Message();
		request.setFrom(TEST_SENDER);
		request.setTo(TEST_CHANNEL_JID);
		
		mockery.checking(new Expectations() {{
			one(mockMixService).getChannel(TEST_CHANNEL_NAME); will(returnValue(null));
			
			one(mockRouter).route(with(Matchers.allOf(
					PacketMatchers.isType(Message.Type.error),
					PacketMatchers.hasErrorCondition(Condition.item_not_found)
				)));
		}});
		
		xmppService.processReceivedPacket(mockMixService, request);
	}
	
	@Test
	public void testAllHandlersAreRunForServiceIQ() throws Exception {
		final IQ request = new IQ(Type.set);
		request.setFrom(TEST_SENDER);
		request.setTo(TEST_SERVICE_JID);
		
		final IQ response = IQ.createResultIQ(request);
		
		mockery.checking(new Expectations() {{
			one(mockServiceHandler1).processIQ(mockMixService, request); will(returnValue(null));
			one(mockServiceHandler2).processIQ(mockMixService, request); will(returnValue(response));
			
			one(mockRouter).route(response);
		}});
		
		xmppService.processReceivedPacket(mockMixService, request);
	}
	
	@Test
	public void testHandlingStopsWhenDealtWithForServiceIQ() throws Exception {
		final IQ request = new IQ(Type.set);
		request.setFrom(TEST_SENDER);
		request.setTo(TEST_SERVICE_JID);
		
		final IQ response = IQ.createResultIQ(request);
		
		mockery.checking(new Expectations() {{
			one(mockServiceHandler1).processIQ(mockMixService, request); will(returnValue(response));
			never(mockServiceHandler2).processIQ(mockMixService, request);
			
			one(mockRouter).route(response);
		}});
		
		xmppService.processReceivedPacket(mockMixService, request);
	}
	
	@Test
	public void testAllHandlersAreRunForChannelMessage() throws Exception {
		final Message request = new Message();
		request.setFrom(TEST_SENDER);
		request.setTo(TEST_SERVICE_JID);
		
		mockery.checking(new Expectations() {{
			one(mockServiceHandler1).processMessage(mockMixService, request); will(returnValue(false));
			one(mockServiceHandler2).processMessage(mockMixService, request); will(returnValue(true));
		}});
		
		xmppService.processReceivedPacket(mockMixService, request);
	}
	
	@Test
	public void testHandlingStopsWhenDealtWithForServiceMessage() throws Exception {
		final Message request = new Message();
		request.setFrom(TEST_SENDER);
		request.setTo(TEST_SERVICE_JID);
		
		mockery.checking(new Expectations() {{
			one(mockServiceHandler1).processMessage(mockMixService, request); will(returnValue(true));
			never(mockServiceHandler2).processMessage(mockMixService, request);
		}});
		
		xmppService.processReceivedPacket(mockMixService, request);
	}
	
	@Test
	public void testInternalServerErrorForIQ() throws Exception {
		final IQ request = new IQ(Type.set);
		request.setFrom(TEST_SENDER);
		request.setTo(TEST_SERVICE_JID);
		
		mockery.checking(new Expectations() {{
			one(mockServiceHandler1).processIQ(mockMixService, request);  will(throwException(new Exception()));
			
			one(mockRouter).route(with(Matchers.allOf(
					PacketMatchers.isType(IQ.Type.error),
					PacketMatchers.hasErrorCondition(Condition.internal_server_error)
				)));
		}});
		
		xmppService.processReceivedPacket(mockMixService, request);
	}
	
	@Test
	public void testInternalServerErrorForMessage() throws Exception {
		final Message request = new Message();
		request.setFrom(TEST_SENDER);
		request.setTo(TEST_SERVICE_JID);
		
		mockery.checking(new Expectations() {{
			one(mockServiceHandler1).processMessage(mockMixService, request);  will(throwException(new Exception()));
			
			one(mockRouter).route(with(Matchers.allOf(
					PacketMatchers.isType(Message.Type.error),
					PacketMatchers.hasErrorCondition(Condition.internal_server_error)
				)));
		}});
		
		xmppService.processReceivedPacket(mockMixService, request);
	}
	

}
