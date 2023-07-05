package org.jivesoftware.openfire.nio;

import io.netty.channel.ChannelHandlerContext;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.XMPPPacketReader;
import org.jivesoftware.openfire.*;
import org.jivesoftware.openfire.net.StanzaHandler;
import org.jivesoftware.openfire.server.OutgoingServerSocketReader;
import org.jivesoftware.openfire.server.ServerDialback;
import org.jivesoftware.openfire.session.DomainPair;
import org.jivesoftware.openfire.session.LocalOutgoingServerSession;
import org.jivesoftware.openfire.session.ServerSession;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.spi.BasicStreamIDFactory;
import org.jivesoftware.openfire.spi.ConnectionConfiguration;
import org.jivesoftware.openfire.spi.ConnectionType;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.xmpp.packet.JID;
import org.xmpp.packet.StreamError;

public class NettyOutboundConnectionHandler extends NettyConnectionHandler {
    private static final Logger log = LoggerFactory.getLogger(NettyOutboundConnectionHandler.class);
    private final DomainPair domainPair;

    protected NettyOutboundConnectionHandler(ConnectionConfiguration configuration, DomainPair domainPair) {
        super(configuration);
        this.domainPair = domainPair;
    }

    NettyConnection createNettyConnection(ChannelHandlerContext ctx) {
        return new NettyConnection(ctx, null, configuration);
    }

    @Override
    StanzaHandler createStanzaHandler(NettyConnection connection) {
        return null;
    }

    @Override
    int getMaxIdleTime() {
        return 0;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, String msg) throws DocumentException {
        System.out.println(msg); //TODO
        Document doc = new XMPPPacketReader().parseDocument(msg);
//      // TODO: Move this to the input handler vvvvvvvvvvv
//      final InputStream inputStream;
        if (directTLS) {
            throw new Exception("directTls not yet implemented!");
//          inputStream = connection.getTLSStreamHandler().getInputStream();
        }
//       else {
//       inputStream = socket.getInputStream();}
//       reader.getXPPParser().setInput(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
//
//       // Get the answer from the Receiving Server
//       XmlPullParser xpp = reader.getXPPParser();
//       for (int eventType = xpp.getEventType(); eventType != XmlPullParser.START_TAG; ) {
//          eventType = xpp.next();
//       }
//       // TODO: Move this to the input handler ^^^^^^^^^^

        Element rootStreamElement = doc.getRootElement(); //TODO: Check this is the stream tag
        String serverVersion = rootStreamElement.attribute("version").getText();
        String id = rootStreamElement.attribute("id").getText();
//      String serverVersion = xpp.getAttributeValue("", "version");
//      String id = xpp.getAttributeValue("", "id");
        log.debug("Got a response (stream ID: {}, version: {}). Check if the remote server is XMPP 1.0 compliant...", id, serverVersion);

        if (serverVersion != null && Session.decodeVersion(serverVersion)[0] >= 1) {
            log.debug("The remote server is XMPP 1.0 compliant (or at least reports to be).");
            // Restore default timeout
//          socket.setSoTimeout(soTimeout); //TODO - figure this out in Netty

            Element featuresElement = rootStreamElement.element("features");
//          reader.parseDocument().getRootElement();
            if (featuresElement != null) {
                log.debug("Processing stream features of the remote domain: {}", featuresElement.asXML());
                if (directTLS) {
                    throw new Exception("directTls not yet implemented!");
//                  log.debug("We connected to the remote server using direct TLS. Authenticate the connection with SASL...");
//                  LocalOutgoingServerSession answer = authenticate(domainPair, connection, reader, openingStream, featuresElement, id);
//                  if (answer != null) {
//                      log.debug("Successfully authenticated the connection with SASL)!");
//                      // Everything went fine so return the encrypted and authenticated connection.
//                      log.debug("Successfully created new session!");
//                      return answer;
//                  }
//                  log.debug("Unable to authenticate the connection with SASL.");
                } else {
                    log.debug("Check if both us as well as the remote server have enabled STARTTLS and/or dialback ...");

                    Connection connection = ctx.channel().attr(CONNECTION).get();

                    final boolean useTLS = configuration.getTlsPolicy() == Connection.TLSPolicy.optional || configuration.getTlsPolicy() == Connection.TLSPolicy.required;
                    if (useTLS && featuresElement.element("starttls") != null) {
                        log.debug("Both us and the remote server support the STARTTLS feature. Encrypt and authenticate the connection with TLS & SASL...");
                        // Build identical opening stream
                        StringBuilder sb = new StringBuilder();
                        sb.append("<stream:stream");
                        sb.append(" xmlns:db=\"jabber:server:dialback\"");
                        sb.append(" xmlns:stream=\"http://etherx.jabber.org/streams\"");
                        sb.append(" xmlns=\"jabber:server\"");
                        sb.append(" from=\"").append(domainPair.getLocal()).append("\""); // OF-673
                        sb.append(" to=\"").append(domainPair.getRemote()).append("\"");
                        sb.append(" version=\"1.0\">");
                        LocalOutgoingServerSession answer = LocalOutgoingServerSession.encryptAndAuthenticate(domainPair, connection, reader, sb);
                        if (answer != null) {
                            log.debug("Successfully encrypted/authenticated the connection with TLS/SASL)!");
                            // Everything went fine so return the secured and
                            // authenticated connection
                            log.debug("Successfully created new session!");
                            return answer;
                        }
                        log.debug("Unable to encrypt and authenticate the connection with TLS & SASL.");
                    } else if (tlsPolicy == Connection.TLSPolicy.required) {
                        log.debug("I have no StartTLS yet I must TLS");
                        connection.close(new StreamError(StreamError.Condition.not_authorized, "TLS is mandatory, but was not established."));
                        return null;
                    }
                    // Check if we are going to try server dialback (XMPP 1.0)
                    else if (ServerDialback.isEnabled() && featuresElement.element("dialback") != null) {
                        log.debug("Both us and the remote server support the 'dialback' feature. Authenticate the connection with dialback...");
                        ServerDialback method = new ServerDialback(connection, domainPair);
                        OutgoingServerSocketReader newSocketReader = new OutgoingServerSocketReader(reader);
                        if (method.authenticateDomain(newSocketReader, id)) {
                            log.debug("Successfully authenticated the connection with dialback!");
                            StreamID streamID = BasicStreamIDFactory.createStreamID(id);
                            LocalOutgoingServerSession session = new LocalOutgoingServerSession(domainPair.getLocal(), connection, newSocketReader, streamID);
                            connection.init(session);
                            session.setAuthenticationMethod(ServerSession.AuthenticationMethod.DIALBACK);
                            // Set the remote domain name as the address of the session.
                            session.setAddress(new JID(null, domainPair.getRemote(), null));
                            log.debug("Successfully created new session!");
                            return session;
                        } else {
                            log.debug("Unable to authenticate the connection with dialback.");
                        }
                    }
                }
            } else {
                log.debug("Error! No data from the remote server (expected a 'feature' element).");
            }
        } else {
            log.debug("The remote server is not XMPP 1.0 compliant.");
        }

        log.debug("Something went wrong so close the connection and try server dialback over a plain connection");
        if (connection.getTlsPolicy() == Connection.TLSPolicy.required) {
            log.debug("I have no StartTLS yet I must TLS");
            connection.close(new StreamError(StreamError.Condition.not_authorized, "TLS is mandatory, but was not established."));
            return null;
        }
    }
}
