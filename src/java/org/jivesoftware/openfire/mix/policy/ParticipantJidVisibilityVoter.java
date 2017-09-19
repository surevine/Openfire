package org.jivesoftware.openfire.mix.policy;

import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityMode;
import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityPreference;
import org.jivesoftware.openfire.mix.handler.MixRequestContext;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.jivesoftware.openfire.mix.policy.PermissionPolicy.Action;

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
public class ParticipantJidVisibilityVoter implements PermissionVoter<MixChannelParticipant> {

	@Override
	public PolicyVoterOutcome vote(MixRequestContext context,
			MixChannelParticipant subject, Action action) {
		switch(context.getMixChannel().getJidVisibilityMode()) {
		case MANDATORY_HIDDEN:
			return PolicyVoterOutcome.DENY;
			
		case MANDATORY_VISIBLE:
			if(subject.getJidVisibilityPreference() != ChannelJidVisibilityPreference.ENFORCE_HIDDEN) {
				return PolicyVoterOutcome.ALLOW;
			} else {
				// Although this state shouldn't happen (as the participant shouldn't be allowed to join the channel),
				// we will fall back to DENY as it's the safest option
				return PolicyVoterOutcome.DENY;
			}
			
		case HIDDEN:
			if(subject.getJidVisibilityPreference() != ChannelJidVisibilityPreference.ENFORCE_VISIBLE) {
				return PolicyVoterOutcome.DENY;
			}
			break;
			
		case VISIBLE:
			if((subject.getJidVisibilityPreference() != ChannelJidVisibilityPreference.ENFORCE_VISIBLE)
					&& (subject.getJidVisibilityPreference() != ChannelJidVisibilityPreference.NO_PREFERENCE)){
				return PolicyVoterOutcome.DENY;
			}
			break;
			
		default:
			// Just a failsafe in case someone adds an option and doesn't update this file
			return PolicyVoterOutcome.DENY;
		}
		
		return PolicyVoterOutcome.ALLOW;
	}

}
