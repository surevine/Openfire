package org.jivesoftware.openfire.net;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.jivesoftware.openfire.auth.AuthFactory;
import org.jivesoftware.openfire.sasl.Failure;
import org.jivesoftware.openfire.sasl.SaslFailureException;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.util.JiveGlobals;

/**
 * Not client sends first (ie, no initial response)
 * Server "challenge" is TOTP URI, which completes.
 * SASL2 framework will then demand a TOTP code. Hmmm.
 */

public class TotpSetupTask implements PostAuthenticationTask {
    private boolean completed = false;
    private User user;
    private static GoogleAuthenticator googleAuthenticator = null;
    TotpSetupTask(User user) throws SaslFailureException {
        this.user = user;
        if (googleAuthenticator == null) {
            googleAuthenticator = new GoogleAuthenticator();
        }
        if (!JiveGlobals.getBooleanProperty("openfire.totp", false)) {
            throw new SaslFailureException(Failure.INVALID_MECHANISM, "TOTP not enabled here.");
        }
    }


    @Override
    public byte[] evaluateResponse(byte[] response) throws SaslFailureException {
        if (response != null && response.length > 0) {
            throw new SaslFailureException(Failure.INCORRECT_ENCODING);
        }
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        user.getProperties().put("openfire.totp.secret", AuthFactory.encryptPassword(key.getKey()));
        completed = true;
        return null;
    }

    @Override
    public boolean isCompleted() {
        return false;
    }
}
