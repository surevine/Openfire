package org.jivesoftware.openfire.net;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.AuthFactory;
import org.jivesoftware.openfire.sasl.Failure;
import org.jivesoftware.openfire.sasl.SaslFailureException;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Not client sends first (ie, no initial response)
 * Server "challenge" is TOTP URI.
 * Client sends TOTP code to complete.
 * SASL2 framework will then demand a TOTP code, which is unfortunate.
 */

public class TotpSetupTask implements PostAuthenticationTask {
    private static final Logger Log = LoggerFactory.getLogger(TotpSetupTask.class);

    private boolean completed = false;
    private User user;
    private static GoogleAuthenticator googleAuthenticator = null;
    private GoogleAuthenticatorKey key = null;
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
            // Expecting 6 ASCII digits.
            if (response.length != 6) {
                throw new SaslFailureException(Failure.MALFORMED_REQUEST, "TOTP code too short");
            }
            if (key == null) {
                throw new SaslFailureException(Failure.MALFORMED_REQUEST, "No TOTP code being setup");
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

            Log.debug("Authorizing secret on setup: {}, code: {}", key.getKey(), totpCode);
            boolean OK = googleAuthenticator.authorize(key.getKey(), totpCode);
            completed = true;
            if (!OK) {
                throw new SaslFailureException(Failure.NOT_AUTHORIZED, "TOTP code error");
            }
            user.getProperties().put("openfire.totp.secret", AuthFactory.encryptPassword(key.getKey()));
            return null;
        }
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();

        String userId = user.getUsername() + "@" + XMPPServer.getInstance().getServerInfo().getXMPPDomain();
        String uri = "otpauth://totp/" + "Openfire:" + userId + "?secret=" + key.getKey() + "&issuer=Openfire";
        // String uri = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL("Openfire", user.getUsername() + "@" + XMPPServer.getInstance().getServerInfo().getXMPPDomain(), key);
        return uri.getBytes();
    }

    @Override
    public boolean isCompleted() {
        return false;
    }
}
