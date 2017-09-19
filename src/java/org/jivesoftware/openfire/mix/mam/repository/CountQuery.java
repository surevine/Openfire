package org.jivesoftware.openfire.mix.mam.repository;

import org.xmpp.packet.IQ;

public abstract class CountQuery {

	public abstract long execute(IQ queryIQ);
}
