package org.jivesoftware.openfire.mix.mam.repository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.bouncycastle.openssl.MiscPEMGenerator;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.mam.ArchivedMixChannelMessage;
import org.jivesoftware.openfire.mix.mam.repository.JpaMixChannelArchiveRepositoryImpl;
import org.jivesoftware.openfire.mix.model.MessageBuilder;
import org.jivesoftware.openfire.mix.model.MixChannelMessage;
import org.jivesoftware.openfire.mix.model.MixChannelMessageImpl;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Ignore;
import org.junit.Test;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Message.Type;
import org.hibernate.jpa.HibernatePersistenceProvider;

import static org.junit.Assert.*;

public class JpaMixChannelArchiveRepositoryImplTest {

    private Mockery context = new Mockery();

	private JpaMixChannelArchiveRepositoryImpl fixture = new JpaMixChannelArchiveRepositoryImpl("test-mam", new HashMap<String, String>());
	private final Message testMsg = new Message();
	private MixChannelMessage mcm;
	private MixChannelParticipant mockMCP = context.mock(MixChannelParticipant.class);

    private static final String TEST_CHANNEL_NAME = "coven";
	private final JID targetChannelJID = new JID(TEST_CHANNEL_NAME, "shakespeare.example", "");
	private final JID fromJID = new JID("hag66", "shakespeare.example", "");

	public JpaMixChannelArchiveRepositoryImplTest() {
		new HibernatePersistenceProvider();
		testMsg.setBody("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi ligula erat, ullamcorper at ullamcorper e");
		testMsg.setType(Type.chat);
		testMsg.setSubject("Lorem ipsum");
		testMsg.setTo(targetChannelJID);
		testMsg.setFrom(fromJID);

        context.checking(new Expectations() {{
            allowing(mockMCP).getJid();
            will(returnValue(fromJID));

            allowing(mockMCP).getNick();
            will(returnValue("test-nick"));
        }});

        mcm = new MixChannelMessageImpl(testMsg, TEST_CHANNEL_NAME, mockMCP);

	}

	@Test
	public void testPersisting() throws MixPersistenceException {

		fixture.archive(mcm);

		// Query
		List<ArchivedMixChannelMessage> results = fixture.findMessagesByChannel(TEST_CHANNEL_NAME);

		assertEquals(1, results.size());
		assertNotNull(results.get(0).getId());

	}

	/**
	 * Archive two messages, checking that there is only 1 if we query after the
	 * firsts ID.
	 */
	@Test
	public void thatAfterUUIDQueryWorks() throws MixPersistenceException {

		String id = fixture.archive(mcm);

		fixture.archive(mcm);

		assertEquals(1, fixture.findMessagesByChannelAfter(TEST_CHANNEL_NAME, id).size());

	}

	@Test
	public void thatLimitingResultsWorks() throws MixPersistenceException {

		int limit = 50;

		for (int i = 0; i < limit * 2; i++) {
			fixture.archive(mcm);
		}

		assertEquals(limit, fixture.findLimitedMessagesByChannel(TEST_CHANNEL_NAME, limit).size());
	}

	@Test
	public void thatMessageCountWorks() throws MixPersistenceException {
		int count = 5;

		for (int i = 0; i < count; i++) {
			fixture.archive(mcm);
		}

		assertEquals(count, fixture.getMessageCountByChannel(TEST_CHANNEL_NAME));
	}
	
	@Test
	public void thatRetractionRemovesMessage() throws MixPersistenceException {

		String id = fixture.archive(mcm);
		
		fixture.retract(id);
		
		assertNull(fixture.findById(id));
	}

	@Test
	public void thatSearchingContentWorks() throws MixPersistenceException {
		fixture.archive(mcm);

		assertEquals(1, fixture.searchAllMessages("ipsum").size());
		assertEquals(0, fixture.searchAllMessages("not-a-term").size());
	}

	@Test
	public void thatSearchingLimitedContentWorks() throws MixPersistenceException {
		int limit = 5;

		for (int i = 0; i < limit * 2; i++) {
			fixture.archive(mcm);
		}

		assertEquals(limit, fixture.searchAllMessagesLimit("ipsum", limit).size());
		assertEquals(0, fixture.searchAllMessagesLimit("not-a-term", limit).size());
	}

	@Test
	public void thatNullCharacterIsStrippedFromMessage() throws MixPersistenceException {
	    String nullCharStr = "\u0000";

		MessageBuilder builder = new MessageBuilder();
		Message nullMessage = builder.from(fromJID)
				.to(targetChannelJID)
				.type(Type.chat)
				.body(nullCharStr)
                .subject(nullCharStr)
				.build();

		MixChannelMessage nullBodyMessage = new MixChannelMessageImpl(nullMessage, TEST_CHANNEL_NAME, mockMCP);

		String id = fixture.archive(nullBodyMessage);

        ArchivedMixChannelMessage archived = fixture.findById(id);

        assertFalse(archived.getSubject().contains(nullCharStr));
        assertFalse(archived.getBody().contains(nullCharStr));
        assertFalse(archived.getStanza().contains(nullCharStr));

	}

}