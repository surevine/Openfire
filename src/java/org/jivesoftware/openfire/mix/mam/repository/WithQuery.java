package org.jivesoftware.openfire.mix.mam.repository;

import java.util.List;

import org.jivesoftware.openfire.mix.mam.ArchivedMixChannelMessage;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

public class WithQuery extends AbstractResultSetQuery {

	private MixChannelArchiveRepository repository;

	private JID channel;

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
			return repository.findMessagesByChannelWith(this.channel.getNode(), this.term);	
		} else {
			return repository.findLimitedMessagesByChannelWith(this.channel.getNode(), this.term, limit);	

		}
		
	}

}
