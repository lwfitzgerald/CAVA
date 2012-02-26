package cava;

import javax.swing.table.AbstractTableModel;

import cava.Database.CavaResultSet;

@SuppressWarnings("serial")
public class BrowserTableModel extends AbstractTableModel{
	private String query;
	private int numRows = 0;
	protected String[] columnNames = null;
	private Object[][] data = null;
	private boolean connected;
	protected Database db;
	//private String selectStatement = null;
	
	/**
	 * No arg constructor for extending class
	 */
	protected BrowserTableModel(){
		return;
	}
	
	public BrowserTableModel(String[] columnHeaders,String query) {
		//Initialize connection
		db = new Database("..//lib//database//trackDB");
		columnNames = columnHeaders;
		this.query = query;
		if(db.isConnected()){
			//System.out.println("Connected");
			connected = true;
			setRowCount();
			setData();
		}else{
			Dbg.syserr("Couldn't connect");
		}
	}
	
	public BrowserTableModel(String[] columnHeaders,String query, String countQuery) {
		//Initialize connection
		//System.out.println("Trying to connect to database...");
		db = new Database("..//lib//database//trackDB");
		columnNames = columnHeaders;
		this.query = query;
		if(db.isConnected()){
			//System.out.println("Connected!");
			connected = true;
			setRowCount(countQuery);
			setData();
		}else{
			Dbg.syserr("Couldn't connect!");
		}
	}
	
	private void setRowCount() {
		//Replace query with a count
		int positionOfSelect = query.indexOf("SELECT");
		int positionOfFrom = query.indexOf("FROM");
		//selectStatement = query.substring(positionOfSelect+7,positionOfFrom);
		CavaResultSet rs;
		if((rs=db.select(query.substring(positionOfSelect,6) + " COUNT(*) " + query.substring(positionOfFrom)))!=null){
			if(rs.fetchRow()){
				numRows = rs.fetchInt(1);
				//System.out.println("Row count set at:" + numRows);
				if(numRows == -1){
					//System.out.println("Fetch int returned -1");
					numRows=0;
				}else{
					return;
				}
			}else{
				Dbg.syserr("Could not fetch row");
			}
		}else{
			Dbg.syserr("Could not execute query. Printing error:");
		}                          
		numRows = 0;
		//System.out.println("Row count set at:" + numRows);
	}
	
	private void setRowCount(String query){
		CavaResultSet rs;
		if((rs=db.select(query))!=null){
			if(rs.fetchRow()){
				numRows = rs.fetchInt(1);
				if(numRows == -1){
					//System.out.println("Fetch int returned -1");
					numRows=0;
				}else{
					return;
				}
			}else{
				//System.out.println("Could not fetch row");
			}
		}else{
			Dbg.syserr("Could not execute query. Printing error:");
		}
		numRows = 0;
		//System.out.println("Row number set at:" + numRows);
	}
	
	protected void setRowCount(int value){
		if(value > 0){
			numRows = value;
		}else{
			numRows = 0;
		}
	}
	
	protected boolean isConnected(){
		return connected;
	}
	
	private void setData(){
		data = new Object[numRows][columnNames.length];
		CavaResultSet rs;
		if((rs=db.select(query))!=null){
			int i = 0;
			while(rs.fetchRow()){
				int j= 0;
				for(String s: columnNames){
					String val = rs.fetchString(s);
					//System.out.println("Data being set to: " + val);
					data[i][j] = (val==null) ? " " : val;
					j++;
				}
				i++;
			}
		}
		
	}

	@Override
	public int getColumnCount() {
		if(columnNames != null){
			return columnNames.length;
		}else{
			return 0;
		}
	}

	@Override
	public int getRowCount() {
		return numRows;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if(data!=null){
			try{
				return data[rowIndex][columnIndex];
			}catch(Exception e){
				return " ";
			}
		}
		return " ";
	}
	
	public String getColumnName(int col){
		return columnNames[col];
	}
	

}
