package org.jivesoftware.openfire.mix.mam;

import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.labelling.AccessControlDecisionFunction;
import org.jivesoftware.openfire.labelling.SecurityLabel;
import org.jivesoftware.openfire.labelling.SecurityLabelException;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.mam.repository.CountQuery;
import org.jivesoftware.openfire.mix.mam.repository.MamQueryFactory;
import org.jivesoftware.openfire.mix.mam.repository.MixChannelArchiveRepository;
import org.jivesoftware.openfire.mix.mam.repository.Query;
import org.jivesoftware.openfire.mix.mam.repository.QueryFactory;
import org.jivesoftware.openfire.mix.mam.repository.ResultSetQuery;
import org.jivesoftware.openfire.mix.model.MixChannelMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.IQ.Type;
import org.xmpp.packet.Message;

public class MessageArchiveServiceImpl implements MessageArchiveService {
	private static final Logger Log = LoggerFactory.getLogger(ArchivedMixChannelMessage.class);

	private MixChannelArchiveRepository repository;

	private PacketRouter router;

	private QueryFactory queryFactory;

	public MessageArchiveServiceImpl(MixChannelArchiveRepository mar, PacketRouter router, QueryFactory queryFactory) {
		this.repository = mar;
		this.router = router;
		this.queryFactory = queryFactory;
	}

	public IQ query(IQ queryIQ) {
		IQ response = queryIQ.createCopy();
		response.setFrom(queryIQ.getTo());
		response.setTo(queryIQ.getFrom());

		if (isValid(queryIQ)) {

			Query query = queryFactory.create(repository, queryIQ);

			String first = null;
			String last = null;
			
			if (query instanceof ResultSetQuery) {
				ResultSetQuery rsq = (ResultSetQuery) query;
				List<ArchivedMixChannelMessage> results = rsq.execute();

				Iterator<ArchivedMixChannelMessage> iter = results.iterator();

				while (iter.hasNext()) {

					ArchivedMixChannelMessage result = iter.next();

					AccessControlDecisionFunction acdf = XMPPServer.getInstance().getAccessControlDecisionFunction();
					SecurityLabel outboundLabel = null;
					if (acdf != null) {
						SecurityLabel archiveLabel = result.getSecurityLabel();
						String clearances = acdf.getClearance(queryIQ.getFrom().asBareJID());
						try {
							outboundLabel = acdf.check(clearances, archiveLabel, queryIQ.getFrom().asBareJID());
						} catch (SecurityLabelException e) {
							Log.warn("Discarding MAM result due to label violation", e);
							continue;
						}
					}

					// Capture ID of first
					if (first == null) {
						first = result.getId();
					}

					if (!iter.hasNext()) {
						last = result.getId();
					}
					Message msg = result.formatMessageResponse(queryIQ);
					if (outboundLabel != null) {
						msg.addExtension(outboundLabel);
					}
					router.route(msg);
				}

			} else if (query instanceof CountQuery) {
				CountQuery cq = (CountQuery) query;
				long count = cq.execute(queryIQ);
			}

			// Now send the 'fin'
			response.setType(Type.result);
			Element fin = response.setChildElement("fin", MessageArchiveService.MAM_NAMESPACE);
			Element set = fin.addElement("set", MessageArchiveService.RSM_NAMESPACE);
			if (first != null) {
				set.addElement("first").addText(first);
			}
			if (last != null) {
				set.addElement("last").addText(last);
			}

			router.route(response);

		} else {
			response = queryIQ.createCopy();
			response.setType(Type.error);
		}

		return response;
	}

	private boolean isValid(IQ query) {

		if (query.getTo() != null || query.getChildElement() != null
				|| !MessageArchiveService.MAM_NAMESPACE.equals(query.getChildElement().getNamespace().toString())) {
			return true;
		} else {
			return false;
		}
	}

	public String archive(MixChannelMessage message) throws MixPersistenceException {
			return repository.archive(message);

	}

}
