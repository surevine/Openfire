package org.jivesoftware.openfire.mix.mam;

import static org.junit.Assert.*;

import org.jivesoftware.openfire.mix.mam.repository.MamTestUtils;
import org.junit.Test;
import org.xmpp.packet.Message;

public class ArchivedMixChannelMessageTest {

	@Test
	public void testFormatting() {
		Message msg = MamTestUtils.getTestMessage().formatMessageResponse(MamTestUtils.getBaseQuery());
		assertNotNull(msg);
	}

}
