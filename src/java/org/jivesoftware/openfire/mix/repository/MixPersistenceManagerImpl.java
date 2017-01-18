package org.jivesoftware.openfire.mix.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.MixPersistenceManager;
import org.jivesoftware.openfire.mix.MixService;
import org.jivesoftware.openfire.mix.MixXmppService;
import org.jivesoftware.openfire.mix.model.LocalMixChannel;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.jivesoftware.openfire.mix.spi.MixServiceImpl;
import org.jivesoftware.util.JiveProperties;
import org.jivesoftware.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixPersistenceManagerImpl implements MixPersistenceManager {
	private static final Logger Log = LoggerFactory.getLogger(MixPersistenceManager.class);

	public final static String CHANNEL_TABLE_NAME = "OFMIXCHANNEL";

	private static final String LOAD_SERVICES = "SELECT serviceID, subdomain, description FROM ofMixService";

	private static final String LOAD_ALL_CHANNELS = "SELECT channelID, creationDate, modificationDate, name, jidVisibility "
			+ "FROM ofMixChannel";

	private JiveProperties jiveProperties;

	private MixXmppService xmppService;
	
	private static final int CHANNEL_SEQ_TYPE = 500;

	private IdentityManager channelKeys = new MixIdentityManager(CHANNEL_SEQ_TYPE, 5);
	
	private static final int MCP_SEQ_TYPE = 501;
	
	private IdentityManager mcpKeys = new MixIdentityManager(MCP_SEQ_TYPE, 5);
	
	private static final int MCP_SUBS_SEQ_TYPE = 502;
	
	private IdentityManager mcpSubsKeys = new MixIdentityManager(MCP_SUBS_SEQ_TYPE, 5);
	
	
	public MixPersistenceManagerImpl(JiveProperties jiveProperties, MixXmppService xmppService) {
		this.jiveProperties = jiveProperties;
		this.xmppService = xmppService;
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
				MixService newSerivce = new MixServiceImpl(xmppServer, jiveProperties, subdomain, description, this.xmppService);
				newSerivce.setId(id);
				mixServices.add(newSerivce);
			}
		} catch (Exception e) {
			Log.error(e.getMessage(), e);
		} finally {
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
			resultSet = statement.executeQuery();

			while (resultSet.next()) {
				try {
					// TODO: initialisation of the nodes that the channel
					// supports
					LocalMixChannel channel = new LocalMixChannel(mixService, resultSet.getString(4), xmppService, this);
					channel.setID(resultSet.getLong(1));
					channel.setCreationDate(new Date(Long.parseLong(resultSet.getString(2).trim()))); // creation
																										// date
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

	private static final String UPDATE_CHANNEL = "UPDATE " + CHANNEL_TABLE_NAME
			+ " SET creationDate=?, name=?, jidVisibility=? WHERE channelID=?";

	private static final String ADD_CHANNEL = "INSERT INTO " + CHANNEL_TABLE_NAME
			+ " (channelID, creationDate, name, jidVisibility, modificationDate)" + " VALUES (?,?,?,?,?)";

	@Override
	public MixChannel save(MixChannel toPersist) throws MixPersistenceException {
		Connection con = null;
		PreparedStatement pstmt = null;

		try {

			con = DbConnectionManager.getConnection();

			pstmt = con.prepareStatement(ADD_CHANNEL);
			
			// Get PK for the channel
			toPersist.setID(this.channelKeys.nextUniqueID());
			pstmt.setLong(1, toPersist.getID());
			
			pstmt.setString(2, StringUtils.dateToMillis(toPersist.getCreationDate()));
			pstmt.setString(3, toPersist.getName());
			pstmt.setInt(4, toPersist.getJidVisibilityMode().getId());
			pstmt.setString(5, StringUtils.dateToMillis(toPersist.getCreationDate()));

			pstmt.executeUpdate();

		} catch (SQLException sqle) {
			Log.error(sqle.getMessage(), sqle);
			throw new MixPersistenceException(sqle);
		} finally {
			DbConnectionManager.closeConnection(pstmt, con);

		}

		return toPersist;

	}

	@Override
	public MixChannel update(MixChannel toUpdate) throws MixPersistenceException {
		Connection con = null;
		PreparedStatement pstmt = null;

		try {

			con = DbConnectionManager.getConnection();

			// its an update
			pstmt = con.prepareStatement(UPDATE_CHANNEL);
			pstmt.setString(1, StringUtils.dateToMillis(toUpdate.getCreationDate()));
			pstmt.setString(2, toUpdate.getName());
			pstmt.setInt(3, toUpdate.getJidVisibilityMode().getId());

			pstmt.executeUpdate();
		} catch (SQLException sqle) {
			Log.error(sqle.getMessage(), sqle);
			throw new MixPersistenceException(sqle);
		} finally {
			DbConnectionManager.closeConnection(pstmt, con);
		}

		return toUpdate;
	}

	private static final String FIND_BY_ID = "SELECT channelID, creationDate, name, jidVisibility FROM "
			+ CHANNEL_TABLE_NAME + " WHERE channelID=?";

	private static final String DELETE_CHANNEL = "DELETE FROM " + CHANNEL_TABLE_NAME + " WHERE channelID=?";

	private static final String CHANNEL_PARTICIPANT_TABLE_NAME = null;

	@Override
	public boolean delete(MixChannel toDelete) throws MixPersistenceException {
		Connection con = null;
		PreparedStatement pstmt = null;

		try {

			con = DbConnectionManager.getConnection();

			pstmt = con.prepareStatement(DELETE_CHANNEL);
			pstmt.setLong(1, toDelete.getID());
			pstmt.executeUpdate();

		} catch (

		SQLException sqle) {
			Log.error(sqle.getMessage(), sqle);
			return false;
		} finally {
			DbConnectionManager.closeConnection(pstmt, con);
		}

		return true;
	}

	private static final String ADD_CHANNEL_PARTICIPANT = "INSERT INTO " + CHANNEL_PARTICIPANT_TABLE_NAME
			+ " (mcpID, realJID, proxyJID, nickName, channelJidVisibilityPreference, channelID_FK)" 
			+ " VALUES (?, ?,?,?,?,?,?,?,?,?,?,?,?,?)";

	private static final String CHANNEL_PARTICIPANT_SUBSCRIPTIONS_TABLE = null;
	
	private static final String ADD_NODE_SUBSCRIPTIONS = "INSERT INTO " + CHANNEL_PARTICIPANT_SUBSCRIPTIONS_TABLE 
			+ "(cpsID, participantID_FK, nodeName)"
			+ " VALUES (?,?,?);";
	
	@Override
	public MixChannelParticipant save(MixChannelParticipant mcp) throws MixPersistenceException {
		Connection con = null;
		PreparedStatement pstmt = null;

		try {

			con = DbConnectionManager.getConnection();

			pstmt = con.prepareStatement(ADD_CHANNEL_PARTICIPANT);
			
			// Set the PK for the MCP
			mcp.setID(this.mcpKeys.nextUniqueID());
			pstmt.setLong(1, mcp.getID());
			pstmt.setString(2, mcp.getRealJid().toFullJID());
			pstmt.setString(3, mcp.getJid().toFullJID());
			pstmt.setString(4, mcp.getNick());
			pstmt.setInt(5, mcp.getJidVisibilityPreference().getId());
			pstmt.setLong(6, mcp.getChannel().getID());
			
			pstmt.executeUpdate();
			
			pstmt.close();
			
			// Now deal with the subscriptions
			for (String sub : mcp.getSubscriptions()) {
				pstmt = con.prepareStatement(ADD_NODE_SUBSCRIPTIONS);
				
				pstmt.setLong(1, this.mcpSubsKeys.nextUniqueID());
				pstmt.setLong(2, mcp.getID());
				pstmt.setString(3, sub);
				
				pstmt.executeUpdate();
				pstmt.close();
			}

		} catch (SQLException sqle) {
			Log.error(sqle.getMessage(), sqle);
			throw new MixPersistenceException(sqle);
		} finally {
			DbConnectionManager.closeConnection(pstmt, con);
		}

		return mcp;
	}

}
