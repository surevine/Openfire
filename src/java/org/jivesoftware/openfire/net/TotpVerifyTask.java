package org.jivesoftware.openfire.net;

import org.jivesoftware.openfire.auth.AuthFactory;
import org.jivesoftware.openfire.sasl.Failure;
import org.jivesoftware.openfire.sasl.SaslFailureException;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.util.JiveGlobals;

import com.warrenstrange.googleauth.GoogleAuthenticator;

public class TotpVerifyTask implements PostAuthenticationTask {
    private User user;
    private boolean completed = false;
    private static GoogleAuthenticator googleAuthenticator = null;
    private String totpSecret = null;
    public TotpVerifyTask(User user) throws SaslFailureException {
        if (!JiveGlobals.getBooleanProperty("openfire.totp", false)) {
            throw new SaslFailureException(Failure.INVALID_MECHANISM, "TOTP not enabled for this service");
        }
        totpSecret = AuthFactory.decryptPassword(user.getProperties().get("openfire.totp.secret"));
        if (totpSecret == null) {
            throw new SaslFailureException(Failure.TEMPORARY_AUTH_FAILURE, "TOTP secret not present");
        }
        this.user = user;
    }

    @Override
    public byte[] evaluateResponse(byte[] response) throws SaslFailureException {
        // Expecting 6 ASCII digits.
        if (response.length != 6) {
            throw new SaslFailureException(Failure.MALFORMED_REQUEST, "TOTP code too short");
        }
        int totpCode = 0;
        for (byte responseByte : response) {
            if (responseByte >= '0' && responseByte <= '9') {
                totpCode *= 10;
                totpCode += (responseByte - '0');
            } else {
                throw new SaslFailureException(Failure.INCORRECT_ENCODING, "TOTP code expected as ASCII");
            }
        }
        if (googleAuthenticator == null) {
            googleAuthenticator = new GoogleAuthenticator();
        }
        boolean OK = googleAuthenticator.authorize(totpSecret, totpCode);
        this.completed = true;
        if (!OK) {
            throw new SaslFailureException(Failure.NOT_AUTHORIZED, "TOTP code error");
        }
        return null;
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }
}
