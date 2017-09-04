package org.jivesoftware.openfire.user;

import org.jivesoftware.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class DeviceKeyMap {
    private String username;
    private Map<String,DeviceKeyInfo> devices;
    public static final String ALGORITHM = "HmacSHA256";
    public static final String PROPNAME = "openfire.device.keys";
    public final DeviceKeyInfo faked = new DeviceKeyInfo("This is a fake one", true);

    public class DeviceKeyInfo {
        public final String deviceId;
        public final String secret;
        public int counter;
        public boolean real = true;

        public DeviceKeyInfo(String deviceId, boolean real) {
            this.deviceId = deviceId;
            this.secret = StringUtils.randomString(40);
            this.counter = 0;
            this.real = real;
        }

        public DeviceKeyInfo(String parseMe) {
            StringTokenizer tokens = new StringTokenizer(parseMe, "|");
            this.deviceId = tokens.nextToken();
            this.secret = tokens.nextToken();
            this.counter = new Integer(tokens.nextToken());
        }

        public String toString() {
            return this.deviceId + "|" + this.secret + "|" + this.counter;
        }

        public String generateHash() {
            try {
                String gunk = username + "\0" + this.deviceId + "\0" + this.counter;
                ++this.counter;
                SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes("UTF-8"), ALGORITHM);
                Mac hmac = Mac.getInstance(ALGORITHM);
                hmac.init(keySpec);
                byte[] result = hmac.doFinal(gunk.getBytes("UTF-8"));
                return StringUtils.encodeBase64(result);
            } catch (UnsupportedEncodingException|NoSuchAlgorithmException|InvalidKeyException e) {
                return null;
            }
        }
    }

    public DeviceKeyInfo create(String deviceId) {
        DeviceKeyInfo keyInfo =new DeviceKeyInfo(deviceId, true);
        devices.put(deviceId, keyInfo);
        return keyInfo;
    }

    public DeviceKeyMap(String username) {
        devices = new HashMap<>();
        try {
            User user = UserManager.getInstance().getUser(username);
            Map<String, String> userProperties = user.getProperties();
            if (userProperties.containsKey(PROPNAME)) {
                StringTokenizer tokens = new StringTokenizer(userProperties.get(PROPNAME), ";");
                while (tokens.hasMoreTokens()) {
                    DeviceKeyInfo keyInfo = new DeviceKeyInfo(tokens.nextToken());
                    devices.put(keyInfo.deviceId, keyInfo);
                }
            }
        } catch (UserNotFoundException e) {
            // This is OK.
        }
    }

    public DeviceKeyInfo getDeviceKeyInfo(String deviceId) {
        if (devices.containsKey(deviceId)) {
            return devices.get(deviceId);
        }
        return faked;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean one = false;
        for (Map.Entry<String,DeviceKeyInfo> e : devices.entrySet()) {
            if (!e.getValue().real) continue;
            if (one) {
                sb.append(';');
            }
            one = true;
            sb.append(e.getValue().toString());
        }
        return sb.toString();
    }

    public void store() {
        try {
            User user = UserManager.getInstance().getUser(username);
            Map<String, String> userProperties = user.getProperties();
            userProperties.put(PROPNAME, toString());
        } catch (UserNotFoundException e) {
            // Also OK.
        }
    }
}
