package org.jivesoftware.openfire.sasl;

import org.jivesoftware.openfire.user.DeviceKeyMap;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringTokenizer;

public class DeviceKey implements SaslServer{
    private boolean completed;
    private String username;
    private CallbackHandler cbh;

    @Override
    public String getMechanismName() {
        return "DEVICE-KEY";
    }

    @Override
    public byte[] evaluateResponse(byte[] bytes) throws SaslException {
        try {
            StringTokenizer tokens = new StringTokenizer(new String(bytes, StandardCharsets.UTF_8), "\0");
            String username = tokens.nextToken();
            String deviceId = tokens.nextToken();
            String hmac = tokens.nextToken();
            AuthorizeCallback acb = new AuthorizeCallback(username,username);
            cbh.handle(new Callback[]{acb});
            if(acb.isAuthorized()) {
                username = acb.getAuthorizedID();
                completed = true;
            } else {
                completed = true;
                username = "Unreal Username";
                throw new SaslException("PASSWORD-RESET-TOKEN: user not authorized: "+username);
            }
            DeviceKeyMap keyMap = new DeviceKeyMap(username);
            DeviceKeyMap.DeviceKeyInfo keyInfo = keyMap.getDeviceKeyInfo(deviceId);
            String candidate = keyInfo.generateHash();
            keyMap.store();
            if (candidate == null) {
                throw new SaslFailureException(Failure.TEMPORARY_AUTH_FAILURE, "Algorithms unsupported, probably");
            }
            if (!candidate.equals(hmac)) {
                throw new SaslFailureException(Failure.NOT_AUTHORIZED, "HMAC Mismatch");
            }
        } catch (UnsupportedCallbackException e) {
            throw new SaslFailureException(Failure.TEMPORARY_AUTH_FAILURE, "Callback unsupported.");
        } catch (IOException e) {
            throw new SaslFailureException(Failure.TEMPORARY_AUTH_FAILURE, "Callback exception.");
        }
        return null;
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
