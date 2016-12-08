package org.jivesoftware.openfire.mix;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.mix.spi.LocalMixChannel;
import org.jivesoftware.openfire.mix.spi.MixServiceImpl;
import org.jivesoftware.openfire.muc.spi.LocalMUCRoom;
import org.jivesoftware.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixPersistenceManagerImpl implements MixPersistenceManager {
	private static final Logger Log = LoggerFactory.getLogger(MixPersistenceManager.class);
	
	private static final String LOAD_SERVICES = "SELECT subdomain,description,isHidden FROM ofMixService";

    private static final String LOAD_ALL_CHANNELS =
            "SELECT channelID, creationDate, modificationDate, name, jidVisibility " +
            "FROM ofMixChannel WHERE serviceID=?";

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
                String subdomain = rs.getString(1);
                String description = rs.getString(2);
                Boolean isHidden = Boolean.valueOf(rs.getString(3));
                MixServiceImpl mixService = new MixServiceImpl(xmppServer, subdomain, description, isHidden);
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
                    LocalMixChannel channel = new LocalMixChannel(mixService, resultSet.getString(4));
                    channel.setID(resultSet.getLong(1));
                    channel.setCreationDate(new Date(Long.parseLong(resultSet.getString(2).trim()))); // creation date
                    channels.add(channel);
                } catch (SQLException e) {
                    Log.error("A database exception prevented one particular MUC room to be loaded from the database.", e);
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
