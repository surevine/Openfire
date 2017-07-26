package org.jivesoftware.openfire.mix.mam.repository;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.model.MixChannelMessage;
import org.jivesoftware.openfire.mix.model.MixChannelMessageImpl;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import java.io.IOException;

import static org.jivesoftware.openfire.mix.mam.repository.MamTestUtils.MIX_CHANNEL_JID;
import static org.jivesoftware.openfire.mix.mam.repository.MamTestUtils.TEST_MIX_CHANNEL_NAME;
import static org.jivesoftware.openfire.mix.mam.repository.MamTestUtils.TEST_USERS_JID;
import static org.junit.Assert.*;

public class SolrMixChannelArchiveRepositoryImplTest {

    private Mockery context = new Mockery();
    private MixChannelParticipant mockMCP = context.mock(MixChannelParticipant.class);
    private Message testMsg = new Message();
    private MixChannelMessage mcm;
    private SolrMixChannelArchiveRepositoryImpl fixture;

    private static SolrClient server;

    @BeforeClass
    public static void setup() {
        CoreContainer container = new CoreContainer("src/test/resources/solr");
        container.load();
        server = new EmbeddedSolrServer( container, "tvx" );
    }


    @AfterClass
    public static void teardownClass() {
        try {
            server.close();
        } catch (Exception e) {
        }
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

        assertNotNull(fixture.findById(id));
    }

}