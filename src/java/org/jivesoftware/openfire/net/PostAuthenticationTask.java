package org.jivesoftware.openfire.net;

import org.jivesoftware.openfire.sasl.SaslFailureException;

public interface PostAuthenticationTask {
    byte[] evaluateResponse(byte[] response) throws SaslFailureException;

    boolean isCompleted();
}
