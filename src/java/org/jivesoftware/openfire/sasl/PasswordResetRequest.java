package org.jivesoftware.openfire.sasl;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.user.ResetToken;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.EmailService;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringTokenizer;

public class PasswordResetRequest implements SaslServer {
    private boolean completed = false;
    private String username = null;
    private CallbackHandler cbh = null;
    public static final String MECH_NAME = "PASSWORD-RESET-TRIGGER";
    public static final String USERPROP_TOKEN = "openfire.password-reset-token";

    public PasswordResetRequest(CallbackHandler cbh) throws SaslException {
        this.cbh = cbh;
    }

    @Override
    public String getMechanismName() {
        return MECH_NAME;
    }

    @Override
    public byte[] evaluateResponse(byte[] bytes) throws SaslException {
        /**
         * Only response (may be initial) is username \0 email
         */
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            completed = true;
            StringTokenizer tokens = new StringTokenizer(new String(bytes, StandardCharsets.UTF_8), "\0");
            username = tokens.nextToken();
            String email = tokens.nextToken();

            // normalize username
            AuthorizeCallback acb = new AuthorizeCallback(username,username);
            cbh.handle(new Callback[]{acb});
            if(acb.isAuthorized()) {
                username = acb.getAuthorizedID();
                completed = true;
            } else {
                completed = true;
                username = null;
                throw new SaslException("PASSWORD-RESET-REQUEST: user not authorized: "+username);
            }
            User user = UserManager.getInstance().getUser(username);
            Map<String, String> userProperties = user.getProperties();
            if (user.getEmail().equals(email)) {
                ResetToken token = new ResetToken(1000L * 60L * 60L); // one hour
                userProperties.put(USERPROP_TOKEN, token.toString());

                EmailService.getInstance().sendMessage(
                        user.getName(),
                        user.getEmail(),
                        "admin",
                        "admin@" + XMPPServer.getInstance().getServerInfo().getXMPPDomain(),
                        "Password Reset",
                        "Here is your password reset token: " + token.getToken(),
                        "Here is your password reset token: " + token.getToken()
                );

                throw new SaslFailureException(Failure.TEMPORARY_AUTH_FAILURE);
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
        return false;
    }

    @Override
    public String getAuthorizationID() {
        throw new IllegalStateException("PassswordResetRequest not completed");
    }

    @Override
    public byte[] unwrap(byte[] bytes, int i, int i1) throws SaslException {
        throw new IllegalStateException("Unwrap for password reset request");
    }

    @Override
    public byte[] wrap(byte[] bytes, int i, int i1) throws SaslException {
        throw new IllegalStateException("Unwrap for password reset request");
    }

    @Override
    public Object getNegotiatedProperty(String propName) {
        throw new IllegalStateException("PassswordResetRequest not completed");
    }

    @Override
    public void dispose() throws SaslException {
        username = null;
        completed = false;
    }
}
