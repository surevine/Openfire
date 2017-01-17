package org.jivesoftware.openfire.mix.spi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.XMPPServerListener;
import org.jivesoftware.openfire.disco.DiscoInfoProvider;
import org.jivesoftware.openfire.disco.DiscoItem;
import org.jivesoftware.openfire.disco.DiscoItemsProvider;
import org.jivesoftware.openfire.disco.DiscoServerItem;
import org.jivesoftware.openfire.disco.ServerItemsProvider;
import org.jivesoftware.openfire.mix.MixChannelNode;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.MixPersistenceManager;
import org.jivesoftware.openfire.mix.MixService;
import org.jivesoftware.openfire.mix.handler.MixChannelJoinPacketHandler;
import org.jivesoftware.openfire.mix.handler.MixChannelMessagePacketHandler;
import org.jivesoftware.openfire.mix.handler.MixChannelPacketHandler;
import org.jivesoftware.openfire.mix.model.LocalMixChannel;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.util.JiveProperties;
import org.jivesoftware.util.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.component.Component;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;
import org.xmpp.forms.DataForm;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;

public class MixServiceImpl implements Component, MixService, ServerItemsProvider, DiscoInfoProvider,
		DiscoItemsProvider, XMPPServerListener {

	private static final Logger Log = LoggerFactory.getLogger(MixServiceImpl.class);

	private final XMPPServer xmppServer;

	private JiveProperties jiveProperties;

	private MixPersistenceManager persistenceManager;
	
	private List<MixChannelPacketHandler> packetHandlers;

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

	/**
	 * The packet router for the server.
	 */
	private PacketRouter router = null;

	private Map<String, MixChannel> channels;

	/**
	 * Create a new group chat server.
	 *
	 * @param subdomain
	 *            Subdomain portion of the conference services (for example,
	 *            conference for conference.example.org)
	 * @param description
	 *            Short description of service for disco and such. If
	 *            <tt>null</tt> or empty, a default value will be used.
	 * @param isHidden
	 *            True if this service should be hidden from services views.
	 * @throws IllegalArgumentException
	 *             if the provided subdomain is an invalid, according to the JID
	 *             domain definition.
	 */
	public MixServiceImpl(XMPPServer xmppServer, JiveProperties jiveProperties,
			MixPersistenceManager persistenceManager, String subdomain, String description, PacketRouter router) {
		this.xmppServer = xmppServer;
		this.jiveProperties = jiveProperties;
		this.persistenceManager = persistenceManager;
		this.router = router;

		channels = new HashMap<>();
		
		packetHandlers = Arrays.asList(
				new MixChannelJoinPacketHandler(),
				new MixChannelMessagePacketHandler()
			);

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

		router = xmppServer.getPacketRouter();
	}

	public void initializeSettings() {
		serviceEnabled = jiveProperties.getBooleanProperty("xmpp.mix.enabled", true);
	}

	public void processPacket(Packet packet) {
		if (!isServiceEnabled()) {
			return;
		}

		try {
			// Check if the packet is a disco request or a packet with namespace
			// iq:register
			if (packet instanceof IQ) {
				if (process((IQ) packet)) {
					return;
				}
			}

			if (packet.getTo().getNode() == null) {
				// This was addressed at the service itself, which by now should
				// have been handled.
				IQ request = (IQ) packet;
				if (packet instanceof IQ && request.isRequest()) {

					final IQ reply = IQ.createResultIQ(request);
					reply.setChildElement(request.getChildElement().createCopy());
					
					MixChannel newChannel = new LocalMixChannel(this, request.getChildElement().attributeValue("channel"), router, persistenceManager);
					persistenceManager.save(newChannel);
					
					router.route(reply);
				}
				Log.debug("Ignoring stanza addressed at conference service: {}", packet.toXML());
			} else {
				// The packet is a normal packet that should possibly be sent to
				// the node
				String channelName = packet.getTo().getNode();
				MixChannel channel = channels.get(channelName);
				
				if (packet instanceof IQ) {
					if(channel == null) {
						final IQ reply = IQ.createResultIQ((IQ) packet);
						reply.setChildElement(((IQ) packet).getChildElement().createCopy());
						reply.setError(PacketError.Condition.service_unavailable);
						router.route(reply);
						return;
					}

					for (MixChannelPacketHandler handler : packetHandlers) {
						IQ result = handler.processIQ(channels.get(channelName), (IQ) packet);

						if (result != null) {
							router.route(result);
						}
						
						break;
					}
				} else if (packet instanceof Message) {
					if(channel == null) {
						return;
					}
					for(MixChannelPacketHandler handler : packetHandlers) {
						if(handler.processMessage(channels.get(channelName), (Message) packet));
					}					
				}

			}
		} catch (Exception e) {
			Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
		}
	}

	/**
	 * Returns true if the IQ packet was processed.
	 *
	 * @param iq
	 *            the IQ packet to process.
	 * @return true if the IQ packet was processed.
	 */
	private boolean process(IQ iq) {
		Element childElement = iq.getChildElement();
		String namespace = null;
		// Ignore IQs of type ERROR
		if (IQ.Type.error == iq.getType()) {
			return false;
		}
		if (childElement != null) {
			namespace = childElement.getNamespaceURI();
		}
		if ("http://jabber.org/protocol/disco#info".equals(namespace)) {
			IQ reply = xmppServer.getIQDiscoInfoHandler().handleIQ(iq);
			router.route(reply);
		} else if ("http://jabber.org/protocol/disco#items".equals(namespace)) {
			IQ reply = xmppServer.getIQDiscoItemsHandler().handleIQ(iq);
			router.route(reply);
		} else if ("urn:xmpp:ping".equals(namespace)) {
			router.route(IQ.createResultIQ(iq));
		} else {
			return false;
		}
		return true;
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

}
