package org.jivesoftware.openfire.mix.spi;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.dom4j.Element;
import org.jivesoftware.openfire.XMPPServer;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xmpp.packet.JID;

public class MixServiceImplTest {
	Mockery mockery = new Mockery();
	
	private static final String TEST_SUBDOMAIN = "mixservice";
	
	private static final String TEST_DESCRIPTION = "Some kind of MIX service";
	
	/**
	 * The class under test
	 */
	private MixServiceImpl mixServiceImpl;
	
	private XMPPServer xmppServer;
	
	@Before
	public void setUp() throws Exception {
		xmppServer = mockery.mock(XMPPServer.class);
		
		mixServiceImpl = new MixServiceImpl(xmppServer, TEST_SUBDOMAIN, TEST_DESCRIPTION, false);
	}

	@Test
	public void testGetIdentitiesForRoot() {
		JID senderJid = new JID("name@server.com");
		
		Iterator<Element> result = mixServiceImpl.getIdentities(null, null, senderJid);
		
		Element el = result.next();
		
		assertEquals("Element is of type 'identity'", "identity", el.getName());
		assertEquals("Identity is of category 'conference'", "conference", el.attributeValue("category"));
		assertEquals("Identity has correct name", TEST_DESCRIPTION, el.attributeValue("name"));
		assertEquals("Identity is of type 'text'", "text", el.attributeValue("type"));
		
		assertFalse("A single identity is expected", result.hasNext());
	}

	@Test
	public void testGetFeatures() {
		fail("Not yet implemented");
	}

	@Test
	public void testHasInfo() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetItems() {
		fail("Not yet implemented");
	}

}
