package org.jivesoftware.openfire.labelling;

import org.dom4j.Element;
import org.dom4j.QName;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.pubsub.PubSubModule;
import org.jivesoftware.openfire.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.*;

import java.util.Set;

/**
 * Created by dwd on 16/03/17.
 */
public class MessageInterceptor implements PacketInterceptor {
    private static final Logger Log = LoggerFactory.getLogger(MessageInterceptor.class);

    private void interceptMessage(AccessControlDecisionFunction acdf, Message msg, Session session, boolean incoming) throws PacketRejectedException {
        try {
            boolean rewritten = false;
            boolean need_rewrite = false;
            SecurityLabel secLabel = (SecurityLabel)msg.getExtension("securitylabel", "urn:xmpp:sec-label:0");
            if (secLabel == null) {
                Log.debug("Label is default");
                need_rewrite = true;
            }
            if (msg.getType() != Message.Type.error) {
                Log.debug("Sender");
                acdf.check(acdf.getClearance(msg.getFrom()), secLabel, null);
            } else {
                Log.debug("Skipped originator and server checks for bounced message");
            }
            Log.debug("Recipient");
            SecurityLabel newlabel = acdf.check(acdf.getClearance(msg.getTo()), secLabel, msg.getTo());
            msg.deleteExtension("securitylabel", "urn:xmpp:sec-label:0");
            if (newlabel != null) {
                msg.addExtension(newlabel);
            }
        } catch (Exception e) {
            Log.info("ACDF rejection: ", e);
            if (incoming) {
                if (msg.getType() != Message.Type.error) {
                    Message error = new Message(); // Don't copy; it might introduce an invalid label.
                    error.setTo(msg.getFrom());
                    error.setFrom(msg.getTo());
                    error.setID(msg.getID());
                    error.setError(PacketError.Condition.forbidden);
                    error.setType(Message.Type.error);
                    XMPPServer.getInstance().getMessageRouter().route(error);
                }
            }
            throw new PacketRejectedException(e);
        }
    }

    @Override
    public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed) throws PacketRejectedException {
        Log.debug("Packet intercept pre: " + packet.toString());
        if (processed) {
            return; // Ignore these.
        }
        AccessControlDecisionFunction acdf = XMPPServer.getInstance().getAccessControlDecisionFunction();
        if (acdf == null) {
            return;
        }
        if (packet instanceof Message) {
            Log.debug("Message intercept: " + packet.toString());
            Message msg = (Message)packet;
            interceptMessage(acdf, msg, session, incoming);
            return;
        } else if (packet instanceof IQ) {
            Log.debug("IQ Intercept: " + packet.toString());
            IQ iq = (IQ)packet;
            if (iq.getType().equals(IQ.Type.result)) {
                Element query = iq.getChildElement();
                if (query == null) return;
                if (query.getQName().equals(QName.get("pubsub", "http://jabber.org/protocol/pubsub"))) {
                    Element items = query.element("items");
                    if (items == null) return;
                    query.remove(items);
                    Element newItems = query.addElement("items");
                    String clearanceTo = acdf.getClearance(iq.getTo());
                    String clearanceFrom = acdf.getClearance(iq.getFrom());
                    for (Element item : items.elements()) {
                        String id = item.attributeValue("id");
                        Element payload = item.elements().get(0);
                        SecurityLabel secLabel = null;
                        if (item.elements().size() > 1) {
                            secLabel = new SecurityLabel(item.elements().get(1));
                        }
                        try {
                            acdf.check(clearanceFrom, secLabel, null);
                            SecurityLabel newLabel = acdf.check(clearanceTo, secLabel, iq.getTo());
                            Element newItem = newItems.addElement("item");
                            newItem.addAttribute("id", id);
                            newItem.add(payload.createCopy());
                            newItem.add(newLabel.getElement());
                        } catch (SecurityLabelException e) {
                            Log.warn("Pubsub item failed ACDF check: ", e);
                        }
                    }
                }
            }
            return;
        }
    }
}
