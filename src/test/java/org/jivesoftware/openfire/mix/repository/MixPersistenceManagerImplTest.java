package org.jivesoftware.openfire.mix.repository;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.jivesoftware.database.ConnectionProvider;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.database.EmbeddedConnectionProvider;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.MixService;
import org.jivesoftware.openfire.mix.MixXmppServiceImpl;
import org.jivesoftware.openfire.mix.model.LocalMixChannel;
import org.jivesoftware.openfire.mix.model.LocalMixChannelParticipant;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.JiveProperties;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

public class MixPersistenceManagerImplTest {

	Mockery mockery = new Mockery() {{
			setImposteriser(ClassImposteriser.INSTANCE);
		}};

	MixPersistenceManagerImpl mixPersistenceManager;

	JiveProperties jiveProperties;

	final MixXmppServiceImpl mockXmppService = mockery.mock(MixXmppServiceImpl.class);
	final MixService mockMixService = mockery.mock(MixService.class);
	final IdentityManager testIdentityManager = new PKGenerator();

	public MixPersistenceManagerImplTest() {

		mixPersistenceManager = new MixPersistenceManagerImpl(jiveProperties, mockXmppService, testIdentityManager,
				testIdentityManager, testIdentityManager);
		
		mockery.checking(new Expectations() {
			{
				allowing(mockMixService).getServiceDomain();
				will(returnValue("shakespeare.example.com"));
				
				allowing(mockXmppService).route(with(any(Message.class)));
			}
		});
	}

	@BeforeClass
	public static void initialiseDatabase() throws SQLException, ClassNotFoundException, IOException {
		// The Embedded database needs somewhere to store temporary files

		System.out.println("Database script dir is " + System.getProperty(DATABASE_UPGRADE_DIR));
		
		String scriptDir = System.getProperty(DATABASE_UPGRADE_DIR) + File.separator + "24" + File.separator;
		Path hsqldbDirPath = Paths.get(scriptDir + File.separator + "hsqldb");
		Files.createDirectory(hsqldbDirPath);
		JiveGlobals.setHomeDirectory(scriptDir + File.separator + "hsqldb");

		ConnectionProvider embedded = new EmbeddedConnectionProvider();

		DbConnectionManager.setConnectionProvider(embedded);

		Connection conn = DbConnectionManager.getConnection();

		// Could be running from the local test diretory, or from ANT so deal with locating the file
		String resourceName = scriptDir + File.separator + "openfire_"
				+ DbConnectionManager.getDatabaseType() + ".sql";

		TestSchemaManager.executeSQLScript(conn, new FileInputStream(resourceName), Boolean.TRUE);
	}
	
	private static final String DATABASE_UPGRADE_DIR = "databaseUpgradeDir";

	@AfterClass
	public static void tidyUp() {
		Path hsqldbDirPath = Paths.get(System.getProperty(DATABASE_UPGRADE_DIR) + File.separator + "24" + File.separator + "hsqldb");
		deleteFilesInFolder(hsqldbDirPath.toFile());
	}

	@Before
	public void setUp() throws Exception {
		jiveProperties = mockery.mock(JiveProperties.class);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void thatTablesArePresent() throws SQLException {

		Connection conn = DbConnectionManager.getConnection();

		DatabaseMetaData meta = conn.getMetaData();
		ResultSet res = meta.getTables(null, null, "OFMIXCHANNEL", null);

		assertTrue(res.next());

	}

	private static final String INSERT_SINGLE_CHANNEL = "INSERT INTO " + MixPersistenceManagerImpl.CHANNEL_TABLE_NAME
			+ "(channelid, creationDate, modificationDate, name, jidVisibility, owner)" + " VALUES (?,?,?,?,?,?);";

	@Test
	public void testLoadChannels() throws SQLException, MixPersistenceException {
		Connection conn = DbConnectionManager.getConnection();
		Random rand = new Random();

		String dateStr = new Date().getTime() + "";
		PreparedStatement stmt = conn.prepareStatement(INSERT_SINGLE_CHANNEL);
		stmt.setLong(1, rand.nextInt());
		stmt.setString(2, dateStr);
		stmt.setString(3, dateStr);
		stmt.setString(4, "CHANNEL_NAME");
		stmt.setInt(5, 0);
		stmt.setString(6, "test_node");

		stmt.execute();

		stmt.close();
		conn.close();

		mockery.checking(new Expectations() {
			{
				allowing(mockMixService).getServiceDomain();
				will(returnValue("shakespeare.example.com"));
				allowing(mockMixService).getId();
				will(returnValue(1L));
			}
		});

		assertEquals(1, mixPersistenceManager.loadChannels(mockMixService).size());
		
	}

	@Test
	public void testSavingAndDeletingMixChannel() throws MixPersistenceException, SQLException {

		MixChannel mc = new LocalMixChannel(mockMixService, TEST_CHANNEL_NAME,
				new JID(TEST_OWNER_NODE, TEST_SERVICE_DOMAIN, null), mockXmppService, mixPersistenceManager);
		mc = mixPersistenceManager.save(mc);
		assertNotNull(mc);
		assertNotNull(mc.getID());
		
		Connection conn = DbConnectionManager.getConnection();
		PreparedStatement stmt = conn.prepareStatement(
				"SELECT * FROM " + MixPersistenceManagerImpl.CHANNEL_TABLE_NAME + "  WHERE channelID=?");
		stmt.setLong(1, mc.getID());

		ResultSet rs = stmt.executeQuery();
		
		assertTrue(rs.next());
		
		mixPersistenceManager.delete(mc);
		
		rs = stmt.executeQuery();
		
		assertFalse(rs.next());
		
	}

	private static final String TEST_OWNER_NODE = "hag66";
	private static final String TEST_USER_NODE = "hecate";
	private static final String TEST_SERVICE_DOMAIN = "shakespeare.example";
	private static final String TEST_MIX_SERVICE_DOMAIN = "mix." + TEST_SERVICE_DOMAIN;
	private static final String TEST_CHANNEL_NAME = "coven";

	private static final JID TEST_USER_JID = new JID(TEST_USER_NODE, TEST_SERVICE_DOMAIN, null);
	private static final JID TEST_USER_PROXY_JID = new JID("123456", TEST_SERVICE_DOMAIN, null);

	@Test
	public void thatParticipantSavingAndDeletion() throws MixPersistenceException, SQLException {

		// Create a channel
		MixChannel mc = mixPersistenceManager.save(new LocalMixChannel(mockMixService, TEST_CHANNEL_NAME,
				new JID(TEST_OWNER_NODE, TEST_SERVICE_DOMAIN, null), mockXmppService, mixPersistenceManager));

		Set<String> subscriptions = new HashSet<String>(
				Arrays.asList("urn:xmpp:mix:nodes:messages", "urn:xmpp:mix:nodes:presence"));

		// Save a participant
		MixChannelParticipant toPersist = new LocalMixChannelParticipant(TEST_USER_PROXY_JID, TEST_USER_JID, mc,
				subscriptions);
		toPersist = mixPersistenceManager.save(toPersist);

		Connection conn = DbConnectionManager.getConnection();
		PreparedStatement stmt = conn.prepareStatement(
				"SELECT * FROM " + MixPersistenceManagerImpl.CHANNEL_PARTICIPANT_TABLE_NAME + "  WHERE realJid=?");
		stmt.setString(1, TEST_USER_JID.toString());

		ResultSet rs = stmt.executeQuery();
		
		assertTrue(rs.next());
		
		// Now delete and make sure its cascaded
		mixPersistenceManager.delete(toPersist);
		
		rs = stmt.executeQuery();
		
		assertFalse(rs.next());

	}

	public static void deleteFilesInFolder(final File folder) {
		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				deleteFilesInFolder(fileEntry);
				fileEntry.delete();
			} else {
				fileEntry.delete();
			}
		}

		folder.delete();
	}

	class PKGenerator implements IdentityManager {

		@Override
		public long nextUniqueID() {
			long limit = Integer.MAX_VALUE;
			return ThreadLocalRandom.current().nextLong(limit);
		}
		
	}
	
	/**
	 * More or less a copy of the Openfire SchemaManager to allow the upgrade
	 * script to be executed.
	 * 
	 * @author garethf
	 *
	 */
	static class TestSchemaManager {

		public static void executeSQLScript(Connection con, InputStream resource, Boolean autoreplace)
				throws SQLException, IOException {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(resource))) {
				boolean done = false;
				while (!done) {
					StringBuilder command = new StringBuilder();
					while (true) {
						String line = in.readLine();
						if (line == null) {
							done = true;
							break;
						}
						// Ignore comments and blank lines.
						if (isSQLCommandPart(line)) {
							command.append(' ').append(line);
						}
						if (line.trim().endsWith(";")) {
							break;
						}
					}
					// Send command to database.
					if (command.toString().contains("ofVersion")) {
						// Ignore this
					} else if (!done && !command.toString().equals("")) {
						// Remove last semicolon when using Oracle or DB2 to
						// prevent "invalid character error"
						if (DbConnectionManager.getDatabaseType() == DbConnectionManager.DatabaseType.oracle
								|| DbConnectionManager.getDatabaseType() == DbConnectionManager.DatabaseType.db2) {
							command.deleteCharAt(command.length() - 1);
						}
						PreparedStatement pstmt = null;
						try {
							String cmdString = command.toString();
							if (autoreplace) {
								cmdString = cmdString.replaceAll("jiveVersion", "ofVersion");
							}
							pstmt = con.prepareStatement(cmdString);
							pstmt.execute();
						} catch (SQLException e) {

							System.out.println("SchemaManager: Failed to execute SQL:\n" + command.toString());
							throw e;
						} finally {
							DbConnectionManager.closeStatement(pstmt);
						}
					}
				}
			}
		}

		private static boolean isSQLCommandPart(String line) {
			line = line.trim();
			if (line.equals("")) {
				return false;
			}
			// Check to see if the line is a comment. Valid comment types:
			// "//" is HSQLDB
			// "--" is DB2 and Postgres
			// "#" is MySQL
			// "REM" is Oracle
			// "/*" is SQLServer
			return !(line.startsWith("//") || line.startsWith("--") || line.startsWith("#") || line.startsWith("REM")
					|| line.startsWith("/*") || line.startsWith("*"));
		}
	}

}
