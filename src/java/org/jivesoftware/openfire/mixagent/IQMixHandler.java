package org.jivesoftware.openfire.mixagent;

import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.roster.RosterItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.jivesoftware.openfire.XMPPServer;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.xmpp.packet.JID;

import java.util.HashMap;

public class IQMixHandler extends IQHandler {

    private static final Logger Log = LoggerFactory.getLogger(IQMixHandler.class);

    private final IQHandlerInfo info;

    private MixAgent mixagent;
    private XMPPServer xmppserver;

    private HashMap<String, String> requests = new HashMap<String, String>();

    public IQMixHandler(MixAgent agent) {
        super("MIX Agent IQ Handler");
        mixagent = agent;
        info = new IQHandlerInfo("_any_", "urn:xmpp:mix:0");
    }

    @Override
    public void initialize (XMPPServer server) {
        super.initialize(server);
        xmppserver = server;
    }

    @Override
    public IQHandlerInfo getInfo() {
        return info;
    }

    private void handleJoin(JID clientJID, JID channelJID) {
        mixagent.addToRoster(clientJID.getNode(), channelJID);
    }

    private void handleLeave(JID clientJID, JID channelJID) {
        mixagent.removeFromRoster(clientJID.getNode(), channelJID);
    }

    public IQ handleIQ(IQ packet) {
        Log.info("Handling mix IQ");
        Log.info("Got packet: " + packet.toString());

        Boolean isJoin = packet.getChildElement().getName().equals("join");
        Boolean isLeave = packet.getChildElement().getName().equals("leave");

        if (!isJoin && !isLeave) {
            Log.error("Not join or not leave, ignoring");
            IQ errorResult = IQ.createResultIQ(packet);
            errorResult.setType(IQ.Type.error);
            return errorResult;
        }

        Boolean isSet = packet.getType() == IQ.Type.set;
        Boolean isFromMixService = packet.getFrom().getDomain().startsWith("mix.");
        Boolean isToSelf = packet.getFrom().toBareJID().equals(packet.getTo().toBareJID());

        if (isSet && isToSelf) {
            Log.info("handling: " + packet.getID() + " " + packet.getFrom().toFullJID());
            requests.put(packet.getFrom().toBareJID() + "!" + packet.getID(), packet.getFrom().toFullJID());

            Log.info("Get info from IQ");
            Element child = packet.getChildElement().createCopy();
            Attribute channel = child.attribute("channel");
            child.remove(channel);

            Log.info("Create a request IQ to the mix service");
            IQ leaveIQ = new IQ(IQ.Type.set);
            leaveIQ.setTo(channel.getValue());
            leaveIQ.setFrom(packet.getFrom().toBareJID());
            // should have same id
            leaveIQ.setID(packet.getID());

            leaveIQ.setChildElement(child);

            Log.info("Send leave request to mix service: " + leaveIQ.toString());
            xmppserver.getIQRouter().route(leaveIQ);
            return null;
        }

        if (isFromMixService) {
            Log.info("Got response from mix service, forwarding to client: " + packet.toString());
            try {
                String key = packet.getTo() + "!" + packet.getID();
                JID clientJID = new JID(requests.get(key));
                JID channelJID = new JID(packet.getFrom().toBareJID());

                Element reply = packet.getElement().createCopy();
                IQ replyIQ = new IQ(reply);
                replyIQ.setTo(clientJID);
                xmppserver.getPacketDeliverer().deliver(replyIQ);

                if(replyIQ.getType() == IQ.Type.result) {
                    if (isJoin) {
                        handleJoin(clientJID, channelJID);
                    }
                    if (isLeave) {
                        handleLeave(clientJID, channelJID);
                    }
                    requests.remove(key);
                }
                return null;
            } catch (Exception e) {
                Log.error("Error delivering mix response: " + e.toString());
                return null;
            }
        }

        return null;
    }
}
