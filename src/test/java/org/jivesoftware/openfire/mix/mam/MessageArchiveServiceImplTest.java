package org.jivesoftware.openfire.mix.mam;

import static org.jivesoftware.openfire.mix.mam.repository.MamTestUtils.MIX_CHANNEL_JID;
import static org.jivesoftware.openfire.mix.mam.repository.MamTestUtils.QUERY_ELEM_NAME;
import static org.jivesoftware.openfire.mix.mam.repository.MamTestUtils.TEST_USERS_JID;
import static org.junit.Assert.assertTrue;

import java.util.*;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityPreference;
import org.jivesoftware.openfire.mix.mam.repository.MamTestUtils;
import org.jivesoftware.openfire.mix.mam.repository.MixChannelArchiveRepository;
import org.jivesoftware.openfire.mix.mam.repository.QueryFactory;
import org.jivesoftware.openfire.mix.mam.repository.ResultSetQuery;
import org.jivesoftware.openfire.mix.model.MessageBuilder;
import org.jivesoftware.openfire.mix.model.MixChannelMessage;
import org.jivesoftware.openfire.mix.model.MixChannelMessageImpl;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Ignore;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

public class MessageArchiveServiceImplTest {
	Mockery context = new Mockery() {{
		setImposteriser(ClassImposteriser.INSTANCE);
	}};

	private static final DocumentFactory docFactory = DocumentFactory.getInstance();

	private MixChannelArchiveRepository mockRepository = context.mock(MixChannelArchiveRepository.class);

	private PacketRouter mockRouter = context.mock(PacketRouter.class);

	private QueryFactory mockFactory = context.mock(QueryFactory.class);

	private XMPPServer mockXmppServer = context.mock(XMPPServer.class);
	
	private MessageArchiveService fixture = new MessageArchiveServiceImpl(mockXmppServer, mockRepository, mockRouter, mockFactory);

	private ResultSetQuery mockRSQuery = context.mock(ResultSetQuery.class);


	@Test
	public void testCorrectNumberOfMessagesSent() {
		ArchivedMixChannelMessage archivedMessage = new ArchivedMixChannelMessage();
		archivedMessage.setId("some-message-id");
		archivedMessage.setStanza("<message />");
		archivedMessage.setArchiveTimestamp(new Date());

		final IQ filterQuery = this.getBaseQueryIQ();
		context.checking(new Expectations() {{

			final List<ArchivedMixChannelMessage> results = new ArrayList<>();
			results.add(archivedMessage);

			one(mockXmppServer).getAccessControlDecisionFunction();
			will(returnValue(null));

			one(mockFactory).create(mockRepository, filterQuery);
			will(returnValue(mockRSQuery));
			
			one(mockRSQuery).execute();
			will(returnValue(results));

			one(mockRouter).route(with(any(Message.class)));
			never(mockRouter).route(with(any(IQ.class)));
		}});

		IQ result = fixture.query(filterQuery);
		assertTrue(result.getType().equals(IQ.Type.result));

		context.assertIsSatisfied();
	}
	

	private IQ getBaseQueryIQ() {
		IQ queryRequest = new IQ(IQ.Type.set);

		queryRequest.setTo(MIX_CHANNEL_JID);
		queryRequest.setFrom(TEST_USERS_JID);

		Element query = docFactory.createElement(QUERY_ELEM_NAME, MessageArchiveService.MAM_NAMESPACE);
		query.addAttribute("queryid", "f28");

		queryRequest.setChildElement(query);
		
		return queryRequest;
	}



}
