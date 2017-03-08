package org.jivesoftware.openfire.mix.mam.repository;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MamQueryFactoryTest {

	private QueryFactory fixture = new MamQueryFactory();

	@Test
	public void testFilterQuery() {
		assertTrue(fixture.create(null, MamTestUtils.getFilterQuery()) instanceof WithQuery);
	}
	
	@Test
	public void testChannelQuery() {
		assertTrue(fixture.create(null, MamTestUtils.getBaseQuery()) instanceof BareChannelQuery);
	}
	
	@Test
	public void testTimeBoundQuery() {
		assertTrue(fixture.create(null, MamTestUtils.getTimeBoundQuery()) instanceof TimeBasedChannelQuery);
	}
}
