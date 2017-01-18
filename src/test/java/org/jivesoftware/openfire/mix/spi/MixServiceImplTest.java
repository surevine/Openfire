package org.jivesoftware.openfire.mix.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.XMPPServerInfo;
import org.jivesoftware.openfire.XMPPServerListener;
import org.jivesoftware.openfire.disco.DiscoInfoProvider;
import org.jivesoftware.openfire.disco.DiscoItem;
import org.jivesoftware.openfire.disco.IQDiscoInfoHandler;
import org.jivesoftware.openfire.disco.IQDiscoItemsHandler;
import org.jivesoftware.openfire.disco.ServerItemsProvider;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.MixPersistenceManager;
import org.jivesoftware.openfire.mix.MixXmppServiceImpl;
import org.jivesoftware.openfire.mix.exception.MixChannelAlreadyExistsException;
import org.jivesoftware.openfire.mix.model.LocalMixChannel;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.util.JiveProperties;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

public class MixServiceImplTest {
	Mockery mockery = new Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};
	
    private static final String TEST_DOMAIN = "somedomain.example";
    
	private static final String TEST_SUBDOMAIN = "mixservice";
	
	private static final String TEST_DESCRIPTION = "Some kind of MIX service";
	
	private static final JID TEST_SENDER = new JID("name@server.com");
	
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
	
	private LocalMixChannel testChannelOne;
	
	private LocalMixChannel testChannelTwo;
	
	private MixXmppServiceImpl mockXmppService;
	
	@Before
	public void setUp() throws Exception {
		xmppServer = mockery.mock(XMPPServer.class);

		xmppServerInfo = mockery.mock(XMPPServerInfo.class);
		
		jiveProperties = mockery.mock(JiveProperties.class);
		
		mixPersistenceManager = mockery.mock(MixPersistenceManager.class);
		
		mockXmppService = mockery.mock(MixXmppServiceImpl.class);
		
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

		mixServiceImpl = new MixServiceImpl(xmppServer, jiveProperties, TEST_SUBDOMAIN, TEST_DESCRIPTION, mockXmppService, mixPersistenceManager);
		
		testChannelOne = new LocalMixChannel(mixServiceImpl, "channel1", null, mixPersistenceManager); 
		testChannelTwo = new LocalMixChannel(mixServiceImpl, "channel2", null, mixPersistenceManager);
	}

	@Test
	public void testGetIdentitiesForService() {		
		mockery.checking(new Expectations() {{
			allowing(jiveProperties).getBooleanProperty("xmpp.mix.enabled", true); will(returnValue(true));
		}});
		
		mixServiceImpl.initializeSettings();
		
		Iterator<Element> result = mixServiceImpl.getIdentities(null, null, TEST_SENDER);
		
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
		
		Iterator<Element> result = mixServiceImpl.getIdentities(null, null, TEST_SENDER);
		
		assertNull("Nothing is expected", result);
	}

	@Test
	public void testGetIdentitiesForChannel() throws MixPersistenceException {
		final List<? extends MixChannel> channels = Arrays.asList(testChannelOne);
		
		mockery.checking(new Expectations() {{
			allowing(mixPersistenceManager).loadChannels(mixServiceImpl); will(returnValue(channels));
		}});
		
		// Loads the channels
		mixServiceImpl.start();
		
		Iterator<Element> result = mixServiceImpl.getIdentities(testChannelOne.getName(), null, TEST_SENDER);
		
		Element el = result.next();
		
		assertEquals("Element is of type 'identity'", "identity", el.getName());
		assertEquals("Identity is of category 'conference'", "conference", el.attributeValue("category"));
		assertEquals("Identity has correct name", testChannelOne.getName(), el.attributeValue("name"));
		assertEquals("Identity is of type 'mix'", "mix", el.attributeValue("type"));
		
		assertFalse("A single identity is expected", result.hasNext());
	}
	
	@Test
	public void testGetFeaturesForService() {
		Iterator<String> result = mixServiceImpl.getFeatures(null, null, TEST_SENDER);
		
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
		
		boolean result = mixServiceImpl.hasInfo(null, null, TEST_SENDER);
		
		assertTrue("We always have info on the service", result);
	}

	@Test
	public void testHasInfoIfDisabled() {
		mockery.checking(new Expectations() {{
			allowing(jiveProperties).getBooleanProperty("xmpp.mix.enabled", true); will(returnValue(false));
		}});
		
		mixServiceImpl.initializeSettings();
		
		boolean result = mixServiceImpl.hasInfo(null, null, TEST_SENDER);
		
		assertFalse("Expect no info if the service is disabled", result);
	}

	@Test
	@Ignore
	public void testGetItems() throws MixPersistenceException {
		final List<? extends MixChannel> channels = Arrays.asList();
		
		mockery.checking(new Expectations() {{
			allowing(mixPersistenceManager).loadChannels(mixServiceImpl); will(returnValue(channels));
		}});
		
		// Loads the channels
		mixServiceImpl.start();
		
		Iterator<DiscoItem> result = mixServiceImpl.getItems(null, null, TEST_SENDER);
		
		DiscoItem el = result.next();		
		assertEquals("Element is of type 'identity'", new JID("channel1@" + TEST_SUBDOMAIN + "." + TEST_DOMAIN), el.getJID());
		
		el = result.next();		
		assertEquals("Element is of type 'identity'", new JID("channel2@" + TEST_SUBDOMAIN + "." + TEST_DOMAIN), el.getJID());
		
	}

	@Test
	public void testHasInfoForChannel() throws MixPersistenceException {
		final List<? extends MixChannel> channels = Arrays.asList(testChannelOne, testChannelTwo);
		
		mockery.checking(new Expectations() {{
			allowing(mixPersistenceManager).loadChannels(mixServiceImpl); will(returnValue(channels));
		}});
		
		// Loads the channels
		mixServiceImpl.start();
		
		boolean result = mixServiceImpl.hasInfo("channel1", null, TEST_SENDER);
		
		assertTrue("We have info on the channel", result);
	}
	
	@Test
	public void testNoInfoInfoForNonExistantChannel() throws MixPersistenceException {
		final List<? extends MixChannel> channels = Arrays.asList(testChannelOne, testChannelTwo);
		
		mockery.checking(new Expectations() {{
			allowing(mixPersistenceManager).loadChannels(mixServiceImpl); will(returnValue(channels));
		}});
		
		// Loads the channels
		mixServiceImpl.start();
		
		boolean result = mixServiceImpl.hasInfo("nonexistant", null, TEST_SENDER);
		
		assertFalse("We have no info on the channel", result);
	}
	
	@Test
	public void testGetFeaturesForChannel() throws MixPersistenceException {
		final List<? extends MixChannel> channels = Arrays.asList(testChannelOne, testChannelTwo);
		
		mockery.checking(new Expectations() {{
			allowing(mixPersistenceManager).loadChannels(mixServiceImpl); will(returnValue(channels));
		}});
		
		// Loads the channels
		mixServiceImpl.start();
		
		Iterator<String> result = mixServiceImpl.getFeatures("channel1", null, TEST_SENDER);
		
		String feature = result.next();
		
		assertEquals("Feature is 'urn:xmpp:mix:0'", "urn:xmpp:mix:0", feature);
		
		assertFalse("A single feature is expected", result.hasNext());
	}
	
	@Test
	public void testGetDiscoItems() throws MixPersistenceException {
		final List<? extends MixChannel> channels = Arrays.asList(testChannelOne, testChannelTwo);
		
		mockery.checking(new Expectations() {{
			allowing(mixPersistenceManager).loadChannels(mixServiceImpl); will(returnValue(channels));
		}});
		
		// Loads the channels
		mixServiceImpl.start();
		
		Iterator<DiscoItem> discoItems = mixServiceImpl.getItems(null, null, null);
		
		// Clunky as its an iterator
		int i = 0;
		while(discoItems.hasNext()) {
		    i++;
		    discoItems.next();
		}
		
		assertEquals(2, i);
	}
	
	@Test
	public void testGetDiscoItemsOnChannelReturnsExpectedNodes() throws MixPersistenceException {
		final List<? extends MixChannel> channels = Arrays.asList(testChannelOne);
		
		mockery.checking(new Expectations() {{
			allowing(mixPersistenceManager).loadChannels(mixServiceImpl); will(returnValue(channels));
		}});
		
		// Loads the channels
		mixServiceImpl.start();
		
		Iterator<DiscoItem> discoItems = mixServiceImpl.getItems(testChannelOne.getName(), null, null);
		
		List<String> expectedNodes = new ArrayList<>(Arrays.asList("urn:xmpp:mix:nodes:participants", "urn:xmpp:mix:nodes:messages"));
		
		while(discoItems.hasNext()) {
			DiscoItem item = discoItems.next();
			
			assertTrue(item.getNode() + " is expected", expectedNodes.contains(item.getNode()));
			assertEquals("JID is for the MIX service", testChannelOne.getJID(), item.getJID());
			
			expectedNodes.remove(item.getNode());
		}
		
		assertTrue("All nodes are accounted for", expectedNodes.isEmpty());
	}
	
	@Test
	public void testCreateChannel() throws Exception {
		
		final MixChannel newMixChannel = mockery.mock(MixChannel.class);
		
		mockery.checking(new Expectations() {{
			one(mixPersistenceManager).save(with(Matchers.<MixChannel>hasProperty("name", equal("coven"))));
			will(returnValue(newMixChannel));
		}});
		
		MixChannel result = mixServiceImpl.createChannel("coven");
		
		assertSame("The new mix channel is returned", newMixChannel, result);
		
	}
	
	@Test
	public void testCreateChannelThrowExceptionIfAlreadyExists() throws Exception {
		final List<? extends MixChannel> channels = Arrays.asList(testChannelOne);
		
		mockery.checking(new Expectations() {{
			allowing(mixPersistenceManager).loadChannels(mixServiceImpl); will(returnValue(channels));
		}});
		
		// Loads the channels
		mixServiceImpl.start();
		
		try {
			mixServiceImpl.createChannel(testChannelOne.getName());
			
			fail();
		} catch(MixChannelAlreadyExistsException e) {
			assertEquals("Exception has the correct name", testChannelOne.getName(), e.getNewChannelName());
		}
		
	}	
	@Test
	public void testProcessDoesNothingIfServiceDisabled() {
		mockery.checking(new Expectations() {{
			allowing(jiveProperties).getBooleanProperty("xmpp.mix.enabled", true); will(returnValue(false));
			allowing(mockXmppService);
		}});
		
		Message message = new Message();
		
		mixServiceImpl.processPacket(message);
	}
	
	static class IQMatcher extends TypeSafeMatcher<IQ> {

		private IQ expectation;
		
		public IQMatcher(IQ expectation) {
			this.expectation = expectation;
		}
		
		@Override
		public void describeTo(Description arg0) {
			// TODO Implement
		}

		@Override
		protected boolean matchesSafely(IQ result) {
			// First check the types
			if (expectation.getType().equals(result.getType())) {
				Element expectedElement = expectation.getChildElement();
				Element resultElement = result.getChildElement();
				
				// Then check the child XML.
				if (expectedElement.asXML().equals(resultElement.asXML())) {
					return true;					
				}
			}
			
			return false;

		}
		
		@Factory
		public static Matcher<IQ> iqMatcher(IQ expection) {
		    return new IQMatcher(expection);
		}

	}
}
