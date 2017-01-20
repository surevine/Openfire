package org.jivesoftware.openfire.mix.handler.channel;

import static org.jivesoftware.openfire.mix.TestConstants.MIX_CHANNEL_JID;
import static org.jivesoftware.openfire.mix.TestConstants.TEST_USERS_JID;
import static org.junit.Assert.assertEquals;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.jivesoftware.openfire.mix.exception.CannotLeaveMixChannelException;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.IQ.Type;
import org.xmpp.packet.JID;

public class MixChannelLeavePacketHandlerTest {
    
	private static final DocumentFactory docFactory = DocumentFactory.getInstance();
	
	private Mockery context = new Mockery();
	
	private MixChannel mockMixChannel = context.mock(MixChannel.class);
	
	private MixChannelLeavePacketHandler fixture = new MixChannelLeavePacketHandler();
	
	private final static String LEAVE_ELEM_NAME = "leave"; 
	
	@Test
	public void testSuccessfulLeaveRequest() throws CannotLeaveMixChannelException {
			
        context.checking(new Expectations() {{
            one(mockMixChannel).removeParticipant(with(equal(TEST_USERS_JID)));
        }});

        IQ response = fixture.processIQ(mockMixChannel, MixChannelLeavePacketHandlerTest.createLeaveRequest(TEST_USERS_JID));
        
        context.assertIsSatisfied();
        assertEquals(LEAVE_ELEM_NAME, response.getChildElement().getName());
		
	}
	
	@Test
	public void testUnsuccessfulLeaveWhenOwner() throws CannotLeaveMixChannelException {
        context.checking(new Expectations() {{
            one(mockMixChannel).removeParticipant(with(equal(TEST_USERS_JID)));
            will(throwException(new CannotLeaveMixChannelException("", "")));
        }});

        fixture.processIQ(mockMixChannel, MixChannelLeavePacketHandlerTest.createLeaveRequest(TEST_USERS_JID));
        
        context.assertIsSatisfied();		
	}
	
	private static IQ createLeaveRequest(final JID sender) {
		IQ leaveRequest = new IQ(Type.set);
		leaveRequest.setTo(MIX_CHANNEL_JID);
		leaveRequest.setFrom(sender);
        
        Element leave = docFactory.createElement(LEAVE_ELEM_NAME, "urn:xmpp:mix:0");
        
        leaveRequest.setChildElement(leave);
        
        return leaveRequest;
	}

}
