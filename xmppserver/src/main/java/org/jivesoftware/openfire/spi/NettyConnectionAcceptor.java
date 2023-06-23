package org.jivesoftware.openfire.spi;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.OrderedEventExecutor;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.jivesoftware.openfire.Connection;
import org.jivesoftware.openfire.JMXManager;
import org.jivesoftware.openfire.mbean.ThreadPoolExecutorDelegate;
import org.jivesoftware.openfire.mbean.ThreadPoolExecutorDelegateMBean;
import org.jivesoftware.openfire.net.StalledSessionsFilter;
import org.jivesoftware.openfire.nio.*;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This class is responsible for accepting new (socket) connections, using Java NIO implementation provided by the
 * Apache MINA framework.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
class NettyConnectionAcceptor extends ConnectionAcceptor
{
    private final Logger Log;
    private final String name;
    private final NettyServerConnectionHandler connectionHandler;

    private final EncryptionArtifactFactory encryptionArtifactFactory;

    private NioSocketAcceptor socketAcceptor;

    /**
     * Object name used to register delegate MBean (JMX) for the thread pool executor.
     */
    private ObjectName executorServiceObjectName;

    /**
     * Instantiates, but not starts, a new instance.
     */
    public NettyConnectionAcceptor(ConnectionConfiguration configuration )
    {
        super( configuration );

        this.name = configuration.getType().toString().toLowerCase() + ( configuration.getTlsPolicy() == Connection.TLSPolicy.legacyMode ? "_ssl" : "" );
        Log = LoggerFactory.getLogger( NettyConnectionAcceptor.class.getName() + "[" + name + "]" );

        connectionHandler = new NettyServerConnectionHandler( configuration );

//        switch ( configuration.getType() )
//        {
//             case SOCKET_S2S:
//                connectionHandler = new ServerConnectionHandler( configuration );
//                break;
//            case SOCKET_C2S:
//                connectionHandler = new ClientConnectionHandler( configuration );
//                break;
//            case COMPONENT:
//                connectionHandler = new ComponentConnectionHandler( configuration );
//                break;
//            case CONNECTION_MANAGER:
//                connectionHandler = new MultiplexerConnectionHandler( configuration );
//                break;
//            default:
//                throw new IllegalStateException( "This implementation does not support the connection type as defined in the provided configuration: " + configuration.getType() );
//        }

        this.encryptionArtifactFactory = new EncryptionArtifactFactory( configuration );
    }

    /**
     * Starts this acceptor by binding the socket acceptor. When the acceptor is already started, a warning will be
     * logged and the method invocation is otherwise ignored.
     */
    @Override
    public synchronized void start()
    {
        System.out.println("RUNNING NETTY!");

        // NioEventLoopGroup is a multithreaded event loop that handles I/O operation.
        // Netty provides various EventLoopGroup implementations for different kind of transports.
        // We are implementing a server-side application in this example, and therefore two
        // NioEventLoopGroup will be used. The first one, often called 'boss', accepts an incoming connection.
        // The second one, often called 'worker', handles the traffic of the accepted connection once the boss
        // accepts the connection and registers the accepted connection to the worker. How many Threads are
        // used and how they are mapped to the created Channels depends on the EventLoopGroup implementation
        // and may be even configurable via a constructor.
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // ServerBootstrap is a helper class that sets up a server. You can set up the server using
            // a Channel directly. However, please note that this is a tedious process, and you do not
            // need to do that in most cases.
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                // Here, we specify to use the NioServerSocketChannel class which is used to
                // instantiate a new Channel to accept incoming connections.
                .channel(NioServerSocketChannel.class)
                // The handler specified here will always be evaluated by a newly accepted Channel.
                // The ChannelInitializer is a special handler that is purposed to help a user configure
                // a new Channel. It is most likely that you want to configure the ChannelPipeline of the
                // new Channel by adding some handlers such as DiscardServerHandler to implement your
                // network application. As the application gets complicated, it is likely that you will add
                // more handlers to the pipeline and extract this anonymous class into a top-level
                // class eventually.
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(connectionHandler);
                    }
                })
                // You can also set the parameters which are specific to the Channel implementation.
                // We are writing a TCP/IP server, so we are allowed to set the socket options such as
                // tcpNoDelay and keepAlive. Please refer to the apidocs of ChannelOption and the specific
                // ChannelConfig implementations to get an overview about the supported ChannelOptions.
                .option(ChannelOption.SO_BACKLOG, 128)
                // option() is for the NioServerSocketChannel that accepts incoming connections.
                // childOption() is for the Channels accepted by the parent ServerChannel,
                // which is NioSocketChannel in this case.
                .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind to the port and start the server to accept incoming connections.
            // We bind to the port 8080 of all NICs (network interface cards) in the machine. You can now
            // call the bind() method as many times as you want (with different bind addresses.)
            ChannelFuture channelFuture = serverBootstrap.bind(new InetSocketAddress( configuration.getBindAddress(), configuration.getPort() )).sync();

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            channelFuture.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            System.err.println( "Error starting " + configuration.getPort() + ": " + e.getMessage() );
            Log.error( "Error starting: " + configuration.getPort(), e );
            // Reset for future use.
//            if (executorServiceObjectName != null) {
//                JMXManager.tryUnregister(executorServiceObjectName);
//                executorServiceObjectName = null;
//            }


        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    /**
     * Stops this acceptor by unbinding the socket acceptor. Does nothing when the instance is not started.
     */
    @Override
    public synchronized void stop()
    {
        if (executorServiceObjectName != null) {
            JMXManager.tryUnregister(executorServiceObjectName);
            executorServiceObjectName = null;
        }
        if ( socketAcceptor != null )
        {
            socketAcceptor.unbind();
            socketAcceptor = null;
        }
    }

    /**
     * Determines if this instance is currently in a state where it is actively serving connections.
     *
     * @return false when this instance is started and is currently being used to serve connections (otherwise true)
     */
    @Override
    public synchronized boolean isIdle()
    {
        return this.socketAcceptor != null && this.socketAcceptor.getManagedSessionCount() == 0;
    }

    @Override
    public synchronized void reconfigure( ConnectionConfiguration configuration )
    {
        this.configuration = configuration;

        if ( socketAcceptor == null )
        {
            return; // reconfig will occur when acceptor is started.
        }

        final DefaultIoFilterChainBuilder filterChain = socketAcceptor.getFilterChain();

        if ( filterChain.contains( ConnectionManagerImpl.EXECUTOR_FILTER_NAME ) )
        {
            final ExecutorFilter executorFilter = (ExecutorFilter) filterChain.get( ConnectionManagerImpl.EXECUTOR_FILTER_NAME );
            ( (ThreadPoolExecutor) executorFilter.getExecutor()).setCorePoolSize( ( configuration.getMaxThreadPoolSize() / 4 ) + 1 );
            ( (ThreadPoolExecutor) executorFilter.getExecutor()).setMaximumPoolSize( ( configuration.getMaxThreadPoolSize() ) );
        }

        if ( configuration.getTlsPolicy() == Connection.TLSPolicy.legacyMode )
        {
            // add or replace TLS filter (that's used only for 'direct-TLS')
            try
            {
                final SslFilter sslFilter = encryptionArtifactFactory.createServerModeSslFilter();
                if ( filterChain.contains( ConnectionManagerImpl.TLS_FILTER_NAME ) )
                {
                    filterChain.replace( ConnectionManagerImpl.TLS_FILTER_NAME, sslFilter );
                }
                else
                {
                    filterChain.addAfter( ConnectionManagerImpl.EXECUTOR_FILTER_NAME, ConnectionManagerImpl.TLS_FILTER_NAME, sslFilter );
                }
            }
            catch ( KeyManagementException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e )
            {
                Log.error( "An exception occurred while reloading the TLS configuration.", e );
            }
        }
        else
        {
            // The acceptor is in 'startTLS' mode. Remove TLS filter (that's used only for 'direct-TLS')
            if ( filterChain.contains( ConnectionManagerImpl.TLS_FILTER_NAME ) )
            {
                filterChain.remove( ConnectionManagerImpl.TLS_FILTER_NAME );
            }
        }

        if ( configuration.getMaxBufferSize() > 0 )
        {
            socketAcceptor.getSessionConfig().setMaxReadBufferSize( configuration.getMaxBufferSize() );
            Log.debug( "Throttling read buffer for connections to max={} bytes", configuration.getMaxBufferSize() );
        }
    }

    public synchronized int getPort()
    {
        return configuration.getPort();
    }

    // TODO see if we can avoid exposing MINA internals.
    public synchronized NioSocketAcceptor getSocketAcceptor()
    {
        return socketAcceptor;
    }

    private static NioSocketAcceptor buildSocketAcceptor()
    {
        // Create SocketAcceptor with correct number of processors
        final int processorCount = JiveGlobals.getIntProperty( "xmpp.processor.count", Runtime.getRuntime().availableProcessors() );

        final NioSocketAcceptor socketAcceptor = new NioSocketAcceptor( processorCount );

        // Set that it will be possible to bind a socket if there is a connection in the timeout state.
        socketAcceptor.setReuseAddress( true );

        // Set the listen backlog (queue) length. Default is 50.
        socketAcceptor.setBacklog( JiveGlobals.getIntProperty( "xmpp.socket.backlog", 50 ) );

        // Set default (low level) settings for new socket connections
        final SocketSessionConfig socketSessionConfig = socketAcceptor.getSessionConfig();

        //socketSessionConfig.setKeepAlive();
        final int receiveBuffer = JiveGlobals.getIntProperty( "xmpp.socket.buffer.receive", -1 );
        if ( receiveBuffer > 0 )
        {
            socketSessionConfig.setReceiveBufferSize( receiveBuffer );
        }

        final int sendBuffer = JiveGlobals.getIntProperty( "xmpp.socket.buffer.send", -1 );
        if ( sendBuffer > 0 )
        {
            socketSessionConfig.setSendBufferSize( sendBuffer );
        }

        final int linger = JiveGlobals.getIntProperty( "xmpp.socket.linger", -1 );
        if ( linger > 0 )
        {
            socketSessionConfig.setSoLinger( linger );
        }

        socketSessionConfig.setTcpNoDelay( JiveGlobals.getBooleanProperty( "xmpp.socket.tcp-nodelay", socketSessionConfig.isTcpNoDelay() ) );

        return socketAcceptor;
    }
}
