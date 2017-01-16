package org.jivesoftware.openfire.mix;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.database.EmbeddedConnectionProvider;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.JiveProperties;
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

	public MixPersistenceManagerImplTest() {

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
				"CREATE TABLE ofMixChannel (channelID INTEGER NOT NULL, serviceID INTEGER NOT NULL, creationDate CHAR(15) NOT NULL, modificationDate CHAR(15) NOT NULL, name VARCHAR(50) NOT NULL, jidVisibility INTEGER NOT NULL, CONSTRAINT ofMixChannel_pk PRIMARY KEY (channelID))");

		st.close();
		conn.close();

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

	@AfterClass
	public static void tidyUp() {
		Path hsqldbDirPath = Paths.get(System.getProperty("user.dir") + File.separator + "hsqldb");
		deleteFilesInFolder(hsqldbDirPath.toFile());
	}

	@Before
	public void setUp() throws Exception {
		jiveProperties = mockery.mock(JiveProperties.class);

		mixPersistenceManager = new MixPersistenceManagerImpl(jiveProperties, mockPacketRouter);

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

	@Test
	@Ignore
	public void testLoadServices() {
		fail("Not yet implemented");
	}

}
