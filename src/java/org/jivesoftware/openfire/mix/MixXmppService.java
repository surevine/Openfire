package org.jivesoftware.openfire.mix;

import org.jivesoftware.openfire.PacketRouter;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;

public interface MixXmppService extends PacketRouter {

	void processReceivedPacket(MixService mixService, Packet packet);

	void replyWithError(Packet message, PacketError error);
}