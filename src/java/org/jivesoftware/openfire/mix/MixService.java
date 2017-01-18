package org.jivesoftware.openfire.mix;

import java.util.Collection;

import org.jivesoftware.database.JiveID;
import org.jivesoftware.util.JiveConstants;
import org.xmpp.component.Component;
import org.jivesoftware.openfire.mix.exception.MixChannelAlreadyExistsException;
import org.jivesoftware.openfire.mix.model.MixChannel;

@JiveID(JiveConstants.MIX_SERVICE)
public interface MixService extends Component {

	Long getId();

    /**
     * Returns the fully-qualifed domain name of this MIX service.
     * The domain is composed by the service name and the
     * name of the XMPP server where the service is running.
     * 
     * @return the MIX server domain (service name + host name).
     */
    String getServiceDomain();

    /**
     * Returns the subdomain of the MIX service.
     *
     * @return the subdomain of the MIX service.
     */
    String getServiceName();
    
    Collection<MixChannel> getChannels();
    
    boolean isServiceEnabled();

	void setId(Long id);

	MixChannel createChannel(String name) throws MixPersistenceException, MixChannelAlreadyExistsException;

	MixChannel getChannel(String channelName);
}
