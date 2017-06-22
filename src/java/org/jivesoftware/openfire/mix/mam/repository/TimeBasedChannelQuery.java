package org.jivesoftware.openfire.mix.mam.repository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.jivesoftware.openfire.mix.mam.ArchivedMixChannelMessage;
import org.xmpp.packet.IQ;

public class TimeBasedChannelQuery extends AbstractResultSetQuery {

	public static final String MAM_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

	private SimpleDateFormat sdf = new SimpleDateFormat(MAM_DATE_FORMAT);

	private Date start;

	private Date end;

	public TimeBasedChannelQuery(MixChannelArchiveRepository mar, IQ query) {
		super(mar, query);

		try {
			if (params.containsKey("start")) {
				start = sdf.parse(params.get("start"));				
			}
			
			if (params.containsKey("end")) {
				end = sdf.parse(params.get("end"));
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
