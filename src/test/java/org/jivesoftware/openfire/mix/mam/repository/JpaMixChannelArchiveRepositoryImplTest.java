package org.jivesoftware.openfire.mix.mam.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.jivesoftware.openfire.mix.mam.ArchivedMixChannelMessage;
import org.jivesoftware.openfire.mix.mam.repository.JpaMixChannelArchiveRepositoryImpl;
import org.junit.Test;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Message.Type;
import org.hibernate.jpa.HibernatePersistenceProvider;

public class JpaMixChannelArchiveRepositoryImplTest {

	private JpaMixChannelArchiveRepositoryImpl fixture = new JpaMixChannelArchiveRepositoryImpl("test-mam");
	private final Message testMsg = new Message();
	private static final String TEST_CHANNEL_NAME = "coven";
	private final JID targetChannelJID = new JID(TEST_CHANNEL_NAME, "shakespeare.example", "");
	private final JID fromJID = new JID("hag66", "shakespeare.exaple", "");

	public JpaMixChannelArchiveRepositoryImplTest() {
		new HibernatePersistenceProvider();
		testMsg.setBody(
				"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi ligula erat, ullamcorper at ullamcorper e");
		testMsg.setType(Type.chat);
		testMsg.setSubject("Lorem ipsum");
		testMsg.setTo(targetChannelJID);
		testMsg.setFrom(fromJID);

	}

	@Test
	public void testPersisting() {

		fixture.archive(testMsg);

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
	public void thatAfterUUIDQueryWorks() {

		String id = fixture.archive(testMsg);

		fixture.archive(testMsg);

		assertEquals(1, fixture.findMessagesByChannelAfter(TEST_CHANNEL_NAME, id).size());

	}

	@Test
	public void thatLimitingResultsWorks() {

		int limit = 50;

		for (int i = 0; i < limit * 2; i++) {
			fixture.archive(testMsg);
		}

		assertEquals(limit, fixture.findLimitedMessagesByChannel(TEST_CHANNEL_NAME, limit).size());
	}

	@Test
	public void thatMessageCountWorks() {
		int count = 50;

		for (int i = 0; i < count; i++) {
			fixture.archive(testMsg);
		}

		assertEquals(count, fixture.getMessageCountByChannel(TEST_CHANNEL_NAME));
	}
	
	@Test
	public void thatRetractionRemovesMessage() {

		String id = fixture.archive(testMsg);
		
		fixture.retract(id);
		
		assertNull(fixture.findById(id));
	}

}
