package org.jivesoftware.openfire.mix.handler.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.jivesoftware.openfire.mix.MixManager;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.TestConstants;
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

public class MixChannelUpdateSubscriptionPacketHandlerTest {

	// Not all of these are currently supported.
	private static final String[] SUPPORTED_NODE_SET = { "urn:xmpp:mix:nodes:messages", "urn:xmpp:mix:nodes:participants" };

	private static final DocumentFactory docFactory = DocumentFactory.getInstance();

	private Mockery context = new Mockery();

	private MixChannel mockMixChannel;

	private MixChannelUpdateSubscriptionPacketHandler fixture = new MixChannelUpdateSubscriptionPacketHandler();

	public MixChannelUpdateSubscriptionPacketHandlerTest() {
		mockMixChannel = context.mock(MixChannel.class);

		context.checking(new Expectations() {{
				allowing(mockMixChannel).getNodesAsStrings();
				will(returnValue(new HashSet<String>(Arrays.asList(SUPPORTED_NODE_SET))));
		}});

	}

	@Test
	public void testSuccessfulUpdate() throws Exception {
		
		String [] updatedSubscriptions = {"urn:xmpp:mix:nodes:participants"};
		
		final Set<String> requestedSubscriptions = new HashSet<String>(Arrays.asList(updatedSubscriptions));
		IQ join = createUpdateSubscriptionRequest(updatedSubscriptions);

		final MixChannelParticipant mcp = new LocalMixChannelParticipant(null, TestConstants.TEST_USERS_JID, mockMixChannel,
				requestedSubscriptions, ChannelJidVisibilityPreference.NO_PREFERENCE,null);
		
		context.checking(new Expectations() {{
				allowing(mockMixChannel).getJID();
				allowing(mockMixChannel).getParticipantByRealJID(TestConstants.TEST_USERS_JID);
				will(returnValue(mcp));
				one(mockMixChannel).updateSubscriptions(with(equal(TestConstants.TEST_USERS_JID)), with(equal(requestedSubscriptions)));
		}});

		IQ response = fixture.processIQ(new MixRequestContextImpl(mcp, null, mockMixChannel), mockMixChannel, join);

		assertEquals(updatedSubscriptions.length,
				response.getChildElement().elements().size());
		
	}

	@Test
	public void processPresence() throws Exception {
		assertFalse(fixture.processPresence(new MixRequestContextImpl(TestConstants.TEST_USERS_JID, null, mockMixChannel), mockMixChannel, null));
	}

	@Test
	public void processMessage() throws Exception {
		assertFalse(fixture.processMessage(new MixRequestContextImpl(TestConstants.TEST_USERS_JID, null, mockMixChannel), mockMixChannel, null));
	}

	private IQ createUpdateSubscriptionRequest(String[] nodes) {
		IQ updateSubscriptionRequest = new IQ(IQ.Type.set);

		updateSubscriptionRequest.setTo(TestConstants.MIX_CHANNEL_JID);
		updateSubscriptionRequest.setFrom(TestConstants.TEST_USERS_JID);

		Element join = docFactory.createElement("update-subscription", MixManager.MIX_NAMESPACE);

		for (int i = 0; i < nodes.length; i++) {
			Element sub = join.addElement("subscribe");
			sub.addAttribute("node", nodes[i]);
		}

		updateSubscriptionRequest.setChildElement(join);

		return updateSubscriptionRequest;
	}

}
