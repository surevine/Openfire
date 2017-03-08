package org.jivesoftware.openfire.mixagent;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.container.BasicModule;
import org.jivesoftware.openfire.roster.RosterItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

public class MixAgent extends BasicModule {
    private static Logger Log = LoggerFactory.getLogger(MixAgent.class);

    private XMPPServer xmppServer;
    private IQMixHandler mixHandler;
    private PacketRouter router;

    public MixAgent() {
        super("XMPP Mix Agent");
        Log.info("Started MIX agent");
    }

    @Override
    public void initialize(XMPPServer server) {
        super.initialize(server);
        xmppServer = server;
        mixHandler = new IQMixHandler(this);
        server.getIQRouter().addHandler(mixHandler);
    }

    public void addToRoster(String user, JID channelJID) {
        Log.info("Adding to roster " + user + " " + channelJID.toString());
        try {
            RosterItem rosteritem = xmppServer.getRosterManager().getRoster(user).createRosterItem(channelJID, false, true);
            rosteritem.setSubStatus(RosterItem.SubType.BOTH);
        } catch (Exception e) {
            Log.error("Error adding channel to roster: " + e.toString());
        }
    }

    public void removeFromRoster(String user, JID channelJID) {
        Log.info("Removing from roster " + user + " " + channelJID.toString());
        try {
            xmppServer.getRosterManager().getRoster(user).deleteRosterItem(channelJID, false);
            RosterItem rosteritem = xmppServer.getRosterManager().getRoster(user).createRosterItem(channelJID, false, true);
            rosteritem.setSubStatus(RosterItem.SubType.BOTH);
        } catch (Exception e) {
            Log.error("Error removing channel from roster: " + e.toString());
        }
    }
}
