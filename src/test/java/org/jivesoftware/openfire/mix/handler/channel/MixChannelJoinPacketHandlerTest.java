package org.jivesoftware.openfire.mix.handler.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityPreference;
import org.jivesoftware.openfire.mix.exception.CannotJoinMixChannelException;
import org.jivesoftware.openfire.mix.handler.MixRequestContextImpl;
import org.jivesoftware.openfire.mix.model.LocalMixChannelParticipant;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

public class MixChannelJoinPacketHandlerTest {

	private static final String TEST_USER = "hag66";

	private final static String TEST_MIX_CHANNEL_NAME = "coven";

	private final static String TEST_SERVICE_DOMAIN = "shakespeare.example";

	private final static String TEST_MIX_DOMAIN = "mix." + TEST_SERVICE_DOMAIN;

	private static final JID TEST_USERS_JID = new JID(TEST_USER, TEST_SERVICE_DOMAIN, null);
	private static final JID MIX_CHANNEL_JID = new JID(TEST_MIX_CHANNEL_NAME, TEST_MIX_DOMAIN, null);

	// Not all of these are currently supported.
	private static final String[] SUPPORTED_NODE_SET = { "urn:xmpp:mix:nodes:messages", "urn:xmpp:mix:nodes:participants" };
	private static final String[] EXTENDED_NODE_SET = { "urn:xmpp:mix:nodes:messages", "urn:xmpp:mix:nodes:participants", "urn:xmpp:mix:nodes:subject", "urn:xmpp:mix:nodes:config" };

	private static final DocumentFactory docFactory = DocumentFactory.getInstance();

	private Mockery context = new Mockery();

	private MixChannel mockMixChannel;

	private MixChannelJoinPacketHandler fixture = new MixChannelJoinPacketHandler();

	public MixChannelJoinPacketHandlerTest() {
		mockMixChannel = context.mock(MixChannel.class);

		context.checking(new Expectations() {{
				allowing(mockMixChannel).getNodesAsStrings();
				will(returnValue(new HashSet<String>(Arrays.asList(SUPPORTED_NODE_SET))));
		}});

	}

	@Test
	public void testSuccessfulJoinRequest() throws MixPersistenceException, CannotJoinMixChannelException {
		
		final Set<String> requestedSubscriptions = new HashSet<String>(Arrays.asList(EXTENDED_NODE_SET));
		IQ join = createJoinRequest(EXTENDED_NODE_SET);
		final MixChannelParticipant mcp = new LocalMixChannelParticipant(TEST_USERS_JID, TEST_USERS_JID, mockMixChannel,
				requestedSubscriptions, ChannelJidVisibilityPreference.NO_PREFERENCE,null);
		
		context.checking(new Expectations() {{
				allowing(mockMixChannel).getJID();

				one(mockMixChannel).addParticipant(with(equal(TEST_USERS_JID)), with(equal(requestedSubscriptions)), with(equal(ChannelJidVisibilityPreference.NO_PREFERENCE)));
				will(returnValue(mcp));
		}});

		IQ response = fixture.processIQ(new MixRequestContextImpl(mcp, null, mockMixChannel), mockMixChannel, join);

		assertEquals(MixChannelJoinPacketHandlerTest.SUPPORTED_NODE_SET.length,
				response.getChildElement().elements().size());

	}

	@Test
	public void processPresence() {
		assertFalse(fixture.processPresence(new MixRequestContextImpl(TEST_USERS_JID, null, mockMixChannel), mockMixChannel, null));
	}

	@Test
	public void processMessage() {
		assertFalse(fixture.processMessage(new MixRequestContextImpl(TEST_USERS_JID, null, mockMixChannel), mockMixChannel, null));
	}

	private IQ createJoinRequest(String[] nodes) {
		IQ joinRequest = new IQ(IQ.Type.set);

		joinRequest.setTo(MIX_CHANNEL_JID);
		joinRequest.setFrom(TEST_USERS_JID);

		Element join = docFactory.createElement("join", "urn:xmpp:mix:0");

		for (int i = 0; i < nodes.length; i++) {
			Element sub = join.addElement("subscribe");
			sub.addAttribute("node", nodes[i]);
		}

		joinRequest.setChildElement(join);

		return joinRequest;
	}

}