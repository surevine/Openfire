package org.jivesoftware.openfire.mix.mam.repository;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.mam.ArchivedMixChannelMessage;
import org.jivesoftware.openfire.mix.model.MixChannelMessage;
import org.jivesoftware.openfire.mix.model.MixChannelMessageImpl;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import java.io.IOException;

import static org.jivesoftware.openfire.mix.mam.repository.MamTestUtils.*;
import static org.junit.Assert.*;

public class SolrMixChannelArchiveRepositoryImplTest {

    private Mockery context = new Mockery();
    private MixChannelParticipant mockMCP = context.mock(MixChannelParticipant.class);
    private Message testMsg = new Message();
    private MixChannelMessage mcm;
    private SolrMixChannelArchiveRepositoryImpl fixture;

    private static CoreContainer container;
    private static SolrClient server;

    @BeforeClass
    public static void setup() {

        container = new CoreContainer(System.getProperty("solr.solr.home"));
        container.load();
        if (container.isLoaded("tvx")) {
            server = new EmbeddedSolrServer( container, "tvx" );
        }

    }


    @AfterClass
    public static void teardownClass() {
        try {
            server.close();
            container.unload("tvx", true, true, false);
        } catch (Exception e) {
        }
    }

    @After
    public void deleteAllFromIndex() throws IOException, SolrServerException {
        server.deleteByQuery( "*:*" );
        server.commit();
    }

    public SolrMixChannelArchiveRepositoryImplTest() {

        fixture = new SolrMixChannelArchiveRepositoryImpl(server);

        testMsg.setBody("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi ligula erat, ullamcorper at ullamcorper e");
        testMsg.setType(Message.Type.chat);
        testMsg.setSubject("Lorem ipsum");
        testMsg.setTo(MIX_CHANNEL_JID);
        testMsg.setFrom(TEST_USERS_JID);

        context.checking(new Expectations() {{
            allowing(mockMCP).getJid();
            will(returnValue(TEST_USERS_JID));

            allowing(mockMCP).getNick();
            will(returnValue("test-nick"));
        }});

        mcm = new MixChannelMessageImpl(testMsg, TEST_MIX_CHANNEL_NAME, mockMCP);
    }

    @Test
    public void testPersisting() throws MixPersistenceException {

        assertNotNull(fixture.archive(mcm));
    }

    @Test
    public void testFindByIdentity() throws MixPersistenceException {

        String id = fixture.archive(mcm);

        ArchivedMixChannelMessage result = fixture.findById(id);
        assertNotNull(result);
        assertNotNull(result.getStanza());
        assertNotNull(result.getBody());
        assertNotNull(result.getSubject());
        assertNotNull(result.getArchiveTimestamp());
        assertNotNull(result.getId());
        assertNotNull(result.getChannel());
        assertNotNull(result.getFromJID());
    }

    @Test
    public void testFindMessagesByChannel() throws MixPersistenceException {

        fixture.archive(mcm);
        fixture.archive(mcm);
        testMsg.setTo(new JID("another-name", TEST_MIX_DOMAIN, null));
        fixture.archive(mcm);

        assertEquals(2, fixture.findMessagesByChannel(TEST_MIX_CHANNEL_NAME).size());

    }

    @Test
    public void thatMessageCountWorks() throws MixPersistenceException {
        int count = 50;

        for (int i = 0; i < count; i++) {
            fixture.archive(mcm);
        }

        assertEquals(count, fixture.getMessageCountByChannel(TEST_MIX_CHANNEL_NAME));
    }

    @Test
    public void thatRetractionRemovesMessage() throws MixPersistenceException {

        String id = fixture.archive(mcm);

        fixture.retract(id);

        assertNull(fixture.findById(id));
    }

    @Test
    public void testFindMessagesByChannelWith() throws MixPersistenceException {
        testMsg.setBody("present");
        fixture.archive(mcm);
        testMsg.setBody("absent");
        fixture.archive(mcm);

        assertEquals(1, fixture.findMessagesByChannelWith(TEST_MIX_CHANNEL_NAME, "present").size());
    }
}