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
import org.apache.solr.common.util.NamedList;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.mam.ArchivedMixChannelMessage;
import org.jivesoftware.openfire.mix.model.MixChannelMessage;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class SolrMixChannelArchiveRepositoryImpl implements MixChannelArchiveRepository {

    private String solrUrl = "http://localhost:32783/solr/tvx";

    public int query() throws IOException, SolrServerException {

        SolrQuery query = new SolrQuery();
        query.setQuery("ipod");
        query.setRequestHandler("/select");
        query.setFields("name, price");

        SolrClient client = getSolrClient();

        QueryResponse response = client.query(query);

        return response.getResults().size();
    }

    private SolrClient getSolrClient() {
         return new HttpSolrClient.Builder(solrUrl).build();
    }

    @Override
    public String archive(MixChannelMessage msg) throws MixPersistenceException {
        ArchivedMixChannelMessage archive = new ArchivedMixChannelMessage(msg);

        // TODO Should we be getting solr to sort this for us?  Like we do with postgres
        archive.setId(UUID.randomUUID().toString());

        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", archive.getId());
        doc.addField("type", msg.getMessage().getType());
        doc.addField("subject", msg.getMessage().getSubject());
        doc.addField("body", msg.getMessage().getBody());
        doc.addField("stanza", archive.getStanza());
        doc.addField("fromJID", archive.getFromJID());
        doc.addField("channel", archive.getChannel());
//        doc.addField("archiveTimestamp", archive.getArchiveTimestamp());

        SolrClient client = getSolrClient();
        try {
            UpdateResponse response = client.add(doc);
            client.commit();
        } catch (SolrServerException e) {
            throw new MixPersistenceException(e);
        } catch (IOException e) {
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
        SolrClient client = getSolrClient();
        try {
            QueryResponse response = client.query(q);

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


        } catch (SolrServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<ArchivedMixChannelMessage> findMessagesByChannel(String channel) {
        return null;
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
        return 0;
    }

    @Override
    public void retract(String id) throws MixPersistenceException {

    }

    @Override
    public List<ArchivedMixChannelMessage> findMessagesByChannelWith(String mixChannelJid, String term) {
        return null;
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
