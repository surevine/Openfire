package org.jivesoftware.openfire.net;

import org.jivesoftware.openfire.auth.AuthFactory;
import org.jivesoftware.openfire.sasl.Failure;
import org.jivesoftware.openfire.sasl.SaslFailureException;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.util.JiveGlobals;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.sasl.Sasl;

public class TotpVerifyTask implements PostAuthenticationTask {
    private static final Logger Log = LoggerFactory.getLogger(TotpVerifyTask.class);

    private User user;
    private boolean completed = false;
    private static GoogleAuthenticator googleAuthenticator = null;
    private String totpSecret = null;
    private int remainingAttempts = JiveGlobals.getIntProperty("openfire.totp.retries", 3);
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

    private void handleVerifyFailure(Failure failure, String reason) throws SaslFailureException {
        --remainingAttempts;
        if (remainingAttempts <= 0) {
            throw new SaslFailureException(failure, reason);
        }
    }

    @Override
    public byte[] evaluateResponse(byte[] response) throws SaslFailureException {
        // if code not provided along with <next>, respond with empty challenge
        if (response == null || response.length == 0) {
            return null;
        }

        // Expecting 6 ASCII digits.
        if (response.length != 6) {
            handleVerifyFailure(Failure.MALFORMED_REQUEST, "TOTP code too short");
            return null;
        }
        int totpCode = 0;
        for (byte responseByte : response) {
            if (responseByte >= '0' && responseByte <= '9') {
                totpCode *= 10;
                totpCode += (responseByte - '0');
            } else {
                handleVerifyFailure(Failure.INCORRECT_ENCODING, "TOTP code expected as ASCII");
                return null;
            }
        }
        if (googleAuthenticator == null) {
            googleAuthenticator = new GoogleAuthenticator();
        }
        Log.debug("Authorizing secret: {}, code: {}", totpSecret, totpCode);
        boolean OK = googleAuthenticator.authorize(totpSecret, totpCode);
        if (OK) {
            this.completed = true;
        } else {
            this.handleVerifyFailure(Failure.NOT_AUTHORIZED, "TOTP code error");
        }
        return null;
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }
}
