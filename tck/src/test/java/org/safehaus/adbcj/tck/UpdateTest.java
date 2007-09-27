package org.safehaus.adbcj.tck;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.safehaus.adbcj.Connection;
import org.safehaus.adbcj.ConnectionManager;
import org.safehaus.adbcj.Result;
import org.safehaus.adbcj.ResultSet;
import org.safehaus.adbcj.Value;
import org.testng.annotations.Test;


public class UpdateTest extends ConnectionManagerDataProvider {

	@Test(dataProvider="connectionManagerDataProvider")
	public void testSimpleUpdates(ConnectionManager connectionManager) throws InterruptedException {
		Connection connection = connectionManager.connect().get();
		assertNotNull(connection);

		try {
			// Clear out updates table
			Result result = connection.executeUpdate("DELETE FROM updates").get();
			assertNotNull(result);

			// Insert a row
			result = connection.executeUpdate("INSERT INTO updates (id) VALUES (1)").get();
			assertNotNull(result);
			assertEquals(result.getAffectedRows(), Long.valueOf(1));
	
			// Select the row
			ResultSet rs = connection.executeQuery("SELECT id FROM updates").get();
			assertNotNull(rs);
			assertEquals(rs.size(), 1);
			Value value = rs.get(0).get(0);
			assertEquals(value.getInt(), 1);
			assertEquals(value.getField().getColumnLabel(), "id");
	
			// Update nothing
			result = connection.executeUpdate("UPDATE updates SET id=1 WHERE id=2").get();
			assertNotNull(result);
			assertEquals(result.getAffectedRows(), Long.valueOf(0));
			
			// Update inserted row
			result = connection.executeUpdate("UPDATE updates SET id=2").get();
			assertNotNull(result);
			assertEquals(result.getAffectedRows(), Long.valueOf(1));
			
			// Delete inserted row
			result = connection.executeUpdate("DELETE FROM updates WHERE id=2").get();
			assertNotNull(result);
			assertEquals(result.getAffectedRows(), Long.valueOf(1));
		} finally {
			// Close connection
			connection.close(true).get();
		}
	}
	
}
