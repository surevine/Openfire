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
    private String username = null;
    private CallbackHandler cbh = null;
    public static final String MECH_NAME = "PASSWORD-RESET-REQUEST";

    private static ResetEmailProvider resetEmailProvider = new DefaultResetEmailProvider();

    public static void setResetEmailProvider(ResetEmailProvider provider) {
        resetEmailProvider = provider;
    }

    public interface ResetEmailProvider {
        public void sendMessage(User user, ResetToken resetToken);
    }

    private static class DefaultResetEmailProvider implements ResetEmailProvider {
        @Override
        public void sendMessage(User user, ResetToken resetToken) {

            EmailService.getInstance().sendMessage(
                    user.getName(),
                    user.getEmail(),
                    "admin",
                    "admin@" + XMPPServer.getInstance().getServerInfo().getXMPPDomain(),
                    "Password Reset",
                    "Here is your password reset token: " + resetToken.getToken(),
                    "Here is your password reset token: " + resetToken.getToken()
            );

        }
    }

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
            StringTokenizer tokens = new StringTokenizer(new String(bytes, StandardCharsets.UTF_8), "\0");
            username = tokens.nextToken();
            String email = tokens.nextToken();

            // normalize username
            AuthorizeCallback acb = new AuthorizeCallback(username,username);
            cbh.handle(new Callback[]{acb});
            if(acb.isAuthorized()) {
                username = acb.getAuthorizedID();
            } else {
                username = null;
                throw new SaslException("PASSWORD-RESET-REQUEST: user not authorized: "+username);
            }
            User user = UserManager.getInstance().getUser(username);
            Map<String, String> userProperties = user.getProperties();
            if (user.getEmail().toLowerCase().equals(email.toLowerCase())) {
                ResetToken token = new ResetToken(1000L * 60L * 60L * 8L); // eight hours
                userProperties.put(PasswordResetToken.USERPROP_TOKEN, token.toString());

                resetEmailProvider.sendMessage(user, token);

                throw new SaslFailureException(Failure.NOT_AUTHORIZED);
            }
        } catch (Exception e) {
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
    }
}
