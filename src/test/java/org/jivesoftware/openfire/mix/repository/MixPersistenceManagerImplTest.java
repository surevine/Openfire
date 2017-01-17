package org.jivesoftware.openfire.mix.repository;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Random;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.database.EmbeddedConnectionProvider;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.MixService;
import org.jivesoftware.openfire.mix.model.LocalMixChannel;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.JiveProperties;
import org.jivesoftware.util.StringUtils;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class MixPersistenceManagerImplTest {

	Mockery mockery = new Mockery() {
		{
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};

	MixPersistenceManagerImpl mixPersistenceManager;

	JiveProperties jiveProperties;

	final PacketRouter mockPacketRouter = mockery.mock(PacketRouter.class);
	final MixService mockMixService = mockery.mock(MixService.class);

	public MixPersistenceManagerImplTest() {

		mixPersistenceManager = new MixPersistenceManagerImpl(jiveProperties, mockPacketRouter);
	}

	@BeforeClass
	public static void initialiseDatabase() throws SQLException, ClassNotFoundException, IOException {
		// The Embedded database needs somewhere to store temporary files

		Path hsqldbDirPath = Paths.get(System.getProperty("user.dir") + File.separator + "hsqldb");
		Files.createDirectory(hsqldbDirPath);
		JiveGlobals.setHomeDirectory(System.getProperty("user.dir") + File.separator + "hsqldb");

		DbConnectionManager.setConnectionProvider(new EmbeddedConnectionProvider());

		Connection conn = DbConnectionManager.getConnection();

		Statement st = conn.createStatement();

		st.execute(
				"CREATE TABLE ofMixChannel (channelID INTEGER NOT NULL, creationDate CHAR(15) NOT NULL, modificationDate CHAR(15) NOT NULL, name VARCHAR(50) NOT NULL, jidVisibility INTEGER NOT NULL, CONSTRAINT ofMixChannel_pk PRIMARY KEY (channelID))");

		st.close();
		conn.close();

	}

	@AfterClass
	public static void tidyUp() {
		Path hsqldbDirPath = Paths.get(System.getProperty("user.dir") + File.separator + "hsqldb");
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

	private static final String INSERT_SINGLE_CHANNEL = "INSERT INTO " + MixPersistenceManagerImpl.CHANNEL_TABLE_NAME + "(channelid, creationDate, modificationDate, name, jidVisibility)" 
			+ " VALUES (?,?,?,?,?);";
	
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
		
		stmt.execute();
		
		stmt.close();
		conn.close();
		
		mockery.checking(new Expectations() {{
			allowing(mockMixService).getId();
			will(returnValue(1L));
		}});
		
		assertEquals(1, mixPersistenceManager.loadChannels(mockMixService).size());
		
	}
	
	@Test
	public void testSaving() throws MixPersistenceException {
		
		assertNotNull(mixPersistenceManager.save(new LocalMixChannel(mockMixService, "TEST_CHANNEL_NAME", mockPacketRouter, mixPersistenceManager)));
	}
	
	@Test
	public void testFindByID() throws MixPersistenceException {
		MixChannel saved = mixPersistenceManager.save(new LocalMixChannel(mockMixService, "TEST_CHANNEL_NAME", mockPacketRouter, mixPersistenceManager));
		
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

}
