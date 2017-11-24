package org.jivesoftware.openfire.mix.mam.repository;

import java.util.List;

import org.jivesoftware.openfire.mix.mam.ArchivedMixChannelMessage;
import org.xmpp.packet.IQ;

public class BareChannelQuery extends AbstractResultSetQuery {
	
	public BareChannelQuery(MixChannelArchiveRepository mar, IQ query) {
		super(mar, query);
	}

	@Override
	public List<ArchivedMixChannelMessage> execute() {
		if (limit == 0) {
			return repository.findMessagesByChannel(channelName);	
		} else {
			return repository.findLimitedMessagesByChannel(channelName, limit);
		}
		
	}

}
