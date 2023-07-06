package org.jivesoftware.openfire.nio;

import io.netty.channel.ChannelHandlerContext;
import org.jivesoftware.openfire.*;
import org.jivesoftware.openfire.net.*;
import org.jivesoftware.openfire.session.*;
import org.jivesoftware.openfire.spi.ConnectionConfiguration;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class NettyOutboundConnectionHandler extends NettyConnectionHandler {
    private static final Logger log = LoggerFactory.getLogger(NettyOutboundConnectionHandler.class);
    private final DomainPair domainPair;

    public NettyOutboundConnectionHandler(ConnectionConfiguration configuration, DomainPair domainPair) {
        super(configuration);
        this.domainPair = domainPair;
    }

    NettyConnection createNettyConnection(ChannelHandlerContext ctx) {
        return new NettyConnection(ctx, null, configuration);
    }

    @Override
    StanzaHandler createStanzaHandler(NettyConnection connection) {
        return new RespondingServerStanzaHandler( XMPPServer.getInstance().getPacketRouter(), connection, domainPair );
    }

    @Override
    int getMaxIdleTime() {
        return 0;
    }
//
//    @Override
//    public void channelReadLESSOLD(ChannelHandlerContext ctx, String msg) {
//
//        NettyConnection connection = ctx.channel().attr(CONNECTION).get();
//
//        boolean directTLS = false; // TODO
//        try {
//            if (directTLS) {
//                try {
//                    connection.startTLS( true, true );
//                } catch ( SSLException ex ) {
//                    if ( JiveGlobals.getBooleanProperty(ConnectionSettings.Server.TLS_ON_PLAIN_DETECTION_ALLOW_NONDIRECTTLS_FALLBACK, true) && ex.getMessage().contains( "plaintext connection?" ) ) {
//                        //log.warn( "Plaintext detected on a new connection that is was started in DirectTLS mode (socket address: {}). Attempting to restart the connection in non-DirectTLS mode.", socketAddress );
//                        try {
//                            // Close old socket
////                            socket.close();
//                        } catch ( Exception e ) {
//                            log.debug( "An exception occurred (and is ignored) while trying to close a socket that was already in an error state.", e );
//                        }
//                        //socket = new Socket();
//                        //socket.connect( socketAddress, RemoteServerManager.getSocketTimeout() );
//                        //connection = new SocketConnection(XMPPServer.getInstance().getPacketDeliverer(), socket, false);
//                        directTLS = false;
//                        //log.info( "Re-established connection to {}. Proceeding without directTLS.", socketAddress );
//                    } else {
//                        // Do not retry as non-DirectTLS, rethrow the exception.
//                        throw ex;
//                    }
//                }
//            }
//
//            // Duplicate stream start stanza TODO: this is also done in LocalOutgoingServerSession...
//            StringBuilder sb = new StringBuilder();
//            sb.append("<stream:stream");
//            sb.append(" xmlns:db=\"jabber:server:dialback\"");
//            sb.append(" xmlns:stream=\"http://etherx.jabber.org/streams\"");
//            sb.append(" xmlns=\"jabber:server\"");
//            sb.append(" from=\"").append(domainPair.getLocal()).append("\""); // OF-673
//            sb.append(" to=\"").append(domainPair.getRemote()).append("\"");
//            sb.append(" version=\"1.0\">");
//
//            // TODO something with socket timeouts in netty (also see restoration of this setting below)
//            // Set a read timeout (of 5 seconds) so we don't keep waiting forever
//            // int soTimeout = socket.getSoTimeout();
//            // socket.setSoTimeout(5000);
//
//            XMPPPacketReader reader = new XMPPPacketReader();
//
//            reader.getXPPParser().setInput(new InputStreamReader( new ByteArrayInputStream(msg.getBytes()), StandardCharsets.UTF_8 ));
//
//            // Get the answer from the Receiving Server
//            XmlPullParser xpp = reader.getXPPParser();
//            for (int eventType = xpp.getEventType(); eventType != XmlPullParser.START_TAG;) {
//                eventType = xpp.next();
//            }
//
//            String serverVersion = xpp.getAttributeValue("", "version");
//            String id = xpp.getAttributeValue("", "id");
//            log.debug( "Got a response (stream ID: {}, version: {}). Check if the remote server is XMPP 1.0 compliant...", id, serverVersion );
//
//            if (serverVersion != null && Session.decodeVersion(serverVersion)[0] >= 1) {
//                log.debug( "The remote server is XMPP 1.0 compliant (or at least reports to be)." );
//
//                // TODO something with socket timeouts in netty (also see initial setting of this above)
//                // Restore default timeout
//                // socket.setSoTimeout(soTimeout);
//
//                Element features = reader.parseDocument().getRootElement();
//                if (features != null) {
//                    log.debug( "Processing stream features of the remote domain: {}", features.asXML() );
//                    if (directTLS) {
////                        log.debug( "We connected to the remote server using direct TLS. Authenticate the connection with SASL..." );
////                        LocalOutgoingServerSession answer = authenticate(domainPair, connection, reader, sb, features, id);
////                        if (answer != null) {
////                            log.debug( "Successfully authenticated the connection with SASL)!" );
////                            // Everything went fine so return the encrypted and authenticated connection.
////                            log.debug( "Successfully created new session!" );
////                            return answer;
////                        }
////                        log.debug( "Unable to authenticate the connection with SASL." );
//                    } else {
//                        log.debug( "Check if both us as well as the remote server have enabled STARTTLS and/or dialback ..." );
//                        final boolean useTLS = connection.getTlsPolicy() == Connection.TLSPolicy.optional || connection.getTlsPolicy() == Connection.TLSPolicy.required;
//                        if (useTLS && features.element("starttls") != null) {
//                            log.debug( "Both us and the remote server support the STARTTLS feature. Encrypt and authenticate the connection with TLS & SASL..." );
//                            LocalOutgoingServerSession answer = encryptAndAuthenticate(domainPair, connection, reader, sb); // TODO - DONE
//                            if (answer != null) {
//                                log.debug( "Successfully encrypted/authenticated the connection with TLS/SASL)!" );
//                                // Everything went fine so return the secured and
//                                // authenticated connection
//                                log.debug( "Successfully created new session!" );
//                                return answer;
//                            }
//                            log.debug( "Unable to encrypt and authenticate the connection with TLS & SASL." );
//                        }
//                        else if (connection.getTlsPolicy() == Connection.TLSPolicy.required) {
//                            log.debug("I have no StartTLS yet I must TLS");
//                            connection.close(new StreamError(StreamError.Condition.not_authorized, "TLS is mandatory, but was not established."));
//                            return null;
//                        }
//                        // Check if we are going to try server dialback (XMPP 1.0)
//                        else if (ServerDialback.isEnabled() && features.element("dialback") != null) {
//                            log.debug( "Both us and the remote server support the 'dialback' feature. Authenticate the connection with dialback..." );
//                            ServerDialback method = new ServerDialback(connection, domainPair);
//                            OutgoingServerSocketReader newSocketReader = new OutgoingServerSocketReader(reader);
//                            if (method.authenticateDomain(newSocketReader, id)) {
//                                log.debug( "Successfully authenticated the connection with dialback!" );
//                                StreamID streamID = BasicStreamIDFactory.createStreamID(id);
//                                LocalOutgoingServerSession session = new LocalOutgoingServerSession(domainPair.getLocal(), connection, newSocketReader, streamID);
//                                connection.init(session);
//                                session.setAuthenticationMethod(ServerSession.AuthenticationMethod.DIALBACK);
//                                // Set the remote domain name as the address of the session.
//                                session.setAddress(new JID(null, domainPair.getRemote(), null));
//                                log.debug( "Successfully created new session!" );
//                                return session;
//                            }
//                            else {
//                                log.debug( "Unable to authenticate the connection with dialback." );
//                            }
//                        }
//                    }
//                }
//                else {
//                    log.debug( "Error! No data from the remote server (expected a 'feature' element).");
//                }
//            } else {
//                log.debug( "The remote server is not XMPP 1.0 compliant." );
//            }
//
//            log.debug( "Something went wrong so close the connection and try server dialback over a plain connection" );
//            if (connection.getTlsPolicy() == Connection.TLSPolicy.required) {
//                log.debug("I have no StartTLS yet I must TLS");
//                connection.close(new StreamError(StreamError.Condition.not_authorized, "TLS is mandatory, but was not established."));
//                return null;
//            }
//            connection.close();
//        }
//        catch (SSLHandshakeException e)
//        {
//            // When not doing direct TLS but startTLS, this a failure as described in RFC3620, section 5.4.3.2 "STARTTLS Failure".
//            log.info( "{} negotiation failed. Closing connection (without sending any data such as <failure/> or </stream>).", (directTLS ? "Direct TLS" : "StartTLS" ), e );
//
//            // The receiving entity is expected to close the socket *without* sending any more data (<failure/> nor </stream>).
//            // It is probably (see OF-794) best if we, as the initiating entity, therefor don't send any data either.
//            if (connection != null) {
//                connection.forceClose();
//            }
//        }
//        catch (Exception e)
//        {
//            // This might be RFC3620, section 5.4.2.2 "Failure Case" or even an unrelated problem. Handle 'normally'.
//            log.warn( "An exception occurred while creating an encrypted session. Closing connection.", e );
//
//            if (connection != null) {
//                connection.close();
//            }
//        }
//
//        if (ServerDialback.isEnabled())
//        {
//            log.debug( "Unable to create a new session. Going to try connecting using server dialback as a fallback." );
//
//            // Use server dialback (pre XMPP 1.0) over a plain connection
//            final LocalOutgoingServerSession outgoingSession = new ServerDialback(domainPair).createOutgoingSession(port);
//            if ( outgoingSession != null) { // TODO this success handler behaves differently from a similar success handler above. Shouldn't those be the same?
//                log.debug( "Successfully created new session (using dialback as a fallback)!" );
//                return outgoingSession;
//            } else {
//                log.warn( "Unable to create a new session: Dialback (as a fallback) failed." );
//                return null;
//            }
//        }
//        else
//        {
//            log.warn( "Unable to create a new session: exhausted all options (not trying dialback as a fallback, as server dialback is disabled by configuration." );
//            return null;
//        }
//    }
//
//
//
//    private static LocalOutgoingServerSession secureAndAuthenticate(DomainPair domainPair, NettyConnection connection, XMPPPacketReader reader, StringBuilder openingStream) throws Exception {
//        Element features;
//
//        log.debug( "Securing and authenticating connection ...");
//
//        log.debug( "Indicating we want TLS and wait for response." );
//        connection.deliverRawText( "<starttls xmlns='urn:ietf:params:xml:ns:xmpp-tls'/>" );
//
//        MXParser xpp = reader.getXPPParser(); // TODO - proceed needs to be handled separately to starttls, we can't sit and wait
//        // Wait for the <proceed> response
//        Element proceed = reader.parseDocument().getRootElement();
//        if (proceed != null && proceed.getName().equals("proceed")) {
//            log.debug( "Received 'proceed' from remote server. Negotiating TLS..." );
//            try {
////                boolean needed = JiveGlobals.getBooleanProperty(ConnectionSettings.Server.TLS_CERTIFICATE_VERIFY, true) &&
////                        		 JiveGlobals.getBooleanProperty(ConnectionSettings.Server.TLS_CERTIFICATE_CHAIN_VERIFY, true) &&
////                        		 !JiveGlobals.getBooleanProperty(ConnectionSettings.Server.TLS_ACCEPT_SELFSIGNED_CERTS, false);
//                connection.startTLS(true, false);
//            } catch(Exception e) {
//                log.debug("TLS negotiation failed: " + e.getMessage());
//                throw e;
//            }
//            log.debug( "TLS negotiation was successful. Connection secured. Proceeding with authentication..." );
//            if (!SASLAuthentication.verifyCertificates(connection.getPeerCertificates(), domainPair.getRemote(), true)) {
//                if (ServerDialback.isEnabled() || ServerDialback.isEnabledForSelfSigned()) {
//                    log.debug( "SASL authentication failed. Will continue with dialback." );
//                } else {
//                    log.warn( "Unable to authenticated the connection: SASL authentication failed (and dialback is not available)." );
//                    return null;
//                }
//            }
//
//            log.debug( "TLS negotiation was successful so initiate a new stream." );
//            connection.deliverRawText( openingStream.toString() );
//
//            // Reset the parser to use the new secured reader
//            xpp.setInput(new InputStreamReader(connection.getTLSStreamHandler().getInputStream(), StandardCharsets.UTF_8));
//            // Skip new stream element
//            for (int eventType = xpp.getEventType(); eventType != XmlPullParser.START_TAG;) {
//                eventType = xpp.next();
//            }
//            // Get the stream ID
//            String id = xpp.getAttributeValue("", "id");
//            // Get new stream features
//            features = reader.parseDocument().getRootElement();
//            if (features != null) {
//                return authenticate( domainPair, connection, reader, openingStream, features, id );
//            }
//            else {
//                log.debug( "Failed to secure and authenticate connection: neither SASL mechanisms nor SERVER DIALBACK were offered by the remote host." );
//                return null;
//            }
//        }
//        else {
//            log.debug( "Failed to secure and authenticate connection: <proceed> was not received!" );
//            return null;
//        }
//    }
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//    public void channelReadOLD(ChannelHandlerContext ctx, String msg) throws DocumentException {
//        System.out.println(msg); //TODO
//        Document doc = new XMPPPacketReader().parseDocument(msg);
////      // TODO: Move this to the input handler vvvvvvvvvvv
////      final InputStream inputStream;
////        if (directTLS) {
////            throw new Exception("directTls not yet implemented!");
////          inputStream = connection.getTLSStreamHandler().getInputStream();
////        }
////       else {
////       inputStream = socket.getInputStream();}
////       reader.getXPPParser().setInput(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
////
////       // Get the answer from the Receiving Server
////       XmlPullParser xpp = reader.getXPPParser();
////       for (int eventType = xpp.getEventType(); eventType != XmlPullParser.START_TAG; ) {
////          eventType = xpp.next();
////       }
////       // TODO: Move this to the input handler ^^^^^^^^^^
//
//        Element rootStreamElement = doc.getRootElement(); //TODO: Check this is the stream tag
//        String serverVersion = rootStreamElement.attribute("version").getText();
//        String id = rootStreamElement.attribute("id").getText();
////      String serverVersion = xpp.getAttributeValue("", "version");
////      String id = xpp.getAttributeValue("", "id");
//        log.debug("Got a response (stream ID: {}, version: {}). Check if the remote server is XMPP 1.0 compliant...", id, serverVersion);
//
//        if (serverVersion != null && Session.decodeVersion(serverVersion)[0] >= 1) {
//            log.debug("The remote server is XMPP 1.0 compliant (or at least reports to be).");
//            // Restore default timeout
////          socket.setSoTimeout(soTimeout); //TODO - figure this out in Netty
//
//            Element featuresElement = rootStreamElement.element("features");
////          reader.parseDocument().getRootElement();
//            if (featuresElement != null) {
//                log.debug("Processing stream features of the remote domain: {}", featuresElement.asXML());
//                if (directTLS) {
//                    throw new Exception("directTls not yet implemented!");
//                    log.debug("We connected to the remote server using direct TLS. Authenticate the connection with SASL...");
//                    LocalOutgoingServerSession answer = authenticate(domainPair, connection, reader, openingStream, featuresElement, id);
//                    if (answer != null) {
//                        log.debug("Successfully authenticated the connection with SASL)!");
//                        // Everything went fine so return the encrypted and authenticated connection.
//                        log.debug("Successfully created new session!");
//                        return answer;
//                    }
//                    log.debug("Unable to authenticate the connection with SASL.");
//                } else {
//                    log.debug("Check if both us as well as the remote server have enabled STARTTLS and/or dialback ...");
//
//                    Connection connection = ctx.channel().attr(CONNECTION).get();
//
//                    final boolean useTLS = configuration.getTlsPolicy() == Connection.TLSPolicy.optional || configuration.getTlsPolicy() == Connection.TLSPolicy.required;
//                    if (useTLS && featuresElement.element("starttls") != null) {
//                        log.debug("Both us and the remote server support the STARTTLS feature. Encrypt and authenticate the connection with TLS & SASL...");
//                        // Build identical opening stream
//                        StringBuilder sb = new StringBuilder();
//                        sb.append("<stream:stream");
//                        sb.append(" xmlns:db=\"jabber:server:dialback\"");
//                        sb.append(" xmlns:stream=\"http://etherx.jabber.org/streams\"");
//                        sb.append(" xmlns=\"jabber:server\"");
//                        sb.append(" from=\"").append(domainPair.getLocal()).append("\""); // OF-673
//                        sb.append(" to=\"").append(domainPair.getRemote()).append("\"");
//                        sb.append(" version=\"1.0\">");
//                        LocalOutgoingServerSession answer = LocalOutgoingServerSession.encryptAndAuthenticate(domainPair, connection, reader, sb);
//                        if (answer != null) {
//                            log.debug("Successfully encrypted/authenticated the connection with TLS/SASL)!");
//                            // Everything went fine so return the secured and
//                            // authenticated connection
//                            log.debug("Successfully created new session!");
//                            return answer;
//                        }
//                        log.debug("Unable to encrypt and authenticate the connection with TLS & SASL.");
//                } else if (tlsPolicy == Connection.TLSPolicy.required) {
//                    log.debug("I have no StartTLS yet I must TLS");
//                    connection.close(new StreamError(StreamError.Condition.not_authorized, "TLS is mandatory, but was not established."));
//                    return null;
//                }
//                // Check if we are going to try server dialback (XMPP 1.0)
//                else if (ServerDialback.isEnabled() && featuresElement.element("dialback") != null) {
//                    log.debug("Both us and the remote server support the 'dialback' feature. Authenticate the connection with dialback...");
//                    ServerDialback method = new ServerDialback(connection, domainPair);
//                    OutgoingServerSocketReader newSocketReader = new OutgoingServerSocketReader(reader);
//                    if (method.authenticateDomain(newSocketReader, id)) {
//                        log.debug("Successfully authenticated the connection with dialback!");
//                        StreamID streamID = BasicStreamIDFactory.createStreamID(id);
//                        LocalOutgoingServerSession session = new LocalOutgoingServerSession(domainPair.getLocal(), connection, newSocketReader, streamID);
//                        connection.init(session);
//                        session.setAuthenticationMethod(ServerSession.AuthenticationMethod.DIALBACK);
//                        // Set the remote domain name as the address of the session.
//                        session.setAddress(new JID(null, domainPair.getRemote(), null));
//                        log.debug("Successfully created new session!");
//                        return session;
//                    } else {
//                        log.debug("Unable to authenticate the connection with dialback.");
//                    }
//                }
//            }
//        } else {
//            log.debug("Error! No data from the remote server (expected a 'feature' element).");
//        }
//    }
//}
}
