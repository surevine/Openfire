package org.jivesoftware.openfire.mix;

import org.xmpp.packet.JID;

public class TestConstants {

    public static final String TEST_USER = "hag66";

    public final static String TEST_MIX_CHANNEL_NAME = "coven";
    
    public final static String TEST_SERVICE_DOMAIN = "shakespeare.example";
    
    public final static String TEST_MIX_DOMAIN = "mix." + TEST_SERVICE_DOMAIN;

    public static final JID TEST_USERS_JID = new JID(TEST_USER, TEST_SERVICE_DOMAIN, null);
    
    public static final JID MIX_CHANNEL_JID = new JID(TEST_MIX_CHANNEL_NAME, TEST_MIX_DOMAIN, null);
}
