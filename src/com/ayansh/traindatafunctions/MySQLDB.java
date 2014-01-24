package com.ayansh.traindatafunctions;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MySQLDB implements DBServer {
	
	private Connection mySQL;
	private String dbURL;
	
	public MySQLDB(String dbURL){
		this.dbURL = dbURL;
	}
	
	@Override
	public void setUpConnection(String user, String pwd) throws SQLException {
		
		mySQL = DriverManager.getConnection(dbURL, user, pwd);
		
	}

	@Override
	public void close() throws SQLException {
		
		if(mySQL != null){
			mySQL.close();
		}
	}

	@Override
	public void saveTrainStops(List<TrainStop> trainStops) throws SQLException {
		
		Iterator<TrainStop> i = trainStops.iterator();
		
		while(i.hasNext()){
			
			TrainStop trainStop = i.next();

			Statement st = (Statement) mySQL.createStatement();

			String sql = "INSERT INTO TrainStops VALUES ('" + trainStop.trainNo
					+ "','" + trainStop.station_code + "',"
					+ trainStop.station_number + ")";

			st.executeUpdate(sql);
		}
		
	}

	@Override
	public void deleteTrainStops(String trainNo) throws SQLException {
		
		Statement st = (Statement) mySQL.createStatement();

		String sql = "DELETE FROM TrainStops WHERE TrainNo = '" + trainNo + "'";

		st.executeUpdate(sql);
		
	}

	@Override
	public List<TrainStop> getTrainStops(String trainNo) throws SQLException {
		
		List<TrainStop> trainStops = new ArrayList<TrainStop>();
		
		Statement stmt = mySQL.createStatement();
		
		String sql = "SELECT * FROM TrainStops WHERE TrainNo = '" + trainNo + "'";

		ResultSet result = stmt.executeQuery(sql);
		
		if(result.next()){
			
			do{
				
				TrainStop trainStop = new TrainStop(result.getString(1));
				trainStop.station_code = result.getString(2);
				trainStop.station_number = String.valueOf(result.getInt(3));
				trainStops.add(trainStop);
				
			}while(result.next());
			
		}
		
		result.close();
		
		return trainStops;
	}

}