package org.jivesoftware.openfire.nio;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.dom4j.io.XMPPPacketReader;
import org.jivesoftware.openfire.Connection;
import org.jivesoftware.openfire.PacketDeliverer;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.net.ServerStanzaHandler;
import org.jivesoftware.openfire.net.StanzaHandler;
import org.jivesoftware.openfire.spi.ConnectionConfiguration;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.SystemProperty;
import org.xmpp.packet.StreamError;

import java.nio.charset.StandardCharsets;

/**
 * ConnectionHandler that knows which subclass of {@link StanzaHandler} should be created and how to build and configure
 * a {@link NIOConnection}.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class NettyServerConnectionHandler extends NettyConnectionHandler
{

    public NettyServerConnectionHandler(ConnectionConfiguration configuration)
    {
        super(configuration);
    }

    @Override
    NettyConnection createNIOConnection(ChannelHandlerContext ctx) {
        final PacketDeliverer backupDeliverer = BACKUP_PACKET_DELIVERY_ENABLED.getValue() ? XMPPServer.getInstance().getPacketDeliverer() : null;
        return new NettyConnection(ctx, backupDeliverer, configuration);
    }

    @Override
    StanzaHandler createStanzaHandler(NettyConnection connection) {
        return new ServerStanzaHandler( XMPPServer.getInstance().getPacketRouter(), connection );
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        System.out.println("Netty XMPP handler added");
        //        ConnectionHandler.sessionOpened()
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        System.out.println("Netty XMPP handler removed");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // org.jivesoftware.openfire.nio.ConnectionHandler.messageReceived

        // Get the parser to use to process stanza. For optimization there is going
        // to be a parser for each running thread. Each Filter will be executed
        // by the Executor placed as the first Filter. So we can have a parser associated
        // to each Thread
        final XMPPPacketReader parser = PARSER_CACHE.get();
        // Update counter of read btyes

        // updateReadBytesCounter(session); TODO maybe replace with https://netty.io/4.0/api/io/netty/handler/traffic/TrafficCounter.html#currentReadBytes--


        //System.out.println("RCVD: " + message);
        // Let the stanza handler process the received stanza
        try {
            stanzaHandler.process((String) message, parser);
        } catch (Throwable e) { // Make sure to catch Throwable, not (only) Exception! See OF-2367
            Log.error("Closing connection due to error while processing message: {}", message, e);
            final Connection connection = (Connection) session.getAttribute(CONNECTION);
            if ( connection != null ) {
                connection.close(new StreamError(StreamError.Condition.internal_server_error, "An error occurred while processing data raw inbound data."));
            }
        }


        ctx.write(msg); // Echo
        ctx.flush();
        // Netty releases the received message for you when it is written out to the wire.
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }




    // TBD V



    /**
     * Enable / disable backup delivery of stanzas to the XMPP server itself when a stanza failed to be delivered on a
     * server-to-server connection. When disabled, stanzas that can not be delivered on the connection are discarded.
     */
    public static final SystemProperty<Boolean> BACKUP_PACKET_DELIVERY_ENABLED = SystemProperty.Builder.ofType(Boolean.class)
        .setKey("xmpp.server.backup-packet-delivery.enabled")
        .setDefaultValue(true)
        .setDynamic(true)
        .build();


    int getMaxIdleTime()
    {
        return JiveGlobals.getIntProperty( "xmpp.server.idle", 6 * 60 * 1000 ) / 1000;
    }
}
