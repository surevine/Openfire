package org.jivesoftware.openfire.mix.mam.repository;

import java.util.List;

import org.jivesoftware.openfire.mix.mam.ArchivedMixChannelMessage;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

public class WithQuery extends AbstractResultSetQuery {
	private String term;

	public WithQuery(MixChannelArchiveRepository repository, IQ query) {
		super(repository, query);

		if (params.containsKey("with")) {
			term = params.get("with");
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public List<ArchivedMixChannelMessage> execute() {
		if (limit == 0) {
			return repository.findMessagesByChannelWith(channelName, this.term);
		} else {
			return repository.findLimitedMessagesByChannelWith(channelName, this.term, limit);

		}
		
	}

}
