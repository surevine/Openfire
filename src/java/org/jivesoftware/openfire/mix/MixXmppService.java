package org.jivesoftware.openfire.mix;

import java.util.List;

import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.mix.handler.MixPacketHandler;
import org.jivesoftware.openfire.mix.handler.channel.MixChannelPacketHandler;
import org.jivesoftware.openfire.mix.handler.service.MixServicePacketHandler;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.util.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;
import org.xmpp.packet.Presence;

public class MixXmppService implements PacketRouter {
	private static final Logger LOG = LoggerFactory.getLogger(MixXmppService.class);
	
	private List<MixServicePacketHandler> servicePacketHandlers;
	private List<MixChannelPacketHandler> channelPacketHandlers;
	
	private PacketRouter router;
	
	public MixXmppService(PacketRouter router, List<MixServicePacketHandler> servicePacketHandlers,
			List<MixChannelPacketHandler> channelPacketHandlers) {
		this.router = router;
		this.servicePacketHandlers = servicePacketHandlers;
		this.channelPacketHandlers = channelPacketHandlers;
	}

	public void processPacket(MixService mixService, Packet packet) {
		if (!mixService.isServiceEnabled()) {
			return;
		}

		try {
			if (packet.getTo().getNode() == null) {
				handlePacket(mixService, packet);
			} else {
				// The packet is a normal packet that should possibly be sent to
				// the node
				String channelName = packet.getTo().getNode();
				MixChannel channel = mixService.getChannel(channelName);
				
				handlePacket(channel, packet);
			}
		} catch (Exception e) {
			LOG.error(LocaleUtils.getLocalizedString("admin.error"), e);
		}
	}

	@SuppressWarnings("unchecked")	// Don't like having to do this!
	private <T> void handlePacket(T object, Packet packet) {
		if(object == null) {
			if (packet instanceof IQ) {
				replyWithError((IQ) packet, new PacketError(PacketError.Condition.item_not_found));
				return;
			} else if (packet instanceof Message) {
				replyWithError((Message) packet, new PacketError(PacketError.Condition.item_not_found));
				return;
			} else {
				return;
			}
		}
		
		List<?> packetHandlers;
		
		if(object instanceof MixService) {
			packetHandlers = servicePacketHandlers;
		} else if(object instanceof MixChannel) {
			packetHandlers = channelPacketHandlers;
		} else {
			return;
		}
		
		if (packet instanceof IQ) {
			try {
				for (Object handlerObj : packetHandlers) {
					MixPacketHandler<T> handler = (MixPacketHandler<T>) handlerObj;
					
					IQ result = handler.processIQ(object, (IQ) packet);
	
					if (result != null) {
						router.route(result);
						break;
					}
				}
			} catch (Exception e) {
				LOG.error(LocaleUtils.getLocalizedString("admin.error"), e);
				replyWithError((IQ) packet, new PacketError(PacketError.Condition.internal_server_error));
			}
		} else if (packet instanceof Message) {
			try {
				for (Object handlerObj : packetHandlers) {
					MixPacketHandler<T> handler = (MixPacketHandler<T>) handlerObj;
					
					if(handler.processMessage(object, (Message) packet)) {
						break;
					}
				}
			} catch (Exception e) {
				LOG.error(LocaleUtils.getLocalizedString("admin.error"), e);
				replyWithError((Message) packet, new PacketError(PacketError.Condition.internal_server_error));
			}
		}
	}
	
	private void replyWithError(Message message, PacketError error) {
		final Message reply = message.createCopy();
		reply.setFrom(message.getTo());
		reply.setTo(message.getFrom());
		reply.setError(error);
		route(reply);		
	}
	
	private void replyWithError(IQ iq, PacketError error) {
		final IQ reply = IQ.createResultIQ(iq);
		reply.setChildElement(iq.getChildElement().createCopy());
		reply.setError(error);
		route(reply);		
	}

	@Override
	public void route(Packet packet) {
		router.route(packet);
	}

	@Override
	public void route(IQ packet) {
		router.route(packet);
	}

	@Override
	public void route(Message packet) {
		router.route(packet);
	}

	@Override
	public void route(Presence packet) {
		router.route(packet);
	}
}
