package org.jivesoftware.openfire.mix.mam.repository;

import org.apache.solr.client.solrj.SolrServerException;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.model.MixChannelMessage;
import org.jivesoftware.openfire.mix.model.MixChannelMessageImpl;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.jmock.Expectations;
import org.jmock.Mockery;
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

    public SolrMixChannelArchiveRepositoryImplTest() {


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
        SolrMixChannelArchiveRepositoryImpl fixture = new SolrMixChannelArchiveRepositoryImpl();

        assertNotNull(fixture.archive(mcm));
    }

}