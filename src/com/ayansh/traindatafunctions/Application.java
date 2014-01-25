/**
 * 
 */
package com.ayansh.traindatafunctions;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.json.JSONObject;


/**
 * @author varun
 * 
 */
public class Application {

	private static Application app;
	private Properties properties;
	private DBServer db;
	
	public static Application getInstance() {

		if (app == null) {
			app = new Application();
		}

		return app;

	}

	private Application() {
		
		properties = new Properties();
		
	}
	
	public void initializeApplication() throws IOException, SQLException{
		
		properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
		
		// Set up DB Connection
		db = new MySQLDB("jdbc:mysql://" + properties.getProperty("mysql_server"));
		
		String user, pwd;
		
		user = properties.getProperty("db_user");
		pwd = properties.getProperty("pwd");
		
		if(user == null || user.contentEquals("")){
			user = "admin_GUser";
			pwd = "PaHxvQ0TJC2L";
		}
		
		db.setUpConnection(user,pwd);
		
	}
	
	public Properties getApplicationProperties(){
		return properties;
	}

	public void close() {

		try {
			db.close();
		} catch (SQLException e) {
			throw new IllegalArgumentException();
		}
	}

	public void fetchTrainStops(JSONObject input) throws Exception {
		
		String trainNo = input.getString("TrainNo");
		
		Train train = new Train(trainNo);
		
		List<TrainStop> trainStops = train.saveScheduleToDB();
		
		db.saveTrainStops(trainStops);
		
	}

	public void deleteTrainStops(JSONObject input) throws SQLException {
		
		String trainNo = input.getString("TrainNo");
		
		db.deleteTrainStops(trainNo);
		
	}

	public List<TrainStop> getTrainStops(String trainNo) throws SQLException {
		
		return db.getTrainStops(trainNo);
		
	}

	public void fetchAllTrainStops(JSONObject input) throws SQLException, InterruptedException {
		
		int max = input.getInt("Max");
		
		List<String> trainList = db.getPendingTrainList(max);
		
		Iterator<String> i = trainList.iterator();
		
		while(i.hasNext()){
			
			JSONObject inp = new JSONObject();
			inp.put("TrainNo", i.next());
			
			try {
				
				fetchTrainStops(inp);
				
				System.out.println("Fetch completed for " + inp);
				
			} catch (Exception e) {
				System.out.println("Error while Fetching for " + inp);
			}
			
			Thread.sleep(15 * 1000);	//15 seconds
			
		}
		
	}
}
