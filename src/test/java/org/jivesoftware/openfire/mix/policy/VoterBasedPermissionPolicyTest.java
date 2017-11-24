package org.jivesoftware.openfire.mix.policy;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.bouncycastle.operator.OutputCompressor;
import org.jivesoftware.openfire.mix.handler.MixRequestContext;
import org.jivesoftware.openfire.mix.policy.PermissionPolicy.Action;
import org.jivesoftware.openfire.mix.policy.PermissionVoter.PolicyVoterOutcome;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VoterBasedPermissionPolicyTest {
	private Mockery mockery = new Mockery();
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testForceAllowStopsVoting() {
        final PermissionVoter<String> voter1 = mockery.mock(PermissionVoter.class, "mockVoter1");
        final PermissionVoter<String> voter2 = mockery.mock(PermissionVoter.class, "mockVoter2");

		final MixRequestContext context = mockery.mock(MixRequestContext.class);
		final String subject = "test";
		final Action action = Action.VIEW;
		
		mockery.checking(new Expectations() {{
			one(voter1).vote(context, subject, action); will(returnValue(PolicyVoterOutcome.FORCE_ALLOW));
			never(voter2).vote(context, subject, action); // Stop voting after a FORCE_ALLOW
		}});
		
		VoterBasedPermissionPolicy<String> policy = new VoterBasedPermissionPolicy<>(Arrays.asList(voter1, voter2));
		
		boolean result = policy.checkPermission(context, subject, action);
		
		assertTrue("Should be allowed", result);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDenyStopsVoting() {
        final PermissionVoter<String> voter1 = mockery.mock(PermissionVoter.class, "mockVoter1");
        final PermissionVoter<String> voter2 = mockery.mock(PermissionVoter.class, "mockVoter2");
        final PermissionVoter<String> voter3 = mockery.mock(PermissionVoter.class, "mockVoter3");

		final MixRequestContext context = mockery.mock(MixRequestContext.class);
		final String subject = "test";
		final Action action = Action.VIEW;
		
		mockery.checking(new Expectations() {{
			one(voter1).vote(context, subject, action); will(returnValue(PolicyVoterOutcome.ALLOW));
			one(voter2).vote(context, subject, action); will(returnValue(PolicyVoterOutcome.DENY));
			never(voter3).vote(context, subject, action); // Stop voting when there's a DENY
		}});
		
		VoterBasedPermissionPolicy<String> policy = new VoterBasedPermissionPolicy<>(Arrays.asList(voter1, voter2));
		
		boolean result = policy.checkPermission(context, subject, action);
		
		assertFalse("Should be denied", result);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAllow() {
		final PermissionVoter<String> voter1 = mockery.mock(PermissionVoter.class, "mockVoter1");
		final PermissionVoter<String> voter2 = mockery.mock(PermissionVoter.class, "mockVoter2");
		final PermissionVoter<String> voter3 = mockery.mock(PermissionVoter.class, "mockVoter3");
		
		final MixRequestContext context = mockery.mock(MixRequestContext.class);
		final String subject = "test";
		final Action action = Action.VIEW;
		
		mockery.checking(new Expectations() {{
			one(voter1).vote(context, subject, action); will(returnValue(PolicyVoterOutcome.ALLOW));
			one(voter2).vote(context, subject, action); will(returnValue(PolicyVoterOutcome.ALLOW));
			one(voter3).vote(context, subject, action); will(returnValue(PolicyVoterOutcome.ALLOW));
		}});
		
		VoterBasedPermissionPolicy<String> policy = new VoterBasedPermissionPolicy<>(Arrays.asList(voter1, voter2));
		
		boolean result = policy.checkPermission(context, subject, action);
		
		assertTrue("Should be allowed", result);
	}

}
