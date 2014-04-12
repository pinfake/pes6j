/*
 * Created on 06-ago-2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package pes6j.database;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Enumeration;
import java.util.Properties;

//import com.mysql.jdbc.Statement;

/**
 * @author pin
 * 
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Db implements Serializable {
	private Connection conn;
	private Statement stmt;
	private ResultSet rset;
	private Properties properties;
	private int dbType = -1;
	private int isolationLevel;

	public static int ISO_SERIALIZABLE = Connection.TRANSACTION_SERIALIZABLE;

	public static final String DbTypeNames[] = new String[] {
			"MYSQL", "POSTGRES" //$NON-NLS-1$ //$NON-NLS-2$
	};

	public static final int TYPE_MYSQL = 0;
	public static final int TYPE_POSTGRES = 1;

	public Db(Properties prop) {
		this.properties = prop;

		String type = properties.getProperty("db-type"); //$NON-NLS-1$

		if (type == null) {
			dbType = -1;
			return;
		}

		if (type.toUpperCase().equals(DbTypeNames[TYPE_POSTGRES])) {
			dbType = TYPE_POSTGRES;
		}
		if (type.toUpperCase().equals(DbTypeNames[TYPE_MYSQL])) {
			dbType = TYPE_MYSQL;
		}
	}

	public void connect() throws SQLException {
		switch (dbType) {
		case TYPE_POSTGRES:
			connect_postgres();
			break;
		case TYPE_MYSQL:
			connect_mysql();
			break;
		default:
			throw new SQLException(
					"Incorrect database type, check the properties file"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	public int getDbType() {
		return (dbType);
	}

	private void connect_postgres() throws SQLException {
		try {
			Class.forName("org.postgresql.Driver"); //$NON-NLS-1$
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new SQLException(ex.getMessage());
		}

		// Connect to the Database

		// conn = DriverManager.getConnection
		// ("jdbc:oracle:thin:@localhost:1521:rinol", "rinoluser", "rinoluser");
		if (conn == null || conn.isClosed()) {
			conn = DriverManager.getConnection("jdbc:postgresql://" //$NON-NLS-1$
					+ properties.getProperty("db-host") //$NON-NLS-1$
					+ "/" //$NON-NLS-1$
					+ properties.getProperty("db-name"), //$NON-NLS-1$
					properties.getProperty("db-username"), //$NON-NLS-1$
					properties.getProperty("db-password")); //$NON-NLS-1$
			isolationLevel = conn.getTransactionIsolation();
		}
	}
	
	private void connect_mysql() throws SQLException {


		// Connect to the Database

		// conn = DriverManager.getConnection
		// ("jdbc:oracle:thin:@localhost:1521:rinol", "rinoluser", "rinoluser");
		if (conn == null || conn.isClosed()) {
			conn =
				DriverManager.getConnection("jdbc:mysql://"+properties.getProperty("db-host")+"/"+
						properties.getProperty("db-name")+"?" +
				"user="+properties.getProperty("db-username")+
				"&password="+properties.getProperty("db-password"));
			isolationLevel = conn.getTransactionIsolation();
		}
	}

	public void disconnect() throws SQLException {
		conn.close();
	}

	public String parseSQL(String raw) {
		String parsed = ""; //$NON-NLS-1$

		return (parsed);
	}

	public PreparedStatement prepareStatement(String query ) throws SQLException {
		PreparedStatement ps = conn.prepareStatement(query/*, Statement.RETURN_GENERATED_KEYS*/);
		ps.clearParameters();
		ps.setEscapeProcessing(true);
		
		return( ps );
	}

	public String getInsertStatement(String table, Row r) {
		String column, columns, values;
		String query = "insert into " + table + " ("; //$NON-NLS-1$ //$NON-NLS-2$
		Enumeration keys = r.keys();
		columns = ""; //$NON-NLS-1$
		while (keys.hasMoreElements()) {
			column = (String) keys.nextElement();
			if (columns.length() > 0)
				columns += ","; //$NON-NLS-1$
			columns += column;
		}
		query += columns + ") values ("; //$NON-NLS-1$
		values = ""; //$NON-NLS-1$
		for (int i = 0; i < r.getColumnCount(); i++) {
			if (values.length() > 0)
				values += ","; //$NON-NLS-1$
			values += "?"; //$NON-NLS-1$
		}
		query += values + ")"; //$NON-NLS-1$
		return query;
	}

	public void setStatementValues(Row r) throws SQLException {
		Enumeration keys = r.keys();
		String column;
		int i = 1;
		while (keys.hasMoreElements()) {
			column = (String) keys.nextElement();
			setStatementValue(null, i++, r.getObject(column));
		}
	}

	public void setStatementValue(PreparedStatement stmt, int idx, Object value) throws SQLException {
		// Codigo para parsear aqui...
		String encoded;
		// System.out.println("Poniendo idx " + idx + "=" + value.toString() );
		if (value instanceof String) {
			((String) value).replaceAll("'", "''"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		/*
		 * Esto es para evitar petes de nullazos en bases de datos oracle 8i
		 */
		// if( value == null ) value = new String( "" );
		try {

			stmt.setObject(idx, value);
		} catch (SQLException e) {
			System.err.println("excep" + e.getMessage()); //$NON-NLS-1$
			e.printStackTrace();
			throw e;
		}

	}

	public void setStatementValues( Object[] values, PreparedStatement stmt) throws SQLException {
		for (int i = 1; i <= values.length; i++) {
			// System.out.println( i-1 + " -> " + values[i-1] ); //$NON-NLS-1$
			setStatementValue(stmt, i, values[i - 1]);
		}
	}

	public void closeStatement(PreparedStatement stmt) throws SQLException {
		stmt.close();
	}

	public int executeUpdate(PreparedStatement stmt) throws SQLException {
		return stmt.executeUpdate();
	}
	
	public Table getKeys(PreparedStatement stmt) throws SQLException {
		ResultSet rset = stmt.getGeneratedKeys(); 
		return( getResults( rset ));
	}

	public Table executeQuery(PreparedStatement stmt) throws SQLException {
		Table t;
		ResultSet rset = stmt.executeQuery();
		t = getResults(rset);
		rset.close();
		return (t);
	}

	private Table getResults(ResultSet rset) throws SQLException {
		Table ret = new Table();

		ResultSetMetaData rsmeta = rset.getMetaData();

		while (rset.next()) {
			Row info = new Row();
			for (int i = 1; i <= rsmeta.getColumnCount(); i++) {
				switch (rsmeta.getColumnType(i)) {
				case Types.FLOAT:
				case Types.DOUBLE:
					info.setFloat(rsmeta.getColumnName(i).toLowerCase(), rset
							.getFloat(i));
					break;
	
				case Types.INTEGER:
				case Types.NUMERIC:
				case Types.BIGINT:

					info.setLong(rsmeta.getColumnName(i).toLowerCase(), rset
							.getLong(i));

					break;
				case Types.BINARY:
				case Types.VARBINARY:
					info.setBytes(rsmeta.getColumnName(i).toLowerCase(), rset
							.getBytes(i));
					break;
				case Types.VARCHAR:
				default:
					info.set(rsmeta.getColumnName(i).toLowerCase(), rset
							.getString(i));
					break;
				}
			}
			ret.put(info);
		}
		return (ret);
	}

	public Table executeQuery(String query) throws SQLException {
		Table ret;

		Statement stmt = conn.createStatement();
		// stmt.setEscapeProcessing( true );
		ResultSet rset = stmt.executeQuery(query);
		ret = getResults(rset);
		rset.close();
		stmt.close();
		return (ret);
	}

	public int executeUpdate(String query) throws SQLException {
		int ret = 0;
		Statement stmt = conn.createStatement();
		stmt.setEscapeProcessing(true);
		stmt.executeUpdate(query);
		ResultSet tmp = stmt.getGeneratedKeys();
		if (tmp.next()) {
			// Retrieve the auto generated key(s).
			ret = tmp.getInt(1);
		}
		stmt.close();
		return ret;
	}

	public void commit() throws SQLException {
		conn.commit();
	}

	public void rollback() throws SQLException {
		conn.rollback();
	}

	public void setAutoCommit(boolean value) throws SQLException {
		conn.setAutoCommit(value);
	}

	public void changeIsolation(int value) throws SQLException {
		conn.setTransactionIsolation(value);
	}

	public void restoreIsolation() throws SQLException {
		conn.setTransactionIsolation(isolationLevel);
	}

	/**
	 * Permite saber si la conexi�n est� abierta
	 * 
	 * @return - true si lo esta y false en otro caso.
	 */
	public boolean isConnected() {
		try {
			return !conn.isClosed();
		} catch (SQLException e) {
			return false;
		}
	}
}
