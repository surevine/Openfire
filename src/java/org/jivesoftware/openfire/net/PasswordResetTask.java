package org.jivesoftware.openfire.net;

import org.jivesoftware.openfire.sasl.Failure;
import org.jivesoftware.openfire.sasl.SaslFailureException;
import org.jivesoftware.openfire.user.User;

public class PasswordResetTask implements PostAuthenticationTask {
    private User user;
    private boolean done;

    public PasswordResetTask(User user) throws SaslFailureException {
        this.user = user;
        this.done = false;
        if (!user.getProperties().containsKey("openfire.password.reset")) {
            throw new SaslFailureException(Failure.NOT_AUTHORIZED, "No password reset requested");
        }
    }

    public byte[] evaluateResponse(byte[] response) {
        // Set the user's password to this bytearray as a string.
        user.setPassword(new String(response));
        user.getProperties().remove("openfire.password.reset");
        this.done = true;
        return null;
    }

    public boolean isCompleted() {
        return this.done;
    }
}
