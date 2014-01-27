/**
 * 
 */
package com.ayansh.traindatafunctions;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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

	public void correctBookCancInfo() throws SQLException {
		
		List<AvailabilityInfo> masterList = db.getMasterList();
		
		Iterator<AvailabilityInfo> i = masterList.iterator();
		
		while(i.hasNext()){
			
			AvailabilityInfo ml = i.next();
			
			int racQuota = db.getRACQuota(ml);
			
			List<AvailabilityInfo> availInfo = db.getAvailabilityInfo(ml);
			
			ListIterator<AvailabilityInfo> iter = availInfo.listIterator();
			
			while(iter.hasNext()){
				
				AvailabilityInfo ai = iter.next();
				//System.out.println("Processing Record No: " + ai.RecordNo);
				
				if(ai.Availability.contains("Charting") || ai.Availability.contains("TRAIN")){
					continue;
				}
				
				// Calculate Avail on Absolute Scale !
				if(ai.grossAvType.contentEquals("RAC")){
					ai.grossAvCount = 0 - ai.grossAvCount;
				}
				
				if(ai.grossAvType.contentEquals("WL")){
					ai.grossAvCount = 0 - racQuota - ai.grossAvCount;
				}
				
				if(ai.netAvType.contentEquals("RAC")){
					ai.netAvCount = 0 - ai.netAvCount;
				}
				
				if(ai.netAvType.contentEquals("WL")){
					ai.netAvCount = 0 - racQuota - ai.netAvCount;
				}
				
				// -1 becoz after next we already moved to next index and prev will give current only
				int prevIndex = iter.previousIndex() - 1;
				
				if(prevIndex >= 0){
					// WE found prev data
					AvailabilityInfo prevAI = availInfo.get(prevIndex);
					
					if(ai.grossAvType.contentEquals("REG")){
						
						int tempNetAvCount = ai.netAvCount - prevAI.netAvCount;
						if(tempNetAvCount > 0){
							// More booking happened
							ai.grossAvCount = prevAI.grossAvCount;
						}
						else{
							// more Cancellations happened
							ai.grossAvCount = prevAI.grossAvCount + tempNetAvCount;
						}
						
					}
					
					// Calculate Bookings
					if(prevAI.grossAvCount > ai.grossAvCount){
						ai.bookings = prevAI.grossAvCount - ai.grossAvCount;
					}
					else{
						ai.bookings = 0;
					}
					
					// Calculate Cancellations
					if(prevAI.grossAvCount < ai.grossAvCount){
						ai.cancellations = ai.grossAvCount - prevAI.grossAvCount;
					}
					else{
						ai.cancellations = (prevAI.grossAvCount - ai.grossAvCount) - (prevAI.netAvCount - ai.netAvCount);
					}
					
				}
				else{
					// We did not find prev data. This is first data
					ai.bookings = ai.cancellations = 0;
					
					if(ai.grossAvType.contentEquals("REG")){
						ai.grossAvCount = 0 - racQuota - 300;
					}
				}
				
				// Special Case
				if(ai.bookings < 0){
					ai.bookings = 0;
				}
				
				if(ai.cancellations < 0){
					ai.cancellations = 0;
				}
				
				// Now Save
				db.saveAvailabilityInfo(ai);
				
				System.out.println("Record No: " + ai.RecordNo + " is updated");
				
			}
			
		}
		
	}
}
