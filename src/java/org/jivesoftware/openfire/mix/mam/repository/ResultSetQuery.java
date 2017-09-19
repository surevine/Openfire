package org.jivesoftware.openfire.mix.mam.repository;

import java.util.List;

import org.jivesoftware.openfire.mix.mam.ArchivedMixChannelMessage;

public interface ResultSetQuery extends Query {

	public List<ArchivedMixChannelMessage> execute();
}
