package org.jivesoftware.openfire.mix.mam.repository;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.NamedList;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.mam.ArchivedMixChannelMessage;
import org.jivesoftware.openfire.mix.model.MixChannelMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class SolrMixChannelArchiveRepositoryImpl implements MixChannelArchiveRepository {

    private static final Logger logger = LoggerFactory.getLogger(SolrMixChannelArchiveRepositoryImpl.class);


    private final SolrClient solr;

    public SolrMixChannelArchiveRepositoryImpl(SolrClient client) {
        this.solr = client;
    }

    @Override
    public String archive(MixChannelMessage msg) throws MixPersistenceException {
        ArchivedMixChannelMessage archive = new ArchivedMixChannelMessage(msg);

        // TODO Should we be getting solr to sort this for us?  Like we do with postgres
        archive.setId(UUID.randomUUID().toString());

        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", archive.getId());
        doc.addField("subject", msg.getMessage().getSubject());
        doc.addField("body", msg.getMessage().getBody());
        doc.addField("stanza", archive.getStanza());
        doc.addField("fromJID", archive.getFromJID());
        doc.addField("channel", archive.getChannel());

        archive.setArchiveTimestamp(new Date());
        doc.addField("archiveTimestamp", archive.getArchiveTimestamp());

        try {
            UpdateResponse response = solr.add(doc);
            solr.commit();
        } catch (SolrServerException | IOException e) {
            throw new MixPersistenceException(e);
        }

        return archive.getId();
    }

    @Override
    public ArchivedMixChannelMessage findById(String id) {

        SolrQuery q = new SolrQuery();
        q.setRequestHandler("/get");
        q.set("id", id);
        q.set("fl", "*");

        try {
            QueryResponse response = solr.query(q);

            // This is a bit of a hack to allow us to get the near real time get request handler, which returns a single result.
            SolrDocumentList sdl = new SolrDocumentList();
            NamedList responseList = response.getResponse();
            SolrDocument doc = (SolrDocument) responseList.get("doc");
            if (doc != null){
                sdl.add(doc);
            }

            if (!sdl.isEmpty()){
                DocumentObjectBinder dob = new DocumentObjectBinder();
                List<ArchivedMixChannelMessage> results = dob.getBeans(ArchivedMixChannelMessage.class, sdl);

                if (results.size() == 1) {
                    return results.get(0);
                }
            }
            else {
                return null;
            }


        } catch (SolrServerException | IOException e) {
            logger.error("Exception caught " + e);
        }
        return null;
    }

    @Override
    public List<ArchivedMixChannelMessage> findMessagesByChannel(String channel) {
        SolrQuery q = new SolrQuery();
        q.setRequestHandler("/select");
        q.set("q", "*:*");
        q.set("fl", "*");
        q.setFilterQueries("channel:" + channel);

        List<ArchivedMixChannelMessage> channelMessages = Collections.emptyList();

        try {
            QueryResponse response = solr.query(q);
             channelMessages = response.getBeans(ArchivedMixChannelMessage.class);
        } catch (SolrServerException | IOException e) {
            logger.error("Exception caught " + e);
        }

        return channelMessages;
    }

    @Override
    public List<ArchivedMixChannelMessage> findMessagesByChannelAfter(String channel, String after) {
        return null;
    }

    @Override
    public List<ArchivedMixChannelMessage> findMessagesByChannelSince(String channel, Date after) {
        return null;
    }

    @Override
    public long getMessageCountByChannel(String channel) {
        SolrQuery q = new SolrQuery();
        q.setRequestHandler("/select");
        q.set("q", "*:*");
        q.set("fl", "*");

        // This is important, don't bring back any results
        q.setRows(0);
        q.setFilterQueries("channel:" + channel);

        List<ArchivedMixChannelMessage> channelMessages = Collections.emptyList();

        Long numResults = new Long(0);
        try {
            QueryResponse response = solr.query(q);
            numResults =  response.getResults().getNumFound();

        } catch (SolrServerException | IOException e) {
            logger.error("Exception caught " + e);
        }

        return numResults;
    }

    @Override
    public void retract(String id) throws MixPersistenceException {
        try {
            solr.deleteById(id);
            solr.commit();
        } catch (SolrServerException | IOException e) {
            logger.error("Exception caught " + e);
        }

        return;
    }

    @Override
    public List<ArchivedMixChannelMessage> findMessagesByChannelWith(String mixChannelJid, String term) {
        SolrQuery q = new SolrQuery();
        q.setRequestHandler("/select");
        q.set("df", "stanza");
        q.set("q", term);
        q.set("fl", "*");
        q.setFilterQueries("channel:" + mixChannelJid);

        List<ArchivedMixChannelMessage> channelMessages = Collections.emptyList();

        try {
            QueryResponse response = solr.query(q);
            channelMessages = response.getBeans(ArchivedMixChannelMessage.class);
        } catch (SolrServerException | IOException e) {
            logger.error("Exception caught " + e);
        }

        return channelMessages;
    }

    @Override
    public List<ArchivedMixChannelMessage> findTimeBoundMessagesByChannel(String channelName, Date start, Date end) {
        return null;
    }

    @Override
    public List<ArchivedMixChannelMessage> findLimitedMessagesByChannelWith(String node, String term, int limit) {
        return null;
    }

    @Override
    public List<ArchivedMixChannelMessage> findLimitedMessagesByChannel(String channelName, int limit) {
        return null;
    }

    @Override
    public List<ArchivedMixChannelMessage> findLimitedTimeBoundMessagesByChannel(String channelName, Date start, Date end, int limit) {
        return null;
    }

    @Override
    public List<ArchivedMixChannelMessage> findLimitedMessagesByChannelSince(String channelName, Date start, int limit) {
        return null;
    }
}
