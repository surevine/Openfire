package org.jivesoftware.openfire.sasl;

import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.session.LocalClientSession;
import org.jivesoftware.openfire.session.LocalSession;
import org.jivesoftware.openfire.user.DeviceKeyMap;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

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
    private LocalClientSession session;
    public static final String MECH_NAME = "DEVICE-KEY";

    public DeviceKey(LocalSession session, CallbackHandler cbh) {
        this.cbh = cbh;
        this.session = (LocalClientSession)session;
    }

    @Override
    public String getMechanismName() {
        return MECH_NAME;
    }

    @Override
    public byte[] evaluateResponse(byte[] bytes) throws SaslException {
        if (bytes == null || bytes.length == 0) return null;
        try {
            StringTokenizer tokens = new StringTokenizer(new String(bytes, StandardCharsets.UTF_8), "\0");
            String username = tokens.nextToken();
            String deviceId = tokens.nextToken();
            String hmac = tokens.nextToken();
            AuthorizeCallback acb = new AuthorizeCallback(username, username);
            cbh.handle(new Callback[]{acb});
            if (acb.isAuthorized()) {
                username = acb.getAuthorizedID();
                completed = true;
            } else {
                completed = true;
                throw new SaslException("DEVICE-KEY: user not authorized: " + username);
            }
            DeviceKeyMap keyMap = new DeviceKeyMap(username);
            DeviceKeyMap.DeviceKeyInfo keyInfo = keyMap.getDeviceKeyInfo(deviceId);
            String candidate = keyInfo.generateHash();
            keyMap.store();
            if (candidate == null) {
                throw new SaslFailureException(Failure.TEMPORARY_AUTH_FAILURE, "Algorithms unsupported, probably");
            }
            if (!candidate.equals(hmac)) {
                if (keyInfo.real) {
                    throw new SaslFailureException(Failure.NOT_AUTHORIZED, "HMAC Mismatch for device: " + keyInfo.deviceId);
                } else {
                    throw new SaslFailureException(Failure.NOT_AUTHORIZED, "HMAC mismatch for devices: " + deviceId + " " + keyInfo.deviceId);
                }
            }
            // AUTHENTICATED
            session.setSessionData("openfire.totp.suppress", "DEVICE-KEY");
        } catch (SaslFailureException e) {
            throw e;
        } catch (UnsupportedCallbackException e) {
            throw new SaslFailureException("Callback unsupported.", e, Failure.TEMPORARY_AUTH_FAILURE);
        } catch (IOException e) {
            throw new SaslFailureException("Callback exception.", e, Failure.TEMPORARY_AUTH_FAILURE);
        } catch (Exception e) {
            throw new SaslFailureException("Unknown exception.", e, Failure.TEMPORARY_AUTH_FAILURE);
        }
        return null;
    }


    @Override
    public boolean isComplete() {
        return completed;
    }

    @Override
    public String getAuthorizationID() {
        if (!completed) throw new IllegalStateException("DEVICE-KEY not completed");
        return username;
    }

    @Override
    public byte[] unwrap(byte[] bytes, int i, int i1) throws SaslException {
        throw new IllegalStateException("Unwrap for DEVICE-KEY");
    }

    @Override
    public byte[] wrap(byte[] bytes, int i, int i1) throws SaslException {
        throw new IllegalStateException("Unwrap for DEVICE-KEY");
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
            throw new IllegalStateException("DEVICE-KEY authentication not completed");
        }
    }

    @Override
    public void dispose() throws SaslException {
        username = null;
        completed = false;
    }

}
