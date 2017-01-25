package org.jivesoftware.openfire.mix.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.XMPPServerListener;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.disco.DiscoInfoProvider;
import org.jivesoftware.openfire.disco.DiscoItem;
import org.jivesoftware.openfire.disco.DiscoItemsProvider;
import org.jivesoftware.openfire.disco.DiscoServerItem;
import org.jivesoftware.openfire.disco.ServerItemsProvider;
import org.jivesoftware.openfire.mix.MixChannelNode;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.MixPersistenceManager;
import org.jivesoftware.openfire.mix.MixService;
import org.jivesoftware.openfire.mix.MixXmppService;
import org.jivesoftware.openfire.mix.exception.CannotCreateMixChannelException;
import org.jivesoftware.openfire.mix.exception.CannotDestroyMixChannelException;
import org.jivesoftware.openfire.mix.exception.MixChannelAlreadyExistsException;
import org.jivesoftware.util.JiveProperties;
import org.jivesoftware.util.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.component.Component;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;
import org.xmpp.forms.DataForm;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;

public class LocalMixService implements Component, MixService, ServerItemsProvider, DiscoInfoProvider,
		DiscoItemsProvider, XMPPServerListener {

	private static final Logger Log = LoggerFactory.getLogger(LocalMixService.class);

	private final XMPPServer xmppServer;

	private JiveProperties jiveProperties;

	private MixPersistenceManager persistenceManager;
	
	private MixXmppService xmppService;

	/**
	 * The ID of the service in the database
	 */
	private Long id;

	/**
	 * the chat service's hostname (subdomain)
	 */
	private final String serviceName;

	/**
	 * the chat service's description
	 */
	private String serviceDescription = null;

	/**
	 * Flag that indicates if MIX service is enabled.
	 */
	private boolean serviceEnabled = true;

	private Map<String, MixChannel> channels;

	/**
	 * Create a new group chat server.
	 * @param mixPersistenceManagerImpl 
	 *
	 * @param subdomain
	 *            Subdomain portion of the conference services (for example,
	 *            conference for conference.example.org)
	 * @param description
	 *            Short description of service for disco and such. If
	 *            <tt>null</tt> or empty, a default value will be used.
	 * @param mixPersistenceManagerImpl 
	 * @param isHidden
	 *            True if this service should be hidden from services views.
	 * @throws IllegalArgumentException
	 *             if the provided subdomain is an invalid, according to the JID
	 *             domain definition.
	 */
	public LocalMixService(XMPPServer xmppServer, JiveProperties jiveProperties, String subdomain, String description, MixXmppService xmppService, MixPersistenceManager mixPersistenceManagerImpl) {
		this.xmppServer = xmppServer;
		this.jiveProperties = jiveProperties;
		this.xmppService = xmppService;
		this.persistenceManager = mixPersistenceManagerImpl;

		channels = new HashMap<>();

		// Check subdomain and throw an IllegalArgumentException if its invalid
		new JID(null, subdomain + "." + xmppServer.getServerInfo().getXMPPDomain(), null);

		this.serviceName = subdomain;
		if (description != null && description.trim().length() > 0) {
			this.serviceDescription = description;
		} else {
			this.serviceDescription = LocaleUtils.getLocalizedString("mix.service-name");
		}
	}

	public String getDescription() {
		return serviceDescription;
	}

	public String getName() {
		return serviceName;
	}

	public void initialize(JID jid, ComponentManager componentManager) throws ComponentException {
		initializeSettings();
	}

	public void initializeSettings() {
		serviceEnabled = jiveProperties.getBooleanProperty("xmpp.mix.enabled", true);
	}

	public void processPacket(Packet packet) {
		xmppService.processReceivedPacket(this, packet);
	}

	public void shutdown() {
		// TODO Auto-generated method stub

	}

	public void start() {
		xmppServer.addServerListener(this);

		// Set us up to answer disco item requests
		xmppServer.getIQDiscoItemsHandler().addServerItemsProvider(this);
		xmppServer.getIQDiscoInfoHandler().setServerNodeInfoProvider(this.getServiceDomain(), this);

		// Load all the persistent rooms to memory
		try {
			for (MixChannel channel : persistenceManager.loadChannels(this)) {
				channels.put(channel.getName().toLowerCase(), channel);
			}
		} catch (MixPersistenceException e) {
			Log.error("Could not load MIX Channels for service " + getServiceDomain(), e);
		}
	}

	public String getServiceDomain() {
		return serviceName + "." + xmppServer.getServerInfo().getXMPPDomain();
	}

	public String getServiceName() {
		return serviceName;
	}

	public void serverStarted() {
		// TODO Auto-generated method stub

	}

	public void serverStopping() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isServiceEnabled() {
		return serviceEnabled;
	}

	public Iterator<DiscoItem> getItems(String name, String node, JID senderJID) {
		// Check if the service is disabled. Info is not available when
		// disabled.
		if (!isServiceEnabled()) {
			return null;
		}
		Set<DiscoItem> answer = new HashSet<>();
		if (name == null && node == null) {
			// Answer all the public rooms as items
			for (MixChannel channel : channels.values()) {
				answer.add(new DiscoItem(channel.getJID(), channel.getName(), null, null));
			}
		}
        else if (name != null && node == null) {
            MixChannel channel = channels.get(name);
            
            if(channel == null) {
            	return null;
            }
            
            for(MixChannelNode channelNode : channel.getNodes()) {
            	answer.add(new DiscoItem(channel.getJID(),
    					null, channelNode.getName(), null));
            }
        }
        return answer.iterator();
	}

	public Iterator<Element> getIdentities(String name, String node, JID senderJID) {
		if (!serviceEnabled) {
			return null;
		}

		ArrayList<Element> identities = new ArrayList<>();

		if (name == null && node == null) {
			// Answer the identity of the MUC service
			Element identity = DocumentHelper.createElement("identity");
			identity.addAttribute("category", "conference");
			identity.addAttribute("name", getDescription());
			identity.addAttribute("type", "text");
			identities.add(identity);
		} else if (name != null && node == null) {
			// Answer the identity of a given channel
			/*
			 * <identity category='conference' name='A Dark Cave' type='mix'/>
			 */
			MixChannel channel = channels.get(name);

			if (channel == null) {
				return null;
			}

			Element identity = DocumentHelper.createElement("identity");
			identity.addAttribute("category", "conference");
			identity.addAttribute("name", channel.getName());
			identity.addAttribute("type", "mix");
			identities.add(identity);
		}
		return identities.iterator();
	}

	public Iterator<String> getFeatures(String name, String node, JID senderJID) {
		ArrayList<String> features = new ArrayList<>();
		if (name == null && node == null) {
			// Answer the features of the MIX service
			features.add("urn:xmpp:mix:0");

			// "searchable"
			// "create-channel"
		} else if (name != null && node == null) {
			// TODO Maybe move this into MixChannel ?
			features.add("urn:xmpp:mix:0");
			// TODO The spec states that a MIX channel MUST support MAM, so send
			// a MAM identity too once that's implemented.
			// features.add("urn:xmpp:mam:1");
		}
		return features.iterator();
	}

	public DataForm getExtendedInfo(String name, String node, JID senderJID) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean hasInfo(String name, String node, JID senderJID) {
		// Check if the service is disabled. Info is not available when
		// disabled.
		if (!isServiceEnabled()) {
			return false;
		}
		if (name == null && node == null) {
			// We always have info about the MIX service
			return true;
		} else if (name != null && node == null) {
			// If we have a channel then we have info
			return (channels.containsKey(name));
		}
		return false;
	}

	public Iterator<DiscoServerItem> getItems() {
		// Check if the service is disabled. Info is not available when
		// disabled.
		if (!isServiceEnabled()) {
			return null;
		}

		final ArrayList<DiscoServerItem> items = new ArrayList<>();
		final DiscoServerItem item = new DiscoServerItem(new JID(getServiceDomain()), getDescription(), null, null,
				this, this);
		items.add(item);
		return items.iterator();
	}

	@Override
	public Collection<MixChannel> getChannels() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public MixChannel createChannel(JID owner, String name) throws MixChannelAlreadyExistsException, CannotCreateMixChannelException {
		if(channels.containsKey(name)) {
			throw new MixChannelAlreadyExistsException(name);
		}
		
		MixChannel newChannel = new LocalMixChannel(this, name, owner, xmppService, persistenceManager);
		try {
			newChannel = persistenceManager.save(newChannel);
			// Need to delay adding the owner as a participant until we have a database ID
			newChannel.addParticipant(owner);
		} catch (MixPersistenceException e) {
			Log.error(e.getMessage());
			throw new CannotCreateMixChannelException(name);
		}
		
		channels.put(name, newChannel);
		
		return newChannel;
	}

	@Override
	public MixChannel getChannel(String channelName) {
		return channels.get(channelName);
	}

	@Override
	public void destroyChannel(JID requestor, String name) throws UnauthorizedException, CannotDestroyMixChannelException {

		MixChannel toDestroy = this.getChannel(name);
		
		if (toDestroy != null) {
			if (toDestroy.isDestructable(requestor)) {
				channels.remove(name);

				try {
					toDestroy.destroy();
					this.persistenceManager.delete(toDestroy);
				} catch (MixPersistenceException e) {
					Log.error(e.getMessage());
					throw new CannotDestroyMixChannelException(name, e.getMessage());
				}	
			} else {
				throw new UnauthorizedException("Not owner");
			}
			
		}
		return;
	}
}
