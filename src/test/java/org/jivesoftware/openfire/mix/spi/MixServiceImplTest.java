package org.jivesoftware.openfire.mix.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;
import org.jivesoftware.openfire.PacketDeliverer;
import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.XMPPServerInfo;
import org.jivesoftware.openfire.XMPPServerListener;
import org.jivesoftware.openfire.disco.DiscoInfoProvider;
import org.jivesoftware.openfire.disco.DiscoItem;
import org.jivesoftware.openfire.disco.IQDiscoInfoHandler;
import org.jivesoftware.openfire.disco.IQDiscoItemsHandler;
import org.jivesoftware.openfire.disco.ServerItemsProvider;
import org.jivesoftware.openfire.mix.MixChannel;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.MixPersistenceManager;
import org.jivesoftware.util.JiveProperties;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
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
	
	private MixPersistenceManager mixPersistenceManager;
	
	private IQDiscoItemsHandler iqDiscoItemsHandler;
	
	private IQDiscoInfoHandler iqDiscoInfoHandler;
	
	@Before
	public void setUp() throws Exception {
		xmppServer = mockery.mock(XMPPServer.class);

		xmppServerInfo = mockery.mock(XMPPServerInfo.class);
		
		jiveProperties = mockery.mock(JiveProperties.class);
		
		mixPersistenceManager = mockery.mock(MixPersistenceManager.class);
		
		iqDiscoItemsHandler = mockery.mock(IQDiscoItemsHandler.class);
		iqDiscoInfoHandler = mockery.mock(IQDiscoInfoHandler.class);
		
		mockery.checking(new Expectations() {{
			allowing(xmppServerInfo).getXMPPDomain(); will(returnValue(TEST_DOMAIN));
			allowing(xmppServer).getServerInfo(); will(returnValue(xmppServerInfo));
			allowing(xmppServer).addServerListener(with(any(XMPPServerListener.class)));
			allowing(xmppServer).getIQDiscoItemsHandler(); will(returnValue(iqDiscoItemsHandler));
			allowing(xmppServer).getIQDiscoInfoHandler(); will(returnValue(iqDiscoInfoHandler));
//			allowing(iqDiscoInfoHandler).addServerFeature(with(any(String.class)));
			allowing(iqDiscoItemsHandler).addServerItemsProvider(with(any(ServerItemsProvider.class)));
			allowing(iqDiscoInfoHandler).setServerNodeInfoProvider(with(any(String.class)), with(any(DiscoInfoProvider.class)));
		}});

		mixServiceImpl = new MixServiceImpl(xmppServer, jiveProperties, mixPersistenceManager, TEST_SUBDOMAIN, TEST_DESCRIPTION);
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
		
		assertNull("Nothing is expected", result);
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
	public void testGetItems() throws MixPersistenceException {
		final List<? extends MixChannel> channels = Arrays.asList(new LocalMixChannel(mixServiceImpl, "channel1"), new LocalMixChannel(mixServiceImpl, "channel2"));
		
		mockery.checking(new Expectations() {{
			allowing(mixPersistenceManager).loadChannels(mixServiceImpl); will(returnValue(channels));
		}});
		
		// Loads the channels
		mixServiceImpl.start();
		
		Iterator<DiscoItem> result = mixServiceImpl.getItems(null, null, null);
		
		DiscoItem el = result.next();		
		assertEquals("Element is of type 'identity'", new JID("channel1@" + TEST_SUBDOMAIN + "." + TEST_DOMAIN), el.getJID());
		
		el = result.next();		
		assertEquals("Element is of type 'identity'", new JID("channel2@" + TEST_SUBDOMAIN + "." + TEST_DOMAIN), el.getJID());
		
	}

}
