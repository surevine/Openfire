package org.jivesoftware.openfire.mix.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.MixPersistenceManager;
import org.jivesoftware.openfire.mix.MixService;
import org.jivesoftware.openfire.mix.MixXmppService;
import org.jivesoftware.openfire.mix.model.LocalMixChannel;
import org.jivesoftware.openfire.mix.model.LocalMixChannelParticipant;
import org.jivesoftware.openfire.mix.model.LocalMixService;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.jivesoftware.util.JiveProperties;
import org.jivesoftware.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

public class MixPersistenceManagerImpl implements MixPersistenceManager {
	private static final Logger Log = LoggerFactory.getLogger(MixPersistenceManager.class);

	public final static String CHANNEL_TABLE_NAME = "OFMIXCHANNEL";

	private static final String LOAD_SERVICES = "SELECT serviceID, subdomain, description FROM ofMixService";

	private static final String LOAD_ALL_CHANNELS = "SELECT channelID, creationDate, modificationDate, name, jidVisibility, owner "
			+ "FROM ofMixChannel";

	private JiveProperties jiveProperties;

	private MixXmppService xmppService;

	private IdentityManager channelKeys;

	private IdentityManager mcpKeys;

	private IdentityManager mcpSubsKeys;

	public MixPersistenceManagerImpl(JiveProperties jiveProperties, MixXmppService xmppService,
			IdentityManager channelKeys, IdentityManager mcpKeys, IdentityManager mcpSubsKeys) {
		this.jiveProperties = jiveProperties;
		this.xmppService = xmppService;
		this.channelKeys = channelKeys;
		this.mcpKeys = mcpKeys;
		this.mcpSubsKeys = mcpSubsKeys;
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
				MixService newSerivce = new LocalMixService(xmppServer, jiveProperties, subdomain, description, this.xmppService, this);
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
					LocalMixChannel channel = new LocalMixChannel(resultSet.getLong(1), mixService, resultSet.getString(4),
							new JID(resultSet.getString(6), mixService.getServiceDomain(), null), xmppService, this, new Date(Long.parseLong(resultSet.getString(2).trim())));

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

	private static final String ADD_CHANNEL = "INSERT INTO " + CHANNEL_TABLE_NAME
			+ " (creationDate, name, jidVisibility, modificationDate, owner, channelID)" + " VALUES (?,?,?,?,?,?)";

	private static final String UPDATE_CHANNEL = "UPDATE " + CHANNEL_TABLE_NAME
			+ " SET creationDate = ?, name = ?, jidVisibility = ?, modificationDate = ?, owner = ?"
			+ " WHERE channelID = ?";

	@Override
	public MixChannel save(MixChannel toPersist) throws MixPersistenceException {
		Connection con = null;
		PreparedStatement pstmt = null;

		try {

			con = DbConnectionManager.getConnection();

			if(toPersist.getID() == null) {
				pstmt = con.prepareStatement(ADD_CHANNEL);
				
				// Get PK for the channel
				toPersist.setID(this.channelKeys.nextUniqueID());
			} else {
				pstmt = con.prepareStatement(UPDATE_CHANNEL);
			}
			
			pstmt.setString(1, StringUtils.dateToMillis(toPersist.getCreationDate()));
			pstmt.setString(2, toPersist.getName());
			pstmt.setInt(3, toPersist.getJidVisibilityMode().getId());
			pstmt.setString(4, StringUtils.dateToMillis(toPersist.getCreationDate()));
			pstmt.setString(5, toPersist.getOwner().getNode());
			pstmt.setLong(6, toPersist.getID());

			pstmt.executeUpdate();

		} catch (SQLException sqle) {
			Log.error(sqle.getMessage(), sqle);
			throw new MixPersistenceException(sqle);
		} finally {
			DbConnectionManager.closeConnection(pstmt, con);

		}

		return toPersist;

	}

	private static final String DELETE_CHANNEL = "DELETE FROM " + CHANNEL_TABLE_NAME + " WHERE channelID=?";

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

	public static final String CHANNEL_PARTICIPANT_TABLE_NAME = "ofMixChannelParticipant";

	private static final String ADD_CHANNEL_PARTICIPANT = "INSERT INTO " + CHANNEL_PARTICIPANT_TABLE_NAME
			+ " (mcpID, realJID, proxyJID, nickName, channelJidVisibilityPreference, channelID_fk)"
			+ " VALUES (?,?,?,?,?,?)";

	public static final String CHANNEL_PARTICIPANT_SUBSCRIPTIONS_TABLE = "ofMixChannelParticipantSubscription";

	private static final String ADD_NODE_SUBSCRIPTIONS = "INSERT INTO " + CHANNEL_PARTICIPANT_SUBSCRIPTIONS_TABLE
			+ "(mcpSubsID, participantID_fk, nodeName)" + " VALUES (?,?,?);";

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
			pstmt.setString(2, mcp.getRealJid().toString());
			pstmt.setString(3, mcp.getJid().toString());
			pstmt.setString(4, mcp.getNick());
			pstmt.setInt(5, mcp.getJidVisibilityPreference().getId());
			pstmt.setLong(6, mcp.getChannel().getID());

            pstmt.executeUpdate();
            DbConnectionManager.fastcloseStmt(pstmt);

			// Now deal with the subscriptions
			for (String sub : mcp.getSubscriptions()) {
				pstmt = con.prepareStatement(ADD_NODE_SUBSCRIPTIONS);

				pstmt.setLong(1, this.mcpSubsKeys.nextUniqueID());
				pstmt.setLong(2, mcp.getID());
				pstmt.setString(3, sub);

				pstmt.executeUpdate();
			}

		} catch (SQLException sqle) {
			Log.error(sqle.getMessage(), sqle);
			throw new MixPersistenceException(sqle);
		} finally {
			DbConnectionManager.closeConnection(pstmt, con);
		}

		return mcp;
	}

	private static final String DELETE_MCP = "DELETE FROM " + CHANNEL_PARTICIPANT_TABLE_NAME + " WHERE mcpID=?";
	private static final String DELETE_MCP_SUBS = "DELETE FROM " + CHANNEL_PARTICIPANT_SUBSCRIPTIONS_TABLE
			+ " WHERE participantID_fk=?";

	@Override
	public boolean delete(MixChannelParticipant toDelete) throws MixPersistenceException {
		Connection con = null;
		PreparedStatement stmt = null;

		try {

			con = DbConnectionManager.getConnection();

			stmt = con.prepareStatement(DELETE_MCP_SUBS);
			stmt.setLong(1, toDelete.getID());
			stmt.executeUpdate();
            DbConnectionManager.fastcloseStmt(stmt);

            stmt = con.prepareStatement(DELETE_MCP);
            stmt.setLong(1, toDelete.getID());
            stmt.executeUpdate();

		} catch (SQLException sqle) {
			Log.error(sqle.getMessage(), sqle);
			return false;
		} finally {
			DbConnectionManager.closeConnection(stmt, con);
		}

		return true;
	}

	private static final String LOAD_ALL_PARTICIPANTS_BY_CHANNEL = "SELECT mcpID, realJID, proxyJID, nickName, channelJidVisibilityPreference, channelID_fk "
			+ "FROM " + CHANNEL_PARTICIPANT_TABLE_NAME + " WHERE channelID_fk=?";
	
	@Override
	public Collection<MixChannelParticipant> findByChannel(MixChannel channel) throws MixPersistenceException {
		List<MixChannelParticipant> participantsByChannel = new ArrayList<>();
		
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
			connection = DbConnectionManager.getConnection();
			statement = connection.prepareStatement(LOAD_ALL_PARTICIPANTS_BY_CHANNEL);
			statement.setLong(1, channel.getID());
			resultSet = statement.executeQuery();

			while (resultSet.next()) {
				try {
					
					JID proxyJid = new JID(resultSet.getString(3));
					JID realJid = new JID(resultSet.getString(2));
					
					LocalMixChannelParticipant mcp = new LocalMixChannelParticipant(resultSet.getLong(1), proxyJid, realJid, channel, this);
					
					participantsByChannel.add(mcp);
					
				} catch (SQLException e) {
					Log.error("A database exception prevented one particular MIX channel loaded from the database.", e);
				}
			}
		} catch (SQLException e) {
			throw new MixPersistenceException(e);
		} finally {
			DbConnectionManager.closeConnection(resultSet, statement, connection);
		}
		
		return participantsByChannel;
	}
	
	private static final String LOAD_SUBSCRIPTIONS_BY_PARTICIPANT = "SELECT mcpSubsID, nodeName, participantID_fk "
			+ "FROM " + CHANNEL_PARTICIPANT_SUBSCRIPTIONS_TABLE + " WHERE participantID_fk=?";
	
	public Set<String> findByParticipant(MixChannelParticipant participant) throws MixPersistenceException {
		Set<String> subscriptions = new HashSet<>();
		
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
			connection = DbConnectionManager.getConnection();
			statement = connection.prepareStatement(LOAD_SUBSCRIPTIONS_BY_PARTICIPANT);
			statement.setLong(1, participant.getID());
			resultSet = statement.executeQuery();

			while (resultSet.next()) {
				try {
					subscriptions.add(resultSet.getString(2));
					
				} catch (SQLException e) {
					Log.error("A database exception prevented one particular MIX channel loaded from the database.", e);
				}
			}
		} catch (SQLException e) {
			throw new MixPersistenceException(e);
		} finally {
			DbConnectionManager.closeConnection(resultSet, statement, connection);
		}
		
		return subscriptions;
	}
	
	private static final String REMOVE_SUBSCRIPTIONS = "DELETE FROM " + CHANNEL_PARTICIPANT_SUBSCRIPTIONS_TABLE + " WHERE participantID_fk=?";

	@Override
	public MixChannelParticipant update(MixChannelParticipant mcp) throws MixPersistenceException {
		Connection con = null;
		PreparedStatement pstmt = null;

		try {

			con = DbConnectionManager.getConnection();

			pstmt = con.prepareStatement(REMOVE_SUBSCRIPTIONS);
			pstmt.setLong(1, mcp.getID());

            pstmt.executeUpdate();
            DbConnectionManager.fastcloseStmt(pstmt);

			// Now deal with the subscriptions
			for (String sub : mcp.getSubscriptions()) {
				pstmt = con.prepareStatement(ADD_NODE_SUBSCRIPTIONS);

				pstmt.setLong(1, this.mcpSubsKeys.nextUniqueID());
				pstmt.setLong(2, mcp.getID());
				pstmt.setString(3, sub);

				pstmt.executeUpdate();
			}

		} catch (SQLException sqle) {
			Log.error(sqle.getMessage(), sqle);
			throw new MixPersistenceException(sqle);
		} finally {
			DbConnectionManager.closeConnection(pstmt, con);
		}

		return mcp;
	}
//	
//	private static final String FIND_PARTICIPANT_BY_ID = "SELECT * FROM "
//	
//	public MixChannelParticipant findParticipantById(long find) throws MixPersistenceException {
//		Connection con = null;
//		PreparedStatement pstmt = null;
//
//		try {
//
//			con = DbConnectionManager.getConnection();
//
//			pstmt = con.prepareStatement(REMOVE_SUBSCRIPTIONS);
//			pstmt.setLong(1, mcp.getID());
//
//            pstmt.executeUpdate();
//            DbConnectionManager.fastcloseStmt(pstmt);
//
//			// Now deal with the subscriptions
//			for (String sub : mcp.getSubscriptions()) {
//				pstmt = con.prepareStatement(ADD_NODE_SUBSCRIPTIONS);
//
//				pstmt.setLong(1, this.mcpSubsKeys.nextUniqueID());
//				pstmt.setLong(2, mcp.getID());
//				pstmt.setString(3, sub);
//
//				pstmt.executeUpdate();
//			}
//
//		} catch (SQLException sqle) {
//			Log.error(sqle.getMessage(), sqle);
//			throw new MixPersistenceException(sqle);
//		} finally {
//			DbConnectionManager.closeConnection(pstmt, con);
//		}
//		
//		return null;
//		
//	}

}
