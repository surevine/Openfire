package org.jivesoftware.openfire.mix.handler.service;

import static org.junit.Assert.*;

import org.dom4j.Element;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.mix.MixService;
import org.jivesoftware.openfire.mix.TestConstants;
import org.jivesoftware.openfire.mix.exception.CannotDestroyMixChannelException;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.IQ.Type;

public class DestroyMixChannelPacketHandlerTest {

	/**
	 * The class under test
	 */
	private DestroyMixChannelPacketHandler fixture;
	
	private MixService mockMixService;
	
	private Mockery mockery = new Mockery();
	
	@Before
	public void setUp() throws Exception {
		mockMixService = mockery.mock(MixService.class);
		
		fixture = new DestroyMixChannelPacketHandler();
	}
	
	@Test
	public void testSuccessfulDeletionOfChannel() throws Exception {
		// Create a create IQ
		final IQ destroyRequest = DestroyMixChannelPacketHandlerTest.createDestroyRequest();
		
		mockery.checking(new Expectations() {{
			one(mockMixService).destroyChannel(TestConstants.TEST_USERS_JID, TestConstants.TEST_MIX_CHANNEL_NAME);
		}});
		
		IQ result = fixture.processIQ(mockMixService, destroyRequest);
		
		assertEquals("IQ result is sent", Type.result, result.getType());
		assertEquals("Result is sent to the originator", destroyRequest.getFrom(), result.getTo());
		assertEquals("Result is from the service", destroyRequest.getTo(), result.getFrom());
		assertEquals("Result contains the child element", destroyRequest.getChildElement().asXML(), result.getChildElement().asXML());
	}
	
	@Test
	public void testUnauthorisedDeletionOfChannel() throws Exception {

		final IQ destroyRequest = DestroyMixChannelPacketHandlerTest.createDestroyRequest();
		
		mockery.checking(new Expectations() {{
			one(mockMixService).destroyChannel(TestConstants.TEST_USERS_JID, TestConstants.TEST_MIX_CHANNEL_NAME);
			will(throwException(new UnauthorizedException()));
		}});
		
		IQ result = fixture.processIQ(mockMixService, destroyRequest);
		
		assertEquals("IQ result is error", Type.error, result.getType());
		
	}

	@Test
	public void testDeletionOfNonExistentChannel() {
		
	}
	
	
	private static IQ createDestroyRequest() {
		IQ destroyRequest = new IQ(IQ.Type.set);
		destroyRequest.setFrom(TestConstants.TEST_USERS_JID);
		destroyRequest.setTo(TestConstants.MIX_CHANNEL_JID);
		
		Element createElement = destroyRequest.setChildElement("destroy", "urn:xmpp:mix:0");
		createElement.addAttribute("channel", TestConstants.TEST_MIX_CHANNEL_NAME);
		
		return destroyRequest;
	}

}
