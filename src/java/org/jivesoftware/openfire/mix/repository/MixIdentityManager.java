package org.jivesoftware.openfire.mix.repository;

import org.jivesoftware.database.SequenceManager;

public class MixIdentityManager implements IdentityManager {

	private SequenceManager seq;
	
	public MixIdentityManager(int sequenceType, int i) {
		seq = new SequenceManager(sequenceType, i);
	}
	
	@Override
	public long nextUniqueID() {
		return seq.nextUniqueID();
	}

}
