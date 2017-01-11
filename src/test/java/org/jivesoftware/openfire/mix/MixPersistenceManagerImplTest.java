package org.jivesoftware.openfire.mix;

import static org.junit.Assert.fail;

import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.util.JiveProperties;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class MixPersistenceManagerImplTest {
	Mockery mockery = new Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};
	
	MixPersistenceManagerImpl mixPersistenceManager;
	
	JiveProperties jiveProperties;
	
	final PacketRouter mockPacketRouter = mockery.mock(PacketRouter.class);
	
	@Before
	public void setUp() throws Exception {
		jiveProperties = mockery.mock(JiveProperties.class);
		
		mixPersistenceManager = new MixPersistenceManagerImpl(jiveProperties, mockPacketRouter);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	@Ignore
	public void testLoadServices() {
		fail("Not yet implemented");
	}

	@Test
	@Ignore
	public void testLoadChannels() {
		fail("Not yet implemented");
	}

}
