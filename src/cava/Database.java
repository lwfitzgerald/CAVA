package cava;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database class. Provides a wrapper for Java DB. The particular dialect of SQL (assuming it is standards
 * compliant) should be transparent to the user; in theory, we should be able to change RDBMS with changes
 * to this class only.
 * @author Ben
 */
public class Database {
	private Connection conn;
	private String driver = "org.apache.derby.jdbc.EmbeddedDriver";
	private boolean connected;
	private String errorMsg;
	
	/**
	 * No-arg constructor to allow TrackDatabase to extend
	 */
	protected Database(){
		 connected = false;
	}

	/**
	 * Optionally create and then connected to the specified database
	 * @param dbName The name of the database you wish to connect to
	 * @param create If true, create a database. If false, just connect to it
	 */
	public Database(String dbName, boolean create){
		String connection;
		if(create){
			connection = "jdbc:derby:" + dbName  + ";create=true";
		}else{
			connection = "jdbc:derby:" + dbName;
		}
		connect(connection);
	}
	
	/**
	 * Connect to an already created database. A convenience method; equivalent to database(dbName,false);
	 * * @param dbName The name of the database you wish to connect to
	 */
	public Database(String dbName){
		String connection = "jdbc:derby:" + dbName; 
		connect(connection);
	}
	
	private void connect(String connection){
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException e) {
			this.connected = false;
			setError("Could not load driver class");
			return;
		}
		try {
			conn = DriverManager.getConnection(connection);
		} catch (Exception e) {
			this.connected = false;
			setError(e);
			return;
		}
		this.connected = true;
	}
	
	private void setError(Exception e){
		//Check to see if this is an SQL-Exception. If it is, grab out the SQL details and unwrap the
		//exception chain. If it's not, just get the message.
		errorMsg = "";
		if(e instanceof SQLException){
	        while (e != null)
	        {
	            errorMsg = errorMsg + "\n----- SQLException -----";
	            errorMsg = errorMsg + "  SQL State:  " + ((SQLException) e).getSQLState();
	            errorMsg = errorMsg + "  Error Code: " + ((SQLException) e).getErrorCode();
	            errorMsg = errorMsg + "  Message:    " + e.getMessage();
	            e = ((SQLException) e).getNextException();
	        }
		}else{
			errorMsg = e.toString();
		}
	}
	
	protected void setError(String s){
		errorMsg = s;
	}
	
	/**
	 * @return Returns whether or not the database was connected to successfully
	 */
	public boolean isConnected(){
		return connected;
	}
	
	/**
	 * @return a string containing the last error message generated
	 */
	public String getLastError(){
		return errorMsg;
	}
	
	/**
	 * Print the last error to stderr
	 */
	public void printLastError(){
		Dbg.syserr(errorMsg);
	}
	
	/**
	 * Create a table 
	 * @param tableDescription An SQL Query of the form "CREATE TABLE TABLE_NAME (COLUMN NAMES AND TYPES) FLAGS"
	 * @return true if the table was created successfully, false if not. See getLastError() for details;
	 */
	public boolean createTable(String tableDescription){
		try {
			Statement st = conn.createStatement();
			st.executeUpdate(tableDescription);
		} catch (Exception e) {
			setError(e);
			return false;
		}
		return true;
	}
	
	private int updateInsertDelete(String q){
		int altered = 0;
		try{
			Statement st = conn.createStatement();
			 altered = st.executeUpdate(q);
		}catch(Exception e){
			e.printStackTrace();
			setError(e);
			return -1;
		}
		return altered;		
	}
	
	/**
	 * Perform an insert query
	 * @param insert A valid SQL query
	 * @return the number of inserted rows, or -1 on failure. See getLastError() for details.
	 */
	public int insert(String insert){
		return updateInsertDelete(insert);
	}
	
	/**
	 * Delete from a table
	 * @param delete A valid SQL query
	 * @return the number of deleted rows, or -1 on failure. See getLastError() for details.
	 */
	public int delete(String delete){
		return updateInsertDelete(delete);
	}
	
	/**
	 * Update row(s) in the table
	 * @param update A valid SQL query
	 * @return the number of affected rows, or -1 on failure. See getLastError() for details
	 */
	public int update(String update){
		return updateInsertDelete(update);
	}
	
	/**
	 * Select rows from a table
	 * @param select A valid SQL query
	 * @return true on success, false on failure -- set getLastError() for details. Call fetchRow() to use the first/next row in the result set
	 */
	public CavaResultSet select(String select){
		
		Statement st;
		try {
			st = conn.createStatement();
			return new CavaResultSet(st.executeQuery(select));
		} catch (Exception e) {
			e.printStackTrace();
			setError(e);
			return null;
		}
	}
	

	
	public boolean addTrack(String trackname,String lowertrackname,int albumid,int artistid,String path,String bitrate,String codec,int length,int trackno,byte[] scms,int scmsavailable){
		String sql = "INSERT INTO track (trackname,lowertrackname,albumid,artistid,path,bitrate,codec,length,trackno,scms,scmsavailable) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
		try{
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, trackname);
			statement.setString(2, lowertrackname);
			statement.setInt(3, albumid);
			statement.setInt(4, artistid);
			statement.setString(5, path);
			statement.setString(6, bitrate);
			statement.setString(7, codec);
			statement.setInt(8, length);
			statement.setInt(9, trackno);
			statement.setBytes(10, scms);
			statement.setInt(11,scmsavailable);
			statement.executeUpdate();
			statement.close();
		}catch (Exception e) {
			e.printStackTrace();
			setError(e);
			return false;
		}

		return true;
	}

	
	public void addSCMSData(int trackid,byte[] scms){
		String sql = "UPDATE track SET scms=?, scmsavailable=1 WHERE trackid="+trackid;
		PreparedStatement statement;
		try {
			statement = conn.prepareStatement(sql);
			statement.setBytes(1, scms);
			statement.executeUpdate();
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
			setError(e);
		}
	}
	
	/**
	 * Create a new prepared statement. Not entirely self-contained, so other
	 * methods may be preferred. 
	 * @param sql
	 * @return
	 */
	public PreparedStatement createStatement(String sql){
		try{
			PreparedStatement statement = conn.prepareStatement(sql);
			return statement;
		}catch(Exception e){
			setError(e);
			return null;
		}
	}
	
	/**
	 * Execute the statement created by createStatement
	 * @param statement
	 * @return
	 */
	public boolean executeStatement(PreparedStatement statement){
		try{
			statement.executeUpdate();
			statement.close();
			return true;
		}catch(Exception e){
		
			setError(e);
			return false;
		}
	}
	


	/**
	 * Escape any special characters in the specified string and truncate it if necessary
	 * @param s the string to be escaped
	 * @param maxLength the maximum length the string can be, or -1 to perform no truncation. Larger strings will be truncated
	 * @return the escaped version of s	
	 */
	public String escapeString(String s,int maxLength){
		s = s.replaceAll("'", "''");
		if(maxLength!= -1 && s.length() > maxLength){
			s = s.substring(0,maxLength);
		}
		return s;
	}
	
	public String checkLength(String s, int maxLength){
		if(maxLength!= -1 && s.length() > maxLength){
			s = s.substring(0,maxLength);
		}
		return s;
	}
	
	
	public static void main(String arg[]){
		//database db = new database("testdb",true);
		Database db = new Database("testdb");
		if(db.isConnected()){
			System.out.println("Connected to db successfully");
		}else{
			System.out.println("Could not connect to db");
			System.out.println(db.getLastError());
		}
		
		if(db.createTable("CREATE TABLE test(num int,name varchar(40))")){
			System.out.println("table created successfully");
		}else{
			System.out.println("Failed to create table");
			System.out.println(db.getLastError());
		}
		int insert = db.insert("INSERT INTO test VALUES(1,'Ben'),(2,'Anu'),(3,'Luke')");
		if(insert != -1){
			System.out.println("Inserted "+ insert +" row(s) into table successfully");
		}else{
			System.out.println("Failed to insert into table");
			System.out.println(db.getLastError());			
		}
		
		CavaResultSet rs = db.select("SELECT COUNT(*) FROM test");

		if(rs!=null){
			if(rs.fetchRow()){
				int count  = rs.fetchInt(1);
				System.out.println("Count: " + count);
			}else{
				System.out.println("Failed selecting row");
			}
		}else{
			System.out.println("Could not select rows");
			System.out.println(db.getLastError());		
		}
		
	}
	
	public class CavaResultSet{
		ResultSet rs;
		public CavaResultSet(ResultSet rs) {
			this.rs = rs;
		}
		
		/**
		 * Attempt to fetch the next row. Note that this must be called before you use the first result.
		 * @return True if successful, false on failure. See getLastError() for details
		 */
		public boolean fetchRow(){
			try {
				return rs.next();
			} catch (Exception e) {
				setError(e);
				return false;
			}
		}
		
		/**
		 * Fetch a value from the given index in the current row (indexing from 1)
		 * @param index the index into the row, starting from 1
		 * @return A generic object type to fit all database types. You may therefore have to cast to the correct type.
		 * Returns null on failure.
		 */
		public Object fetchValue(int index){
			try {
				return rs.getObject(index);
			} catch (Exception e) {
				setError(e);
				return null;
			}	
		}
		
		/**
		 * Fetch a value from the column with the given name from the current row
		 * @param colName the name of column you with to fetch from
		 * @return A generic object type to fit all database types. You may therefore have to cast to the correct type.
		 * Returns null on failure.
		 */
		public Object fetchValue(String colName){
			try {
				return rs.getObject(colName);
			} catch (Exception e) {
				setError(e);
				return null;
			}	
		}
		
		/**
		 * Fetch an integer value from the given index in the current row (indexing from 1)
		 * @param index the index (from 1) into the current row you wish to fetch from
		 * @return The required integer on success, or -1 on failure -- use isIntValueReadable() first if 
		 * -1 is a valid value for your query.
		 */
		public int fetchInt(int index){
			try {
				return rs.getInt(index);
			} catch (Exception e) {
				setError(e);
				return -1;
			}	
		}
		
		/**
		 * Fetch an integer value from the column with the given name from the current row
		 * @param colName the name of column you with to fetch from
		 * @return The required integer on success, or -1 on failure -- use isIntValueReadable() first if 
		 * -1 is a valid value for your query.
		 */
		public int fetchInt(String colName){
			try {
				return rs.getInt(colName);
			} catch (Exception e) {
				setError(e);
				return -1;
			}	
		}
		
		/**
		 * Test the value at the given index to see if it is readable as an integer. fetchInt() returns
		 * -1, so if this is a valid value for your query, call this method first
		 * @param index the index (from 1) into the current row you wish to fetch from
		 * @return true if the value can be read, false on error. See getLastError() for details.
		 */
		public boolean isIntValueReadable(int index){
			try{
				rs.getInt(index);
				return true;
			}catch(Exception e){
				setError(e);
				return false;
			}
		}
		
		/**
		 * Test the value with the given colName in the current row to see if it readable as an integer
		 * fetchInt() returns 01, so if this is a value value for your query, call this method first.
		 * @param colName the name of column you with to fetch from
		 * @return true if the value can be read, false on error. See getLastError() for details.
		 */
		public boolean isIntValueReadable(String colName){
			try{
				rs.getInt(colName);
				return true;
			}catch(Exception e){
				setError(e);
				return false;
			}
		}
		
		/**
		 * Fetch a string value from the given index in the current row (indexing from 1)
		 * @param index the index (from 1) into the current row you wish to fetch from
		 * @return The required string on success, or null on failure - see getLastError() for details.
		 */
		public String fetchString(int index){
			try{
				return (String) fetchValue(index);
			}catch(Exception e){
				setError(e);
				return null;
			}
		}
		
		/**
		 * Fetch a string value from the column with the given name from the current row
		 * @param colName the name of column you wish to fetch from
		 * @return The required string on success, or null on failure - see getLastError() for details.
		 */
		public String fetchString(String colName){
			try{
				return (String) fetchValue(colName);
			}catch(Exception e){
				setError(e);
				return null;
			}
		}
		
		public byte[] fetchBlob(int index){
			try{
				return rs.getBytes(index);
			}catch(Exception e){
				setError(e);
				return null;
			}
		}
		
		public Blob createBlobFrom(byte[] bytes){
			Blob blob;
			try {
				blob = conn.createBlob();
				blob.setBytes(1, bytes);
			} catch (SQLException e) {
				setError(e);
				return null;
			}
			return blob;
		}
		
	}
	
	
	
}