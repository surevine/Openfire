package org.jivesoftware.openfire.mix.spi;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.dom4j.Element;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.XMPPServerInfo;
import org.jivesoftware.util.JiveProperties;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.internal.ExpectationBuilder;
import org.jmock.internal.ExpectationCollector;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xmpp.packet.JID;

public class MixServiceImplTest {
	Mockery mockery = new Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};
	
    private static final String TEST_DOMAIN = "somedomain.example";
    
	private static final String TEST_SUBDOMAIN = "mixservice";
	
	private static final String TEST_DESCRIPTION = "Some kind of MIX service";
	
	/**
	 * The class under test
	 */
	private MixServiceImpl mixServiceImpl;
	
	private XMPPServer xmppServer;
	
	private XMPPServerInfo xmppServerInfo;
	
	private JiveProperties jiveProperties;
	
	@Before
	public void setUp() throws Exception {
		xmppServer = mockery.mock(XMPPServer.class);

		xmppServerInfo = mockery.mock(XMPPServerInfo.class);
		
		jiveProperties = mockery.mock(JiveProperties.class);
		
		mockery.checking(new Expectations() {{
			allowing(xmppServerInfo).getXMPPDomain(); will(returnValue(TEST_DOMAIN));
			allowing(xmppServer).getServerInfo(); will(returnValue(xmppServerInfo));
		}});

		mixServiceImpl = new MixServiceImpl(xmppServer, jiveProperties, TEST_SUBDOMAIN, TEST_DESCRIPTION);
	}

	@Test
	public void testGetIdentitiesForService() {		
		mockery.checking(new Expectations() {{
			allowing(jiveProperties).getBooleanProperty("xmpp.mix.enabled", true); will(returnValue(true));
		}});
		
		mixServiceImpl.initializeSettings();
		
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
	public void testGetIdentitiesForServiceDisabled() {		
		mockery.checking(new Expectations() {{
			allowing(jiveProperties).getBooleanProperty("xmpp.mix.enabled", true); will(returnValue(false));
		}});
		
		mixServiceImpl.initializeSettings();
		
		JID senderJid = new JID("name@server.com");
		
		Iterator<Element> result = mixServiceImpl.getIdentities(null, null, senderJid);
		
		assertFalse("Nothing is expected", result.hasNext());
	}

	@Test
	public void testGetFeaturesForService() {
		JID senderJid = new JID("name@server.com");
		
		Iterator<String> result = mixServiceImpl.getFeatures(null, null, senderJid);
		
		String feature = result.next();
		
		assertEquals("Feature is 'urn:xmpp:mix:0'", "urn:xmpp:mix:0", feature);
		
		assertFalse("A single feature is expected", result.hasNext());
	}

	@Test
	public void testHasInfo() {
		mockery.checking(new Expectations() {{
			allowing(jiveProperties).getBooleanProperty("xmpp.mix.enabled", true); will(returnValue(true));
		}});
		
		mixServiceImpl.initializeSettings();
		
		JID senderJid = new JID("name@server.com");
		
		boolean result = mixServiceImpl.hasInfo(null, null, senderJid);
		
		assertTrue("We always have info on the service", result);
	}

	@Test
	public void testHasInfoIfDisabled() {
		mockery.checking(new Expectations() {{
			allowing(jiveProperties).getBooleanProperty("xmpp.mix.enabled", true); will(returnValue(false));
		}});
		
		mixServiceImpl.initializeSettings();
		
		JID senderJid = new JID("name@server.com");
		
		boolean result = mixServiceImpl.hasInfo(null, null, senderJid);
		
		assertFalse("Expect no info if the service is disabled", result);
	}

	@Test
	public void testGetItems() {
		fail("Not yet implemented");
	}

}
