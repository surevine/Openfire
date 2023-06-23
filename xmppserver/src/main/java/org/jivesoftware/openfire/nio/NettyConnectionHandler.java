/*
 * Copyright (C) 2005-2008 Jive Software, 2022 Ignite Realtime Foundation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.openfire.nio;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.dom4j.io.XMPPPacketReader;
import org.jivesoftware.openfire.Connection;
import org.jivesoftware.openfire.net.MXParser;
import org.jivesoftware.openfire.net.ServerTrafficCounter;
import org.jivesoftware.openfire.net.StanzaHandler;
import org.jivesoftware.openfire.spi.ConnectionConfiguration;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmpp.packet.StreamError;

import java.nio.charset.StandardCharsets;

/**
 * A ConnectionHandler is responsible for creating new sessions, destroying sessions and delivering
 * received XML stanzas to the proper StanzaHandler.
 *
 * @author Gaston Dombiak
 */
public abstract class NettyConnectionHandler extends ChannelInboundHandlerAdapter {

    private static final Logger Log = LoggerFactory.getLogger(NettyConnectionHandler.class);

    static final String XML_PARSER = "XML-PARSER";
    static final String HANDLER = "HANDLER";
    static final String CONNECTION = "CONNECTION";

    protected static final ThreadLocal<XMPPPacketReader> PARSER_CACHE = new ThreadLocal<XMPPPacketReader>()
            {
               @Override
               protected XMPPPacketReader initialValue()
               {
                  final XMPPPacketReader parser = new XMPPPacketReader();
                  parser.setXPPFactory( factory );
                  return parser;
               }
            };
    /**
     * Reuse the same factory for all the connections.
     */
    private static XmlPullParserFactory factory = null;

    static {
        try {
            factory = XmlPullParserFactory.newInstance(MXParser.class.getName(), null);
            factory.setNamespaceAware(true);
        }
        catch (XmlPullParserException e) {
            Log.error("Error creating a parser factory", e);
        }
    }

    /**
     * The configuration for new connections.
     */
    protected final ConnectionConfiguration configuration;

    protected NettyConnectionHandler(ConnectionConfiguration configuration ) {
        this.configuration = configuration;
    }

    abstract NettyConnection createNIOConnection(ChannelHandlerContext ctx);

    abstract StanzaHandler createStanzaHandler(NettyConnection connection);

    /**
     * Returns the max number of seconds a connection can be idle (both ways) before
     * being closed.<p>
     *
     * @return the max number of seconds a connection can be idle.
     */
    abstract int getMaxIdleTime();

    /**
     * Updates the system counter of read bytes. This information is used by the incoming
     * bytes statistic.
     *
     * @param session the session that read more bytes from the socket.
     */
    private void updateReadBytesCounter(ChannelHandlerContext ctx) {
        long currentBytes = session.getReadBytes();
        Long prevBytes = (Long) session.getAttribute("_read_bytes");
        long delta;
        if (prevBytes == null) {
            delta = currentBytes;
        }
        else {
            delta = currentBytes - prevBytes;
        }
        session.setAttribute("_read_bytes", currentBytes);
        ServerTrafficCounter.incrementIncomingCounter(delta);
    }

    /**
     * Updates the system counter of written bytes. This information is used by the outgoing
     * bytes statistic.
     *
     * @param session the session that wrote more bytes to the socket.
     */
    private void updateWrittenBytesCounter(IoSession session) {
        long currentBytes = session.getWrittenBytes();
        Long prevBytes = (Long) session.getAttribute("_written_bytes");
        long delta;
        if (prevBytes == null) {
            delta = currentBytes;
        }
        else {
            delta = currentBytes - prevBytes;
        }
        session.setAttribute("_written_bytes", currentBytes);
        ServerTrafficCounter.incrementOutgoingCounter(delta);
    }
}
