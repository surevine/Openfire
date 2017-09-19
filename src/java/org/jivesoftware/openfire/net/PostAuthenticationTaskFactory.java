package org.jivesoftware.openfire.net;

import org.jivesoftware.openfire.sasl.Failure;
import org.jivesoftware.openfire.sasl.SaslFailureException;
import org.jivesoftware.openfire.session.LocalSession;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.util.JiveGlobals;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PostAuthenticationTaskFactory {
    private static PostAuthenticationTaskFactory instance = null;

    static PostAuthenticationTaskFactory getInstance() {
        if (instance == null) {
            instance = new PostAuthenticationTaskFactory();
        }
        return instance;
    }

    Set<String> availableTasks(LocalSession session, User user) {
        Set<String> tasks = new HashSet<>();
        Map<String,String> userProperties = user.getProperties();
        if (userProperties.containsKey("openfire.password.reset")) {
            tasks.add("PASSWORD-RESET");
        }
        if (userProperties.containsKey("openfire.totp.secret")) {
            if (session.getSessionData("openfire.totp.suppress") == null) {
                tasks.add("TOTP");
            }
        } else if (JiveGlobals.getBooleanProperty("openfire.totp", false)) {
            tasks.add("TOTP-INIT");
        }
        return tasks;
    }

    PostAuthenticationTask getTask(User user, String name) throws SaslFailureException {
        if (name.equalsIgnoreCase("PASSWORD-RESET")) {
            return new PasswordResetTask(user);
        }
        if (name.equalsIgnoreCase("TOTP")) {
            return new TotpVerifyTask(user);
        }
        if (name.equalsIgnoreCase("TOTP-INIT")) {
            return new TotpSetupTask(user);
        }
        throw new SaslFailureException(Failure.NOT_AUTHORIZED, "Requested task not understood");
    }
}
