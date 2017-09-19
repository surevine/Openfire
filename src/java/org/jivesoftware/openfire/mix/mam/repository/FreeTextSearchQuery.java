package org.jivesoftware.openfire.mix.mam.repository;

import org.jivesoftware.openfire.mix.mam.ArchivedMixChannelMessage;
import org.jivesoftware.util.XMPPDateTimeFormat;
import org.xmpp.packet.IQ;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

public class FreeTextSearchQuery extends AbstractResultSetQuery {
    private String term;

    public FreeTextSearchQuery(MixChannelArchiveRepository mar, IQ query) {
        super(mar, query);

        if (params.containsKey("search")) {
            term = params.get("search");
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public List<ArchivedMixChannelMessage> execute() {
        if (limit == 0) {
            return repository.searchAllMessages(this.term);
        } else {
            return repository.searchAllMessagesLimit(this.term, limit);
        }
    }
}
