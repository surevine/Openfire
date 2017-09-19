package org.jivesoftware.openfire.mix.mam.repository;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.jivesoftware.openfire.mix.mam.ArchivedMixChannelMessage;
import org.jivesoftware.util.XMPPDateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;

public class TimeBasedChannelQuery extends AbstractResultSetQuery {

	private final XMPPDateTimeFormat xmppDateTimeFormat = new XMPPDateTimeFormat();

	private Date start;
	private Date end;

	public TimeBasedChannelQuery(MixChannelArchiveRepository mar, IQ query) {
		super(mar, query);

		try {
			if (params.containsKey("start")) {
				start = xmppDateTimeFormat.parseString(params.get("start"));
			}
			
			if (params.containsKey("end")) {
				end = xmppDateTimeFormat.parseString(params.get("end"));
			}
			
		} catch (ParseException e) {
			throw new IllegalArgumentException();
		}

		if (start == null) {
			throw new IllegalArgumentException();
		}

	}

	@Override
	public List<ArchivedMixChannelMessage> execute() {
		if (end != null) {
			if  (limit == 0) {
				return repository.findTimeBoundMessagesByChannel(channelName, start, end);	
			} else {
				return repository.findLimitedTimeBoundMessagesByChannel(channelName, start, end, limit);	
			}
						
		} else {
			if  (limit == 0) {
				return repository.findMessagesByChannelSince(channelName, start);	
			} else {
				return repository.findLimitedMessagesByChannelSince(channelName, start, limit);	
			}
		}
	}
}
