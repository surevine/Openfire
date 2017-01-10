package org.jivesoftware.openfire.mix;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.BasicModule;
import org.jivesoftware.util.JiveProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManagerFactory;

public class MixManager extends BasicModule {
	private static final Logger Log = LoggerFactory.getLogger(MixManager.class);

	public static final String MIX_VERSION = "0.6.1";
	
	private XMPPServer xmppServer;
	
	private MixPersistenceManager persistenceManager;
	
	private ConcurrentHashMap<String, MixService> mixServices = new ConcurrentHashMap<>();

	
    /**
     * Creates a new MultiUserChatManager instance.
     */
    public MixManager() {
    	this(XMPPServer.getInstance(), new MixPersistenceManagerImpl(JiveProperties.getInstance(), XMPPServer.getInstance().getPacketRouter()));
    }
    
    public MixManager(XMPPServer xmppServer, MixPersistenceManager persistenceManager) {
        super("Mediated Information eXchange (MIX) manager");

        this.xmppServer = xmppServer;
    	this.persistenceManager = persistenceManager;
    	
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
