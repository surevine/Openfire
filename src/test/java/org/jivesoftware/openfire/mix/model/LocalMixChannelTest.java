package org.jivesoftware.openfire.mix.model;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;

import org.dom4j.DocumentFactory;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.mix.MixService;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

public class LocalMixChannelTest {
	
	private static final String TEST_USER = "hag66";

	private final static String TEST_MIX_CHANNEL_NAME = "coven";
	
	private final static String TEST_SERVICE_DOMAIN = "shakespeare.example";
	
	private final static String TEST_MIX_DOMAIN = "mix." + TEST_SERVICE_DOMAIN;

	private static final JID TEST_USER1_JID = new JID(TEST_USER, TEST_SERVICE_DOMAIN, null);
    private static final JID TEST_USER2_JID = new JID(TEST_USER + 6, TEST_MIX_DOMAIN, null);
    
	private static final String []PARTIAL_NODE_SET = {"urn:xmpp:mix:nodes:messages", "urn:xmpp:mix:nodes:participants", "urn:xmpp:mix:nodes:subject", "urn:xmpp:mix:nodes:config"};
	
	private static final DocumentFactory docFactory = DocumentFactory.getInstance();

	
	Mockery context = new Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};
    
	final MixService mockMixService = context.mock(MixService.class); 
	
	final Packet mockPacket = context.mock(Packet.class);
	
	final PacketRouter mockRouter = context.mock(PacketRouter.class);
	
	private LocalMixChannel fixture = new LocalMixChannel(mockMixService, TEST_MIX_CHANNEL_NAME, mockRouter);
	
	@Before
	public void setUp() {
        context.checking(new Expectations() {{
            allowing(mockMixService).getServiceDomain();
            will(returnValue(TEST_MIX_DOMAIN));
        }});
	}
	
	@Test
	public void thatFirstUserCanJoinChannelAndSubscribeToNodes() {	    
		
		// Expect that a single message is sent to the participant that has just signed up 
	    context.checking(new Expectations() {{
	    	one(mockRouter);
	    }});
		
		MixChannelParticipant mcp = fixture.addParticipant(TEST_USER1_JID, new HashSet<String>(Arrays.asList(PARTIAL_NODE_SET)));
		
		assertEquals(mcp.getRealJid(), TEST_USER1_JID);

	}
	
	@Test
	public void thatSecondUserJoiningTriggersTwoParticipantUpdates() {
		
		context.checking(new Expectations() {{
			// One for the first participant, and two for the second participant
	    	exactly(3).of(mockRouter).route(with(any(Message.class)));
	    }});
		
		fixture.addParticipant(TEST_USER1_JID, new HashSet<String>(Arrays.asList(PARTIAL_NODE_SET)));
		fixture.addParticipant(TEST_USER2_JID, new HashSet<String>(Arrays.asList(PARTIAL_NODE_SET)));
		
	}
}
