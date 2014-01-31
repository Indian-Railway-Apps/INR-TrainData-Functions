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

	@Override
	public List<String> getPendingTrainList(int max) throws SQLException {

		List<String> trainList = new ArrayList<String>();
		
		Statement stmt = mySQL.createStatement();
		
		String sql = "SELECT t.TrainNo from ValidTrains as t LEFT OUTER JOIN TrainStops as s "
				+ "on t.TrainNo = s.TrainNo where s.TrainNo is null and t.Active = 'X' limit " + max;

		ResultSet result = stmt.executeQuery(sql);
		
		if(result.next()){
			
			do{
				
				trainList.add(result.getString(1));
				
			}while(result.next());
			
		}
		
		result.close();
		
		return trainList;
	}

	@Override
	public List<AvailabilityInfo> getMasterList(String trainNo) throws SQLException {
		
		List<AvailabilityInfo> availInfo = new ArrayList<AvailabilityInfo>();
		
		Statement stmt = mySQL.createStatement();
		
		String sql;
		
		if (trainNo.contentEquals("")) {
			sql = "SELECT DISTINCT TrainNo, TravelDate, Class FROM AvailabilityInfo";
		} else {
			sql = "SELECT DISTINCT TrainNo, TravelDate, Class FROM AvailabilityInfo WHERE TrainNo = '"
					+ trainNo + "'";
		}

		ResultSet result = stmt.executeQuery(sql);
		
		if(result.next()){
			
			do{
				
				AvailabilityInfo ai = new AvailabilityInfo();
				ai.TrainNo = result.getString(1);
				ai.JourneyDate = result.getString(2);
				ai.Class = result.getString(3);
				availInfo.add(ai);
				
			}while(result.next());
			
		}
		
		result.close();
		
		return availInfo;
	}
	
	@Override
	public List<AvailabilityInfo> getAvailabilityInfo(AvailabilityInfo ai) throws SQLException {
		
		List<AvailabilityInfo> availInfo = new ArrayList<AvailabilityInfo>();
		
		Statement stmt = mySQL.createStatement();
		
		String sql = "SELECT RecordNo, TrainNo, TravelDate, LookupDate, Class, Availability "
				+ "FROM AvailabilityInfo where TrainNo = '"
				+ ai.TrainNo
				+ "' AND TravelDate = '"
				+ ai.JourneyDate
				+ "' AND Class = '"
				+ ai.Class + "' ORDER BY LookupDate ASC";

		ResultSet result = stmt.executeQuery(sql);
		
		if(result.next()){
			
			do{
				
				AvailabilityInfo ainf = new AvailabilityInfo();
				ainf.RecordNo = result.getInt(1);
				ainf.TrainNo = result.getString(2);
				ainf.JourneyDate = result.getString(3);
				ainf.LookupTimeStamp = result.getString(4);
				ainf.Class = result.getString(5);
				ainf.setAvailability(result.getString(6));
				availInfo.add(ainf);
				
			}while(result.next());
			
		}
		
		result.close();
		
		return availInfo;
	}

	@Override
	public int getRACQuota(AvailabilityInfo ai) throws SQLException {
		
		int racQuota = 0;
		
		Statement stmt = mySQL.createStatement();
		
		String sql = "SELECT RACQuota FROM TrainQuota where TrainNo = '"
				+ ai.TrainNo
				+ "' AND Class = '"
				+ ai.Class + "'";

		ResultSet result = stmt.executeQuery(sql);
		
		if(result.next()){
			
			racQuota = result.getInt(1);
			
		}
		
		result.close();
		
		return racQuota;
	}

	@Override
	public void saveAvailabilityInfo(AvailabilityInfo ai) throws SQLException {
		
		Statement st = (Statement) mySQL.createStatement();

		String sql = "UPDATE AvailabilityInfo SET GrossAvType = '"
				+ ai.grossAvType + "', GrossAvCount = " + ai.grossAvCount
				+ ", NetAvType = '" + ai.netAvType + "', NetAvCount = "
				+ ai.netAvCount + ", Bookings = " + ai.bookings
				+ ", Cancellations = " + ai.cancellations
				+ " WHERE RecordNo = " + ai.RecordNo;

		st.executeUpdate(sql);
		
	}

}