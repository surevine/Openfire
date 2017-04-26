package org.jivesoftware.openfire.mix;

import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.database.ConnectionProvider;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.database.DefaultConnectionProvider;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.BasicModule;
import org.jivesoftware.openfire.mix.handler.channel.*;
import org.jivesoftware.openfire.mix.handler.service.*;
import org.jivesoftware.openfire.mix.mam.MessageArchiveService;
import org.jivesoftware.openfire.mix.mam.MessageArchiveServiceImpl;
import org.jivesoftware.openfire.mix.mam.repository.JpaMixChannelArchiveRepositoryImpl;
import org.jivesoftware.openfire.mix.mam.repository.MamQueryFactory;
import org.jivesoftware.openfire.mix.mam.repository.MixChannelArchiveRepository;
import org.jivesoftware.openfire.mix.mam.repository.QueryFactory;
import org.jivesoftware.openfire.mix.repository.MixIdentityManager;
import org.jivesoftware.openfire.mix.repository.MixPersistenceManagerImpl;
import org.jivesoftware.util.JiveProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManagerFactory;

public class MixManager extends BasicModule {
	private static final Logger Log = LoggerFactory.getLogger(MixManager.class);

	public static final String MIX_VERSION = "0.6.1";
	
	public static final String MIX_NAMESPACE = "urn:xmpp:mix:0";
	
	private XMPPServer xmppServer;
	
	private MixPersistenceManager persistenceManager;
	
	private ConcurrentHashMap<String, MixService> mixServices = new ConcurrentHashMap<>();
	
	// Keep these statics here, rather than update the static collection in sequence manager
	private static final int CHANNEL_SEQ_TYPE = 500;
	private static final int MCP_SEQ_TYPE = 501;
	private static final int MCP_SUBS_SEQ_TYPE = 502;
	
    /**
     * Creates a new MixManager instance.
     */
    public MixManager() {
        super("Mediated Information eXchange (MIX) manager");

        this.xmppServer = XMPPServer.getInstance();

        PacketRouter router = XMPPServer.getInstance().getPacketRouter();
    	
    	List<MixServicePacketHandler> serviceHandlers = Arrays.asList(
    			new DiscoMixServicePacketHandler(xmppServer),
    			new MixServiceChannelCreatePacketHandler(),
    			new DestroyMixChannelPacketHandler(),
				new MamQueryPacketHandler()
    		);
    	
    	List<MixChannelPacketHandler> channelHandlers = Arrays.asList(
				new MixChannelJoinPacketHandler(),
    			new MixChannelJoinStatusPacketHandler(),
    			new MixChannelMessagePacketHandler(router),
    			new MixChannelLeavePacketHandler(),
    			new MixChannelNodeItemsGetPacketHandler(),
    			new MixChannelUpdateSubscriptionPacketHandler(),
				new MixChannelMamQueryPacketHandler()
    		);
    	
    	MixXmppServiceImpl xmppService = new MixXmppServiceImpl(router, serviceHandlers, channelHandlers);

		MixChannelArchiveRepository mar = new JpaMixChannelArchiveRepositoryImpl("mam", this.getDbConfig());
		QueryFactory queryFactory = new MamQueryFactory();
		MessageArchiveService archive = new MessageArchiveServiceImpl(mar, router, queryFactory);
    	
    	this.persistenceManager = new MixPersistenceManagerImpl(JiveProperties.getInstance(), xmppService, 
    			new MixIdentityManager(CHANNEL_SEQ_TYPE, 5), new MixIdentityManager(MCP_SEQ_TYPE, 5), new MixIdentityManager(MCP_SUBS_SEQ_TYPE, 5), archive);
    }


    public MixManager(XMPPServer xmppServer, MixPersistenceManager persistenceManager) {
        super("Mediated Information eXchange (MIX) manager");

        this.xmppServer = xmppServer;
    	this.persistenceManager = persistenceManager;
    }

	private Map<String, String> getDbConfig() {
		Map<String, String> config = new HashMap<String, String>();
		try {
			DefaultConnectionProvider connectionProvider = (DefaultConnectionProvider)DbConnectionManager.getConnectionProvider();
			config.put("javax.persistence.jdbc.driver", connectionProvider.getDriver());
			config.put("javax.persistence.jdbc.url", connectionProvider.getServerURL());
			config.put("javax.persistence.jdbc.user", connectionProvider.getUsername());
			config.put("javax.persistence.jdbc.password", connectionProvider.getPassword());
			return config;
		} catch (Exception e) {
			return config;
		}
	}

	/**
	 * Called when manager starts up, to initialize things.
	 */
	@Override
	public void start() {
		super.start();

		try {
			Collection<MixService> services = persistenceManager.loadServices(xmppServer);
			
			for (MixService service : services) {
				mixServices.put(service.getName(), service);
				
				registerMixService(service);
			}
		} catch(MixPersistenceException e) {
			Log.error("Error occurred while loading MIX Services", e);
		}
	}

	/**
	 * Called when manager is stopped, to clean things up.
	 */
	@Override
	public void stop() {
		super.stop();
		for (MixService service : mixServices.values()) {
			unregisterMixService(service.getServiceName());
		}
	}

    /**
     * Registers a new {@link MixService} implementation to the manager.
     * This is typically used if you have a custom MUC implementation that you
     * want to register with the manager.  In other words, it may not be database
     * stored and may follow special rules, implementating {@link MixService}.
     * It is also used internally to register services from the database.  Triggers
     * the service to start up.
     *
     * @param service The MixService to be registered.
     */
    public void registerMixService(MixService service) {
        Log.debug("MixManager: Registering MIX service "+service.getServiceName());
        try {
            ComponentManagerFactory.getComponentManager().addComponent(service.getServiceName(), service);
            mixServices.put(service.getServiceName(), service);
        }
        catch (ComponentException e) {
            Log.error("MixManager: Unable to add "+service.getServiceName()+" as component.", e);
        }
    }

    public MixService getMixService(String subdomain) {
		MixService service = mixServices.get(subdomain);
		return service;
	}

    /**
     * Unregisters a MultiUserChatService from the manager.  It can be used
     * to explicitly unregister services, and is also used internally to unregister
     * database stored services.  Triggers the service to shut down.
     *
     * @param subdomain The subdomain of the service to be unregistered.
     */
    public void unregisterMixService(String subdomain) {
        Log.debug("MixManager: Unregistering MIX service "+subdomain);
        MixService service = mixServices.get(subdomain);
        if (service != null) {
            service.shutdown();
            try {
                ComponentManagerFactory.getComponentManager().removeComponent(subdomain);
            }
            catch (ComponentException e) {
                Log.error("MixManager: Unable to remove "+subdomain+" from component manager.", e);
            }
            mixServices.remove(subdomain);
        }
    }
}