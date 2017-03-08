package org.jivesoftware.openfire.mix.mam;

import static org.jivesoftware.openfire.mix.mam.repository.MamTestUtils.MIX_CHANNEL_JID;
import static org.jivesoftware.openfire.mix.mam.repository.MamTestUtils.QUERY_ELEM_NAME;
import static org.jivesoftware.openfire.mix.mam.repository.MamTestUtils.TEST_USERS_JID;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.mix.mam.repository.MamTestUtils;
import org.jivesoftware.openfire.mix.mam.repository.MixChannelArchiveRepository;
import org.jivesoftware.openfire.mix.mam.repository.QueryFactory;
import org.jivesoftware.openfire.mix.mam.repository.ResultSetQuery;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;

public class MessageArchiveServiceImplTest {

	private static final DocumentFactory docFactory = DocumentFactory.getInstance();

	private Mockery context = new Mockery();

	private MixChannelArchiveRepository mockRepository = context.mock(MixChannelArchiveRepository.class);

	private PacketRouter mockRouter = context.mock(PacketRouter.class);

	private QueryFactory mockFactory = context.mock(QueryFactory.class);
	
	private MessageArchiveService fixture = new MessageArchiveServiceImpl(mockRepository, mockRouter, mockFactory);

	private ResultSetQuery mockRSQuery = context.mock(ResultSetQuery.class);


	@Test
	public void testCorrectNumberOfMessagesSent() {

		final IQ filterQuery = this.getBaseQueryIQ();
		context.checking(new Expectations() {{

			final List<ArchivedMixChannelMessage> results = new ArrayList<>();
			
			results.add(MamTestUtils.getTestMessage());
			results.add(MamTestUtils.getTestMessage());

			one(mockFactory).create(mockRepository, filterQuery);
			will(returnValue(mockRSQuery));
			
			one(mockRSQuery).execute();
			will(returnValue(results));
			
			exactly(2).of(mockRouter).route(with(any(Message.class)));
			one(mockRouter).route(with(any(IQ.class)));
		}});

		fixture.query(filterQuery);

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
