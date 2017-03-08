package org.jivesoftware.openfire.mix.mam.repository;

import static org.junit.Assert.*;

import org.jmock.Mockery;
import org.junit.Test;

public class TimeBasedChannelQueryTest {
	
	private Mockery mockContext = new Mockery();
	
	private MixChannelArchiveRepository repository = mockContext.mock(MixChannelArchiveRepository.class);

	@Test
	public void testConstructorExtractsBoundQueryTerms() {
		assertNotNull(new TimeBasedChannelQuery(repository, MamTestUtils.getTimeBoundQuery()));
	}

	@Test
	public void testConstructorExtractsStartQueryTerm() {
		assertNotNull(new TimeBasedChannelQuery(repository, MamTestUtils.getAfterQuery()));
	}
	
}
