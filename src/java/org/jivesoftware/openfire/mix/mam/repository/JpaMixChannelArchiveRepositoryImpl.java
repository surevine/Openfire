package org.jivesoftware.openfire.mix.mam.repository;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.mam.ArchivedMixChannelMessage;
import org.jivesoftware.openfire.mix.model.MixChannelMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.Message;
import org.jivesoftware.database.DbConnectionManager;

public class JpaMixChannelArchiveRepositoryImpl implements MixChannelArchiveRepository {

	private static final Logger logger = LoggerFactory.getLogger(JpaMixChannelArchiveRepositoryImpl.class);
	
	private static final String END_PARAM = "end";
	private static final String START_PARAM = "start";
	private static final String FROM_PARAM = "from";
	private static final String TIMESTAMP_PARAM = "ts";
	private static final String CHANNEL_PARAM = "channel";
	private static final String SEARCH_TERM_PARAM = "term";
	private static final String SELECT_TIME_BOUND_BY_CHANNEL = "selectTimeBoundByChannel";
	private static final String SELECT_BY_CHANNEL_WITH = "selectByChannelWith";
	private static final String MESSAGE_COUNT_BY_CHANNEL = "messageCountByChannel";
	private static final String SELECT_MESSAGES_BY_CHANNEL_AFTER = "selectMessagesByChannelAfter";
	private static final String SELECT_MESSAGES_BY_CHANNEL_SINCE = "selectMessagesByChannelSince";
	private static final String SELECT_BY_CHANNEL = "selectByChannel";
	private static final String SELECT_MESSAGES_BY_SEARCH = "selectMessagesBySearch";

	private EntityManagerFactory emf;

	public JpaMixChannelArchiveRepositoryImpl(Map<String,String> config) {
		this("mam", config);
	}

	public JpaMixChannelArchiveRepositoryImpl(String pu, Map<String,String> config) {
		emf = Persistence.createEntityManagerFactory(pu, config);
		EntityManager em = emf.createEntityManager();
		
		Query selectByChannel = em.createQuery("SELECT a FROM ArchivedMixChannelMessage a WHERE a.channel LIKE :channel");
		emf.addNamedQuery(SELECT_BY_CHANNEL, selectByChannel);
		
		Query selectMessagesByChannelSince = em.createQuery("SELECT a FROM ArchivedMixChannelMessage a WHERE a.channel LIKE :channel AND a.archiveTimestamp > :ts");
		emf.addNamedQuery(SELECT_MESSAGES_BY_CHANNEL_SINCE, selectMessagesByChannelSince);
		
		Query selectMessagesByChannelAfter = em.createQuery("SELECT a FROM ArchivedMixChannelMessage a WHERE a.channel LIKE :channel AND a.archiveTimestamp > :ts");
		emf.addNamedQuery(SELECT_MESSAGES_BY_CHANNEL_AFTER, selectMessagesByChannelAfter);
		
		Query messageCountByChannel = em.createQuery("SELECT count(a) FROM ArchivedMixChannelMessage a WHERE a.channel LIKE :channel");
		emf.addNamedQuery(MESSAGE_COUNT_BY_CHANNEL, messageCountByChannel);
		
		Query selectByChannelWith = em.createQuery("SELECT a FROM ArchivedMixChannelMessage a WHERE a.channel LIKE :channel AND a.fromJID LIKE :from");
		emf.addNamedQuery(SELECT_BY_CHANNEL_WITH, selectByChannelWith);

		Query selectTimeBoundByChannel = em.createQuery("SELECT a FROM ArchivedMixChannelMessage a WHERE a.channel LIKE :channel AND a.archiveTimestamp > :start AND a.archiveTimestamp < :end");
		emf.addNamedQuery(SELECT_TIME_BOUND_BY_CHANNEL, selectTimeBoundByChannel);

		Query selectMessagesBySearch = em.createQuery("SELECT a FROM ArchivedMixChannelMessage a WHERE a.stanza LIKE :term");
		emf.addNamedQuery(SELECT_MESSAGES_BY_SEARCH, selectMessagesBySearch);
	}


	public ArchivedMixChannelMessage findById(String id) {
		return this.emf.createEntityManager().find(ArchivedMixChannelMessage.class, id);
	}
	
	public String archive(MixChannelMessage archive) throws MixPersistenceException {

		ArchivedMixChannelMessage tmp = new ArchivedMixChannelMessage(archive);

		JpaMixChannelArchiveRepositoryImpl.stripNullCharactersFromMessageForArchive(tmp);

		EntityManager em = emf.createEntityManager();

		EntityTransaction transaction = em.getTransaction();
		try {

			transaction.begin();
			em.persist(tmp);
			transaction.commit();

		} catch (Exception e) {

		    logger.error("Unable to persist message", e);

			if (transaction.isActive()) {
				transaction.rollback();
			}

			throw new MixPersistenceException("Unable to persist", e);
		}

		return tmp.getId();
	}

	public List<ArchivedMixChannelMessage> findMessagesByChannel(String channel) {
		TypedQuery<ArchivedMixChannelMessage> nq = emf.createEntityManager().createNamedQuery(SELECT_BY_CHANNEL, ArchivedMixChannelMessage.class);
		nq.setParameter(CHANNEL_PARAM, channel);
		return nq.getResultList();
	}

	public List<ArchivedMixChannelMessage> findMessagesByChannelSince(String channel, Date since) {
		TypedQuery<ArchivedMixChannelMessage> nq = emf.createEntityManager().createNamedQuery(SELECT_BY_CHANNEL, ArchivedMixChannelMessage.class);
		nq.setParameter(CHANNEL_PARAM, channel);
		nq.setParameter(TIMESTAMP_PARAM, since);
		return nq.getResultList();
	}
	

	@Override
	public List<ArchivedMixChannelMessage> findLimitedMessagesByChannelSince(String channelName, Date since,
			int limit) {
		TypedQuery<ArchivedMixChannelMessage> nq = emf.createEntityManager().createNamedQuery(SELECT_BY_CHANNEL, ArchivedMixChannelMessage.class);
		nq.setParameter(CHANNEL_PARAM, channelName);
		nq.setParameter(TIMESTAMP_PARAM, since);
		nq.setMaxResults(limit);
		
		return nq.getResultList();
	}

	public void retract(String id) throws MixPersistenceException {

        EntityManager em = emf.createEntityManager();

		EntityTransaction transaction = em.getTransaction();
		try {

			transaction.begin();
			em.remove(em.find(ArchivedMixChannelMessage.class, id));
			transaction.commit();

		} catch (Exception e) {

		    logger.error("Unable to retract message with ID " + id, e);
			if (transaction.isActive()) {
				transaction.rollback();
			}

            throw new MixPersistenceException("Unable to retract message with id " + id, e);

		}

	}

	public List<ArchivedMixChannelMessage> findMessagesByChannelAfter(String channel, String after) {
		TypedQuery<ArchivedMixChannelMessage> nq = emf.createEntityManager().createNamedQuery(SELECT_MESSAGES_BY_CHANNEL_AFTER, ArchivedMixChannelMessage.class);
		nq.setParameter(CHANNEL_PARAM, channel);
		
		// Need to use the timestamp of the 'after' message to seed the query
		nq.setParameter(TIMESTAMP_PARAM, this.findById(after).getArchiveTimestamp());
		
		return nq.getResultList();
	}


	@Override
	public List<ArchivedMixChannelMessage> findLimitedMessagesByChannel(String channelName, int limit) {
		TypedQuery<ArchivedMixChannelMessage> nq = emf.createEntityManager().createNamedQuery(SELECT_BY_CHANNEL, ArchivedMixChannelMessage.class);
		nq.setParameter(CHANNEL_PARAM, channelName);
		nq.setMaxResults(limit);
		
		return nq.getResultList();
	}


	public long getMessageCountByChannel(String channel) {
		TypedQuery<Long> nq = emf.createEntityManager().createNamedQuery(MESSAGE_COUNT_BY_CHANNEL, Long.class);
		nq.setParameter(CHANNEL_PARAM, channel);
		
		return nq.getSingleResult().longValue();
	}

	@Override
	public List<ArchivedMixChannelMessage> findMessagesByChannelWith(String channel, String term) {
		TypedQuery<ArchivedMixChannelMessage> nq = emf.createEntityManager().createNamedQuery(SELECT_BY_CHANNEL_WITH, ArchivedMixChannelMessage.class);
		nq.setParameter(CHANNEL_PARAM, channel);
		nq.setParameter(FROM_PARAM, term);
		
		return nq.getResultList();
	}
	

	@Override
	public List<ArchivedMixChannelMessage> findLimitedMessagesByChannelWith(String channel, String term, int limit) {
		TypedQuery<ArchivedMixChannelMessage> nq = emf.createEntityManager().createNamedQuery(SELECT_BY_CHANNEL_WITH, ArchivedMixChannelMessage.class);
		nq.setParameter(CHANNEL_PARAM, channel);
		nq.setParameter(FROM_PARAM, term);
		nq.setMaxResults(limit);
		
		return nq.getResultList();
	}


	@Override
	public List<ArchivedMixChannelMessage> findTimeBoundMessagesByChannel(String channelName, Date start, Date end) {
		TypedQuery<ArchivedMixChannelMessage> nq = emf.createEntityManager().createNamedQuery(SELECT_TIME_BOUND_BY_CHANNEL, ArchivedMixChannelMessage.class);
		nq.setParameter(CHANNEL_PARAM, channelName);
		nq.setParameter(START_PARAM, start.getTime());
		nq.setParameter(END_PARAM, end.getTime());
		
		return nq.getResultList();
	}


	@Override
	public List<ArchivedMixChannelMessage> findLimitedTimeBoundMessagesByChannel(String channelName, Date start,
			Date end, int limit) {
		TypedQuery<ArchivedMixChannelMessage> nq = emf.createEntityManager().createNamedQuery(SELECT_TIME_BOUND_BY_CHANNEL, ArchivedMixChannelMessage.class);
		nq.setParameter(CHANNEL_PARAM, channelName);
		nq.setParameter(START_PARAM, start.getTime());
		nq.setParameter(END_PARAM, end.getTime());
		nq.setMaxResults(limit);
		
		return nq.getResultList();
	}

	@Override
	public List<ArchivedMixChannelMessage> searchAllMessages(String term) {
		TypedQuery<ArchivedMixChannelMessage> nq = emf.createEntityManager().createNamedQuery(SELECT_MESSAGES_BY_SEARCH, ArchivedMixChannelMessage.class);
		nq.setParameter(SEARCH_TERM_PARAM, '%' + term + '%');

		return nq.getResultList();
	}

	@Override
	public List<ArchivedMixChannelMessage> searchAllMessagesLimit(String term, int limit) {
		TypedQuery<ArchivedMixChannelMessage> nq = emf.createEntityManager().createNamedQuery(SELECT_MESSAGES_BY_SEARCH, ArchivedMixChannelMessage.class);
		nq.setParameter(SEARCH_TERM_PARAM, '%' + term + '%');
		nq.setMaxResults(limit);

		return nq.getResultList();

	}

    /**
     * Postgres doesn't allow text content with the null character '\0' so just strip it from any user generated content.
     *
     * @param toArchive
     */
	public static void stripNullCharactersFromMessageForArchive(ArchivedMixChannelMessage toArchive) {

		if (toArchive.getSubject() != null) {
			toArchive.setSubject(toArchive.getSubject().replace("\u0000", ""));
		}

		if (toArchive.getBody() != null) {
            toArchive.setBody(toArchive.getBody().replace("\u0000", ""));
		}

		// XML escaped equivalent of null character
        // Suggests this replacement should be done higher up the stack, but as its DB esp. postgres related decided
        // to keep it close to the DB.
		if (toArchive.getStanza() != null) {
            toArchive.setStanza(toArchive.getStanza().replace("&#0;", ""));
		}
    }
}
