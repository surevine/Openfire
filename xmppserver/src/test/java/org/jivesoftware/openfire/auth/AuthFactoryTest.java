/*
 * Copyright (C) 2026 Ignite Realtime Foundation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.openfire.auth;

import org.jivesoftware.Fixtures;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.lockout.LockOutManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.cache.CacheFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthFactory#authenticate(String, String)}, for usernames given with this server's domain
 * ('alice@domain'), which the default authentication provider accepts.
 *
 * @author Matthew Vivian
 */
public class AuthFactoryTest
{
    static final String DOMAIN = Fixtures.XMPP_DOMAIN;
    static final String PASSWORD = "secret";

    private Class<?> originalProvider;

    /**
     * Accepts 'alice' with the password 'secret', stripping this server's domain as {@link DefaultAuthProvider} does,
     * and records the usernames it is asked about.
     */
    public static class DomainStrippingAuthProvider implements AuthProvider
    {
        public static final List<String> calls = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void authenticate(String username, String password) throws UnauthorizedException
        {
            calls.add(username);
            username = username.trim().toLowerCase();
            if (username.contains("@")) {
                final int index = username.indexOf('@');
                if (!username.substring(index + 1).equals(DOMAIN)) {
                    throw new UnauthorizedException();
                }
                username = username.substring(0, index);
            }
            if (!"alice".equals(username) || !PASSWORD.equals(password)) {
                throw new UnauthorizedException();
            }
        }

        @Override public String getPassword(String username) throws UserNotFoundException { throw new UserNotFoundException(); }
        @Override public void setPassword(String username, String password) { throw new UnsupportedOperationException(); }
        @Override public boolean supportsPasswordRetrieval() { return false; }
        @Override public boolean isScramSupported() { return false; }
        @Override public String getSalt(String username) { throw new UnsupportedOperationException(); }
        @Override public int getIterations(String username) { throw new UnsupportedOperationException(); }
        @Override public String getServerKey(String username) { throw new UnsupportedOperationException(); }
        @Override public String getStoredKey(String username) { throw new UnsupportedOperationException(); }
    }

    @BeforeAll
    public static void setUpClass() throws Exception
    {
        Fixtures.reconfigureOpenfireHome();
        Fixtures.disableDatabasePersistence();
        CacheFactory.initialize();
    }

    @BeforeEach
    public void setUp()
    {
        XMPPServer.setInstance(Fixtures.mockXMPPServer());
        originalProvider = AuthFactory.AUTH_PROVIDER.getValue();
        AuthFactory.AUTH_PROVIDER.setValue(DomainStrippingAuthProvider.class);
        DomainStrippingAuthProvider.calls.clear();
    }

    @AfterEach
    public void tearDown()
    {
        AuthFactory.AUTH_PROVIDER.setValue((Class) originalProvider);
    }

    /**
     * Verifies that a bare username is authenticated as before.
     */
    @Test
    public void testAuthenticateBareUsername() throws Exception
    {
        // Execute system under test.
        final AuthToken token = AuthFactory.authenticate("alice", PASSWORD);

        // Verify result.
        assertEquals("alice", token.getUsername(), "the token must be for the bare username");
        assertEquals(List.of("alice"), DomainStrippingAuthProvider.calls, "the provider must be asked about the username as given");
    }

    /**
     * Verifies that a username with this server's domain is authenticated: the lockout check is made for the bare
     * username, the provider is asked about the username as given, and the token is for the bare username.
     */
    @Test
    public void testAuthenticateUsernameWithServerDomain() throws Exception
    {
        // Execute system under test.
        final AuthToken token = AuthFactory.authenticate("alice@" + DOMAIN, PASSWORD);

        // Verify result.
        assertEquals("alice", token.getUsername(), "the token must be for the bare username");
        assertEquals(List.of("alice@" + DOMAIN), DomainStrippingAuthProvider.calls, "the provider must be asked about the username as given");
    }

    /**
     * Verifies that a wrong password for a username with this server's domain is still refused.
     */
    @Test
    public void testAuthenticateUsernameWithServerDomainAndWrongPassword()
    {
        // Execute system under test & verify result.
        assertThrows(UnauthorizedException.class, () -> AuthFactory.authenticate("alice@" + DOMAIN, "wrong"),
            "a wrong password must be refused");
    }

    /**
     * Verifies that a locked account is refused when the username is given with this server's domain, without asking
     * the provider, and that the refusal is recorded against the bare username.
     */
    @Test
    public void testLockedAccountIsRefusedForUsernameWithServerDomain()
    {
        // Setup test fixture.
        final LockOutManager lockOutManager = mock(LockOutManager.class);
        when(lockOutManager.isAccountDisabled("alice")).thenReturn(true);
        try (final MockedStatic<LockOutManager> lockOutStatic = mockStatic(LockOutManager.class)) {
            lockOutStatic.when(LockOutManager::getInstance).thenReturn(lockOutManager);

            // Execute system under test.
            assertThrows(UnauthorizedException.class, () -> AuthFactory.authenticate("alice@" + DOMAIN, PASSWORD),
                "a locked account must be refused whatever form its username is given in");

            // Verify result.
            verify(lockOutManager, times(1).description("the refusal must be recorded against the bare username")).recordFailedLogin("alice");
            verify(lockOutManager, never().description("the lockout must not be looked up under the username with the domain")).isAccountDisabled("alice@" + DOMAIN);
            assertTrue(DomainStrippingAuthProvider.calls.isEmpty(), "the provider must not be asked about a locked account");
        }
    }

    /**
     * Verifies that a username with another domain is not treated as a local account: the provider is not asked, as
     * before.
     */
    @Test
    public void testUsernameWithOtherDomainIsNotAuthenticated()
    {
        // Execute system under test & verify result.
        assertThrows(Exception.class, () -> AuthFactory.authenticate("alice@other.example", PASSWORD),
            "a username with another domain must not be authenticated");
        assertTrue(DomainStrippingAuthProvider.calls.isEmpty(), "the provider must not be asked about a username with another domain");
    }
}
