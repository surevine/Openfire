package org.jivesoftware.openfire.mix.handler.service;

import static org.junit.Assert.assertEquals;

import org.dom4j.Element;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.MixService;
import org.jivesoftware.openfire.mix.exception.MixChannelAlreadyExistsException;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.IQ.Type;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError.Condition;

public class MixServiceChannelCreatePacketHandlerTest {

    private static final String TEST_DOMAIN = "somedomain.example";
    
	private static final String TEST_SUBDOMAIN = "mixservice";
	
	private static final String TEST_DESCRIPTION = "Some kind of MIX service";
	
	private static final JID TEST_SENDER = new JID("name@server.com");

	private Mockery mockery = new Mockery();

	/**
	 * The class under test
	 */
	MixServiceChannelCreatePacketHandler handler;
	
	MixService mockMixService;
	
	@Before
	public void setUp() throws Exception {
		mockMixService = mockery.mock(MixService.class);
		
		handler = new MixServiceChannelCreatePacketHandler();
	}

	@Test
	public void testCreateChannel() throws Exception {
		// Create a create IQ
		final IQ createRequest = new IQ(IQ.Type.set);
		createRequest.setFrom(TEST_SENDER);
		createRequest.setTo(TEST_DOMAIN + "." + TEST_SUBDOMAIN);
		
		Element createElement = createRequest.setChildElement("create", "urn:xmpp:mix:0");
		createElement.addAttribute("channel", "coven");
		
		mockery.checking(new Expectations() {{
			one(mockMixService).createChannel(TEST_SENDER, "coven");
		}});
		
		IQ result = handler.processIQ(mockMixService, createRequest);
		
		assertEquals("IQ result is sent", Type.result, result.getType());
		assertEquals("Result is sent to the originator", createRequest.getFrom(), result.getTo());
		assertEquals("Result is from the service", createRequest.getTo(), result.getFrom());
		assertEquals("Result contains the child element", createRequest.getChildElement().asXML(), result.getChildElement().asXML());
	}

	@Test
	public void testCreateChannelIfAlreadyExists() throws Exception {
		// Create a create IQ
		final IQ createRequest = new IQ(IQ.Type.set);
		createRequest.setFrom(TEST_SENDER);
		createRequest.setTo(TEST_DOMAIN + "." + TEST_SUBDOMAIN);
		
		Element createElement = createRequest.setChildElement("create", "urn:xmpp:mix:0");
		createElement.addAttribute("channel", "coven");
		
		mockery.checking(new Expectations() {{
			one(mockMixService).createChannel(TEST_SENDER, "coven"); will(throwException(new MixChannelAlreadyExistsException("coven")));
		}});
		
		IQ result = handler.processIQ(mockMixService, createRequest);
		
		assertEquals("IQ error is sent", Type.error, result.getType());
		assertEquals("Result is sent to the originator", createRequest.getFrom(), result.getTo());
		assertEquals("Result is from the service", createRequest.getTo(), result.getFrom());
		assertEquals("Result contains the child element", createRequest.getChildElement().asXML(), result.getChildElement().asXML());
		assertEquals("Error is a conflict", Condition.conflict, result.getError().getCondition());
	}

}
