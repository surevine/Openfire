package org.jivesoftware.openfire.mix.spi;

import java.util.Date;

import org.jivesoftware.openfire.mix.MixChannel;
import org.jivesoftware.openfire.mix.MixService;
import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityMode;
import org.xmpp.packet.JID;

public class LocalMixChannel implements MixChannel {

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

	public LocalMixChannel(MixService mixService, String name) {
		this.name = name;
		this.mixService = mixService;
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
		// TODO Auto-generated method stub
		return null;
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
}
