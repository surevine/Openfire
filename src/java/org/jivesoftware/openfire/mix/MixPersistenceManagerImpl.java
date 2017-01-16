package org.jivesoftware.openfire.mix;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityMode;
import org.jivesoftware.openfire.mix.model.LocalMixChannel;
import org.jivesoftware.openfire.mix.spi.MixServiceImpl;
import org.jivesoftware.util.JiveProperties;
import org.jivesoftware.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixPersistenceManagerImpl implements MixPersistenceManager {
	private static final Logger Log = LoggerFactory.getLogger(MixPersistenceManager.class);

	public final static String CHANNEL_TABLE_NAME = "ofMixChannel";

	private static final String LOAD_SERVICES = "SELECT serviceID, subdomain, description FROM ofMixService";

	private static final String LOAD_ALL_CHANNELS = "SELECT channelID, creationDate, modificationDate, name, jidVisibility "
			+ "FROM ofMixChannel WHERE serviceID=?";

	private JiveProperties jiveProperties;

	private PacketRouter router;

	private MixServiceImpl mixService;

	public MixPersistenceManagerImpl(JiveProperties jiveProperties, PacketRouter router) {
		this.jiveProperties = jiveProperties;
		this.router = router;
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
				this.mixService = new MixServiceImpl(xmppServer, jiveProperties, this, subdomain, description, this.router);
				mixService.setId(id);
				mixServices.add(mixService);
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
			statement.setLong(1, mixService.getId());
			resultSet = statement.executeQuery();

			while (resultSet.next()) {
				try {
					// TODO: initialisation of the nodes that the channel
					// supports
					LocalMixChannel channel = new LocalMixChannel(mixService, resultSet.getString(4), router);
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
			+ " (creationDate, name, jidVisibility)" + "VALUES (?,?,?)";

	@Override
	public boolean save(MixChannel toPersist) throws MixPersistenceException {
		Connection con = null;
		PreparedStatement pstmt = null;

		try {

			con = DbConnectionManager.getConnection();

			// its a new channel
			pstmt = con.prepareStatement(ADD_CHANNEL, Statement.RETURN_GENERATED_KEYS);

			pstmt.setString(1, StringUtils.dateToMillis(toPersist.getCreationDate()));
			pstmt.setString(2, toPersist.getName());
			pstmt.setInt(3, convertToInt(toPersist.getJidVisibilityMode()));

			pstmt.executeUpdate();

			ResultSet rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				toPersist.setID(rs.getInt(1));
			}

		} catch (SQLException sqle) {
			Log.error(sqle.getMessage(), sqle);
			return false;
		} finally {
			DbConnectionManager.closeConnection(pstmt, con);

		}

		return true;

	}

	@Override
	public boolean update(MixChannel toUpdate) throws MixPersistenceException {
		Connection con = null;
		PreparedStatement pstmt = null;

		try {

			con = DbConnectionManager.getConnection();

			// its an update
			pstmt = con.prepareStatement(UPDATE_CHANNEL);
			pstmt.setString(1, StringUtils.dateToMillis(toUpdate.getCreationDate()));
			pstmt.setString(2, toUpdate.getName());
			pstmt.setInt(3, convertToInt(toUpdate.getJidVisibilityMode()));

			pstmt.executeUpdate();
		} catch (SQLException sqle) {
			Log.error(sqle.getMessage(), sqle);
			return false;
		} finally {
			DbConnectionManager.closeConnection(pstmt, con);
		}

		return true;
	}

	private static final String FIND_BY_ID = "SELECT channelID, creationDate, name, jidVisibility FROM "
			+ CHANNEL_TABLE_NAME + " WHERE channelID=?";

	@Override
	public MixChannel findByID(long ID) throws MixPersistenceException {
		Connection con = null;
		PreparedStatement pstmt = null;

		LocalMixChannel channel = null;

		try {

			con = DbConnectionManager.getConnection();

			pstmt = con.prepareStatement(FIND_BY_ID);
			pstmt.setLong(1, ID);

			ResultSet rs = pstmt.executeQuery();

			if (rs.first()) {

				try {
					channel = new LocalMixChannel(this.mixService, rs.getString(3), router);
					channel.setID(rs.getLong(1));
					channel.setCreationDate(new Date(Long.parseLong(rs.getString(2).trim())));
					channel.setName(rs.getString(3));
					channel.setJidVisibilityMode(convertFromInt(rs.getInt(4)));
				} catch (SQLException e) {
					Log.error("A database exception prevented one particular MIX channel loaded from the database.", e);
				}
			}
		} catch (SQLException sqle) {
			Log.error(sqle.getMessage(), sqle);
			return null;
		} finally {
			DbConnectionManager.closeConnection(pstmt, con);
		}

		return channel;
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

	private static int convertToInt(ChannelJidVisibilityMode jidVisibilityMode) {

		int retVal = 0;
		switch (jidVisibilityMode) {
		case VISIBLE:
			retVal = 0;
		case HIDDEN:
			retVal = 1;
		}

		return retVal;

	}

	private static ChannelJidVisibilityMode convertFromInt(int mode) {
		if (mode == 1) {
			return ChannelJidVisibilityMode.HIDDEN;
		} else {
			return ChannelJidVisibilityMode.VISIBLE;
		}
	}

}
