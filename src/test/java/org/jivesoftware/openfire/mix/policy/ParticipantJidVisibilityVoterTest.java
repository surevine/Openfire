package org.jivesoftware.openfire.mix.policy;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityMode;
import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityPreference;
import org.jivesoftware.openfire.mix.handler.MixRequestContext;
import org.jivesoftware.openfire.mix.handler.MixRequestContextImpl;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.jivesoftware.openfire.mix.policy.PermissionPolicy.Action;
import org.jivesoftware.openfire.mix.policy.PermissionVoter.PolicyVoterOutcome;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xmpp.packet.JID;

public class ParticipantJidVisibilityVoterTest {
	Mockery mockery = new Mockery();
	
	private static final JID TEST_JID = new JID("hag66@shakespeare.lit");

	ParticipantJidVisibilityVoter voter;
	
	@Before
	public void setUp() throws Exception {
		voter = new ParticipantJidVisibilityVoter();
	}

	@After
	public void tearDown() throws Exception {
	}
	/**
	 * <pre>
	 *   Channel Jid Visibility                     | Participant Visibility                         | Outcome
	 *  ------------------------------------------------------------------------------------------------------------------------
	 *   {@link ChannelJidVisibilityMode#HIDDEN}            | {@link ChannelJidVisibilityPreference#ENFORCE_HIDDEN}  | {@link PolicyVoterOutcome#DENY}
	 *   {@link ChannelJidVisibilityMode#HIDDEN}            | {@link ChannelJidVisibilityPreference#ENFORCE_VISIBLE} | {@link PolicyVoterOutcome#ALLOW}
	 *   {@link ChannelJidVisibilityMode#HIDDEN}            | {@link ChannelJidVisibilityPreference#NO_PREFERENCE}   | {@link PolicyVoterOutcome#DENY}
	 *   {@link ChannelJidVisibilityMode#HIDDEN}            | {@link ChannelJidVisibilityPreference#PREFER_HIDDEN}   | {@link PolicyVoterOutcome#DENY}
	 *   {@link ChannelJidVisibilityMode#MANDATORY_HIDDEN}  | {@link ChannelJidVisibilityPreference#ENFORCE_HIDDEN}  | {@link PolicyVoterOutcome#DENY}
	 *   {@link ChannelJidVisibilityMode#MANDATORY_HIDDEN}  | {@link ChannelJidVisibilityPreference#ENFORCE_VISIBLE} | N/A
	 *   {@link ChannelJidVisibilityMode#MANDATORY_HIDDEN}  | {@link ChannelJidVisibilityPreference#NO_PREFERENCE}   | {@link PolicyVoterOutcome#DENY}
	 *   {@link ChannelJidVisibilityMode#MANDATORY_HIDDEN}  | {@link ChannelJidVisibilityPreference#PREFER_HIDDEN}   | {@link PolicyVoterOutcome#DENY}
	 *   {@link ChannelJidVisibilityMode#MANDATORY_VISIBLE} | {@link ChannelJidVisibilityPreference#ENFORCE_HIDDEN}  | N/A
	 *   {@link ChannelJidVisibilityMode#MANDATORY_VISIBLE} | {@link ChannelJidVisibilityPreference#ENFORCE_VISIBLE} | {@link PolicyVoterOutcome#ALLOW}
	 *   {@link ChannelJidVisibilityMode#MANDATORY_VISIBLE} | {@link ChannelJidVisibilityPreference#NO_PREFERENCE}   | {@link PolicyVoterOutcome#ALLOW}
	 *   {@link ChannelJidVisibilityMode#MANDATORY_VISIBLE} | {@link ChannelJidVisibilityPreference#PREFER_HIDDEN}   | {@link PolicyVoterOutcome#ALLOW}
	 *   {@link ChannelJidVisibilityMode#VISIBLE}           | {@link ChannelJidVisibilityPreference#ENFORCE_HIDDEN}  | {@link PolicyVoterOutcome#DENY}
	 *   {@link ChannelJidVisibilityMode#VISIBLE}           | {@link ChannelJidVisibilityPreference#ENFORCE_VISIBLE} | {@link PolicyVoterOutcome#ALLOW}
	 *   {@link ChannelJidVisibilityMode#VISIBLE}           | {@link ChannelJidVisibilityPreference#NO_PREFERENCE}   | {@link PolicyVoterOutcome#ALLOW}
	 *   {@link ChannelJidVisibilityMode#VISIBLE}           | {@link ChannelJidVisibilityPreference#PREFER_HIDDEN}   | {@link PolicyVoterOutcome#DENY}
	 * </pre>
	 */

	@SuppressWarnings("serial")
	@Test
	public void test() {
		HashMap<ChannelJidVisibilityMode, HashMap<ChannelJidVisibilityPreference, PolicyVoterOutcome>> truthTable = new HashMap<ChannelJidVisibilityMode, HashMap<ChannelJidVisibilityPreference, PolicyVoterOutcome>>() {{
			put(ChannelJidVisibilityMode.HIDDEN, new HashMap<ChannelJidVisibilityPreference, PolicyVoterOutcome>() {{
				put(ChannelJidVisibilityPreference.ENFORCE_HIDDEN, PolicyVoterOutcome.DENY);
				put(ChannelJidVisibilityPreference.ENFORCE_VISIBLE, PolicyVoterOutcome.ALLOW);
				put(ChannelJidVisibilityPreference.NO_PREFERENCE, PolicyVoterOutcome.DENY);
				put(ChannelJidVisibilityPreference.PREFER_HIDDEN, PolicyVoterOutcome.DENY);
			}});
			put(ChannelJidVisibilityMode.MANDATORY_HIDDEN, new HashMap<ChannelJidVisibilityPreference, PolicyVoterOutcome>() {{
				put(ChannelJidVisibilityPreference.ENFORCE_HIDDEN, PolicyVoterOutcome.DENY);
				put(ChannelJidVisibilityPreference.ENFORCE_VISIBLE, PolicyVoterOutcome.DENY); // This is actually an invalid state but we always fall back on DENY
				put(ChannelJidVisibilityPreference.NO_PREFERENCE, PolicyVoterOutcome.DENY);
				put(ChannelJidVisibilityPreference.PREFER_HIDDEN, PolicyVoterOutcome.DENY);
			}});
			put(ChannelJidVisibilityMode.MANDATORY_VISIBLE, new HashMap<ChannelJidVisibilityPreference, PolicyVoterOutcome>() {{
				put(ChannelJidVisibilityPreference.ENFORCE_HIDDEN, PolicyVoterOutcome.DENY); // This is actually an invalid state but we always fall back on DENY
				put(ChannelJidVisibilityPreference.ENFORCE_VISIBLE, PolicyVoterOutcome.ALLOW);
				put(ChannelJidVisibilityPreference.NO_PREFERENCE, PolicyVoterOutcome.ALLOW);
				put(ChannelJidVisibilityPreference.PREFER_HIDDEN, PolicyVoterOutcome.ALLOW);
			}});
			put(ChannelJidVisibilityMode.VISIBLE, new HashMap<ChannelJidVisibilityPreference, PolicyVoterOutcome>() {{
				put(ChannelJidVisibilityPreference.ENFORCE_HIDDEN, PolicyVoterOutcome.DENY);
				put(ChannelJidVisibilityPreference.ENFORCE_VISIBLE, PolicyVoterOutcome.ALLOW);
				put(ChannelJidVisibilityPreference.NO_PREFERENCE, PolicyVoterOutcome.ALLOW);
				put(ChannelJidVisibilityPreference.PREFER_HIDDEN, PolicyVoterOutcome.DENY);
			}});
		}};
		
		for(final ChannelJidVisibilityMode channelMode : truthTable.keySet()) {
			final MixChannel channel = mockery.mock(MixChannel.class);
			
			mockery.checking(new Expectations() {{
				allowing(channel).getJidVisibilityMode(); will(returnValue(channelMode));
			}});
			
			for(final ChannelJidVisibilityPreference userMode : truthTable.get(channelMode).keySet()) {
				final MixChannelParticipant participant = mockery.mock(MixChannelParticipant.class);
				
				mockery.checking(new Expectations() {{
					allowing(participant).getJidVisibilityPreference(); will(returnValue(userMode));
				}});
				
				MixRequestContext context = new MixRequestContextImpl(TEST_JID, null, channel);
				
				PolicyVoterOutcome result = voter.vote(context, participant, Action.VIEW);
				
				PolicyVoterOutcome expected = truthTable.get(channelMode).get(userMode);
				
				assertEquals("Outcome for Channel = " + channelMode + " and Participant = " + userMode + " should be " + expected, expected, result);
			}
		}
	}

}
