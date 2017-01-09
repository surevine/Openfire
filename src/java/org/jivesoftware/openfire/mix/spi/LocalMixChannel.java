package org.jivesoftware.openfire.mix.spi;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Node;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.mix.MixChannel;
import org.jivesoftware.openfire.mix.MixChannelParticipant;
import org.jivesoftware.openfire.mix.MixService;
import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.IQ.Type;

public class LocalMixChannel implements MixChannel {

	private static final Logger Log = LoggerFactory.getLogger(LocalMixChannel.class);
	
	private Long id;
	
	/**
	 * This {@link MixService} to which this channel is attached.
	 */
	private MixService mixService;
	
    /**
     * The name of the channel.
     */
    private String name;

    /**
     * The date when the channel was created.
     */
    private Date creationDate;
    
    /**
     * For routes packets back to the user/proxy.
     */
    private PacketRouter router;
    
    private List<MixChannelNode> supportedNodes;
    
    private Map<MixChannelParticipant, List<MixChannelNode>> participants;

	public LocalMixChannel(MixService mixService, String name, PacketRouter router) {
		this.name = name;
		this.mixService = mixService;
		this.router = router;
	}

	public void setName(String name) {
		this.name = name;
	}

	private ChannelJidVisibilityMode jidVisibilityMode;

	@Override
	public String getUID() {
		// name is unique for each one particular MUC service.
		return name;
	}

	@Override
	public JID getJID() {
		return new JID(getName(), mixService.getServiceDomain(), null);
	}

	@Override
	public Long getID() {
		return id;
	}
    
    public String getName() {
		return name;
	}

	public void setID(Long id) {
		this.id = id;
	}

	@Override
	public ChannelJidVisibilityMode getJidVisibilityMode() {
		return jidVisibilityMode;
	}

	public void setJidVisibilityMode(ChannelJidVisibilityMode jidVisibilityMode) {
		this.jidVisibilityMode = jidVisibilityMode;
	}

	@Override
	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	/**
	 * Entry method for Packets to be processed for the channel.
	 * 
	 * @see org.jivesoftware.openfire.mix.MixChannel#process(org.xmpp.packet.Packet)
	 */
	@Override
	public void process(Packet packet) throws IllegalArgumentException {

		// Validate the packet is destined for this channel, if not throw the exception
		if (!this.getJID().equals(packet.getTo())) {
			Log.error("Received invalid packet at " + this.getName() + " channel, destined for " + packet.getTo());
			throw new IllegalArgumentException();
		}
		
		if (packet instanceof IQ) {
			IQ iq = (IQ) packet;
			switch (iq.getType()) {
				case set:
					if ("join".equals(iq.getChildElement().getName())) {
						// Deal with a user joining this channel
						this.join(iq);
					}
				default:
					
			}
		}
	}

	private void join(IQ joinRequest) {
		
		Element joinNode = joinRequest.getChildElement();
		
		@SuppressWarnings("unchecked")
		List<Node> selectedSubscriptions = joinNode.selectNodes("./subscribe");

		List<MixChannelNode> subscribedNodes = new ArrayList<MixChannelNode>();
		
        for (Node subscription: selectedSubscriptions) {
            if (subscription.getNodeType() == Node.ELEMENT_NODE) {
            	Element elem = (Element) subscription;
            	String nodeName = elem.attribute("node").getName();
            	for (MixChannelNode supported : supportedNodes) {
            		if (supported.getType().name().equals(nodeName)) {
            			subscribedNodes.add(supported);
            		}
            	}
            }
        }
        
        participants.put(new MixChannelParticipant(joinRequest.getFrom(), subscribedNodes), subscribedNodes);
		
		IQ result = new IQ(Type.result, joinRequest.getID());
		result.setFrom(this.getJID());
		result.setTo(joinRequest.getFrom());
		
		Element responseElem = joinRequest.getChildElement().createCopy();
		responseElem.addAttribute("jid", joinRequest.getFrom().toBareJID());
		
		result.setChildElement(responseElem);
		
		router.route(result);
		
	}

	public void setSupportedNodes(List<MixChannelNode> supportedNodes) {
		this.supportedNodes = supportedNodes;
	}

}
