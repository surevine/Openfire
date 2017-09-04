package org.jivesoftware.openfire.handler;

import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.user.DeviceKeyMap;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

public class DeviceKeyGen extends IQHandler {
    private static final IQHandlerInfo iqHandlerInfo = new IQHandlerInfo("create", "urn:xmpp:devicekey");

    public DeviceKeyGen() {
        super("DeviceKeyGen");
    }

    @Override
    public IQ handleIQ(IQ packet) throws UnauthorizedException {
        JID barejid = packet.getTo();
        DeviceKeyMap keyMap = new DeviceKeyMap(barejid.getNode());
        Element query = packet.getChildElement();
        String deviceId = query.attributeValue("device-id");
        String deviceName = query.attributeValue("device-name");
        DeviceKeyMap.DeviceKeyInfo keyInfo = keyMap.create(deviceId, deviceName);
        keyMap.store();
        IQ response = IQ.createResultIQ(packet);
        Element secret = response.setChildElement("create", "urn:xmpp:devicekey");
        secret.addAttribute("device-id", keyInfo.deviceId);
        secret.addAttribute("device-name", keyInfo.deviceName);
        secret.setText(keyInfo.secret);
        return response;
    }

    @Override
    public IQHandlerInfo getInfo() {
        return iqHandlerInfo;
    }
}
