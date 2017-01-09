package org.jivesoftware.openfire.mix;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.mix.spi.LocalMixChannel;
import org.jivesoftware.openfire.mix.spi.MixServiceImpl;
import org.jivesoftware.util.JiveProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixPersistenceManagerImpl implements MixPersistenceManager {
	private static final Logger Log = LoggerFactory.getLogger(MixPersistenceManager.class);
	
	private static final String LOAD_SERVICES = "SELECT serviceID, subdomain, description FROM ofMixService";

    private static final String LOAD_ALL_CHANNELS =
            "SELECT channelID, creationDate, modificationDate, name, jidVisibility " +
            "FROM ofMixChannel WHERE serviceID=?";

    private JiveProperties jiveProperties;
    
    private PacketRouter router;
    
    public MixPersistenceManagerImpl(JiveProperties jiveProperties) {
		this.jiveProperties = jiveProperties;
	}
    
    public void initialize(XMPPServer server) {
    	router = server.getPacketRouter();
    }
    
    @Override
    public Collection<MixService> loadServices(XMPPServer xmppServer) {
    	List<MixService> mixServices = new ArrayList<>();
    	
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(LOAD_SERVICES);
            rs = pstmt.executeQuery();
            while (rs.next()) {
            	Long id = rs.getLong(1);
                String subdomain = rs.getString(2);
                String description = rs.getString(3);
                MixServiceImpl mixService = new MixServiceImpl(xmppServer, jiveProperties, this, subdomain, description);
                mixService.setId(id);
                mixServices.add(mixService);
            }
        }
        catch (Exception e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
        
		return mixServices;    	
    }
	
	@Override
	public Collection<MixChannel> loadChannels(MixService mixService) throws MixPersistenceException {
        final List<MixChannel> channels = new ArrayList<>();

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = DbConnectionManager.getConnection();
            statement = connection.prepareStatement(LOAD_ALL_CHANNELS);
            statement.setLong(1, mixService.getId());
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                try {
                	// TODO: initialisation of the nodes that the channel supports
                    LocalMixChannel channel = new LocalMixChannel(mixService, resultSet.getString(4), router);
                    channel.setID(resultSet.getLong(1));
                    channel.setCreationDate(new Date(Long.parseLong(resultSet.getString(2).trim()))); // creation date
                    channels.add(channel);
                } catch (SQLException e) {
                    Log.error("A database exception prevented one particular MIX channel loaded from the database.", e);
                }
            }
        } catch (SQLException e) {
			throw new MixPersistenceException(e);
		} finally {
            DbConnectionManager.closeConnection(resultSet, statement, connection);
        }

        return channels;
	}

}
