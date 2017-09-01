package org.jivesoftware.openfire.net;

import org.jivesoftware.openfire.sasl.Failure;
import org.jivesoftware.openfire.sasl.PasswordResetToken;
import org.jivesoftware.openfire.sasl.SaslFailureException;
import org.jivesoftware.openfire.user.User;

public class PasswordResetTask implements PostAuthenticationTask {
    private User user;
    private boolean done;
    public static final String USERPROP_RESET_REQUIRED = "openfire.password.reset";

    public PasswordResetTask(User user) throws SaslFailureException {
        this.user = user;
        this.done = false;
        if (!user.getProperties().containsKey(USERPROP_RESET_REQUIRED)) {
            throw new SaslFailureException(Failure.NOT_AUTHORIZED, "No password reset requested");
        }
    }

    public byte[] evaluateResponse(byte[] response) {
        // Set the user's password to this bytearray as a string.
        user.setPassword(new String(response));
        user.getProperties().remove(USERPROP_RESET_REQUIRED);
        user.getProperties().remove(PasswordResetToken.USERPROP_TOKEN);
        this.done = true;
        return null;
    }

    public boolean isCompleted() {
        return this.done;
    }
}
