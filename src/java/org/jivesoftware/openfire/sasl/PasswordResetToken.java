package org.jivesoftware.openfire.sasl;

import org.jivesoftware.openfire.user.ResetToken;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;

import org.jivesoftware.openfire.net.PasswordResetTask;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.StringTokenizer;

public class PasswordResetToken implements SaslServer {
    private boolean completed = false;
    private String username = null;
    private CallbackHandler cbh = null;
    public static final String MECH_NAME = "PASSWORD-RESET-TOKEN";
    public static final String USERPROP_TOKEN = "openfire.password-reset-token";

    public PasswordResetToken(CallbackHandler cbh) throws SaslException {
        this.cbh = cbh;
    }

    @Override
    public String getMechanismName() {
        return MECH_NAME;
    }

    @Override
    public byte[] evaluateResponse(byte[] bytes) throws SaslException {
        /**
         * Only response (may be initial) is username \0 access-token
         */
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            completed = true;
            StringTokenizer tokens = new StringTokenizer(new String(bytes, StandardCharsets.UTF_8), "\0");
            username = tokens.nextToken();
            String access_token = tokens.nextToken();
            AuthorizeCallback acb = new AuthorizeCallback(username,username);
            cbh.handle(new Callback[]{acb});
            if(acb.isAuthorized()) {
                username = acb.getAuthorizedID();
                completed = true;
            } else {
                completed = true;
                username = null;
                throw new SaslException("PASSWORD-RESET-TOKEN: user not authorized: "+username);
            }
            User user = UserManager.getInstance().getUser(username);
            Map<String, String> userProperties = user.getProperties();
            if (userProperties.containsKey(USERPROP_TOKEN)) {
                ResetToken stored = new ResetToken(userProperties.get(USERPROP_TOKEN));
                ResetToken offered = new ResetToken(null, access_token);
                if (offered.matches(stored)) {
                    // User should now change their password.
                    userProperties.put(PasswordResetTask.USERPROP_RESET_REQUIRED, new Date().toString());
                    return null;
                }
            }
        } catch (UserNotFoundException e) {
            throw new SaslFailureException(Failure.NOT_AUTHORIZED);
        } catch (UnsupportedCallbackException e) {
            throw new SaslFailureException(Failure.NOT_AUTHORIZED);
        } catch (IOException e) {
            throw new SaslFailureException(Failure.NOT_AUTHORIZED);
        }
        throw new SaslFailureException(Failure.NOT_AUTHORIZED);
    }

    @Override
    public boolean isComplete() {
        return completed;
    }

    @Override
    public String getAuthorizationID() {
        if (!completed) throw new IllegalStateException("Password reset not completed");
        return username;
    }

    @Override
    public byte[] unwrap(byte[] bytes, int i, int i1) throws SaslException {
        throw new IllegalStateException("Unwrap for password reset");
    }

    @Override
    public byte[] wrap(byte[] bytes, int i, int i1) throws SaslException {
        throw new IllegalStateException("Unwrap for password reset");
    }

    @Override
    public Object getNegotiatedProperty(String propName) {
        if (completed) {
            if (propName.equals(Sasl.QOP)) {
                return "auth";
            } else {
                return null;
            }
        } else {
            throw new IllegalStateException("PLAIN authentication not completed");
        }
    }

    @Override
    public void dispose() throws SaslException {
        username = null;
        completed = false;
    }
}
