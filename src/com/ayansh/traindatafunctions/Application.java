/**
 * 
 */
package com.ayansh.traindatafunctions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
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
			user = "appuser";
			pwd = "x8w4ySzIV";
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

	public void correctBookCancInfo(JSONObject input) throws SQLException {
		
		String trainNo = input.getString("TrainNo");
		
		List<AvailabilityInfo> masterList = db.getMasterList(trainNo);
		
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

	public void updateActualPNRStatus(JSONObject input) throws SQLException {
		
		// Get Query History
		HashMap<Integer,String> pnrList = db.getPNRList();
		
		Iterator<Integer> i = pnrList.keySet().iterator();
		
		while(i.hasNext()){
			
			int id = i.next();
			String pnr = pnrList.get(id);
			
			// Get PNR Status
			
			try {
				
				JSONObject pnrData = getPNRStatus(pnr);
				String currentStatus = pnrData.getString("CurrentStatus");
				
				db.updateQueryHistory(id,currentStatus);
				
			} catch (Exception e) {
				// Its OK. Proceed with next
				System.out.println("Error fetching PNR Status of: " + pnr + ". Error: " + e.getMessage());
			}
				
		}
		
	}

	public JSONObject getPNRStatus(String pnr) throws Exception {
		
		String response_string = "";
		String[] result1, result2;
		
		String url = getPNREnquiryURL();
		
		// Create a new HttpClient and Post Header
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(url);

		// Try to Post the PNR
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("lccp_pnrno1", pnr));
		nameValuePairs.add(new BasicNameValuePair("lccp_cap_val", "51213"));
		nameValuePairs.add(new BasicNameValuePair("lccp_capinp_val", "51213"));
		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		httppost.setHeader("Referer","http://www.indianrail.gov.in/pnr_Enq.html");

		// Execute HTTP Post Request
		HttpResponse response = httpclient.execute(httppost);

		if (response != null) {
			// Read

			InputStream in = response.getEntity().getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			
			boolean read1 = false, read2 = false;
			String line = null, response_string1 = "", response_string2 = "";
			while ((line = reader.readLine()) != null) {

				response_string = response_string.concat(line);
				response_string = response_string.concat("\n");

				if (line.contains("PNR Number :")) {
					read1 = true;
				}
				if (line.contains("<FORM NAME=\"RouteInfo\" METHOD=\"POST\"")) {
					read1 = false;
				}
				if (line.contains("Get Schedule")) {
					read2 = true;
					line = "";
				}
				if (line.contains("Charting Status")) {
					read2 = false;
				}
				if (read1) {
					response_string1 = response_string1.concat(line);
					response_string1 = response_string1.concat("\n");
				}
				if (read2) {
					if (line.contains("<font size=1>")) {
						line = "";
					}
					if (line.contains("<TABLE width=")) {
						line = "<table border=" + '\"' + "1" + '\"' + ">";
					}
					if (line.contains("<td width=")) {
						line = line.replaceFirst(line.substring(3, 15), "");
					}
					response_string2 = response_string2.concat(line);
					response_string2 = response_string2.concat("\n");
				}
			}

			in.close();

			String identifier = "<TD class=\"table_border_both\">";
			result1 = response_string1.split(identifier, 10);
			result2 = response_string2.split(identifier, 10);

			int index = 1;
			int size = result1.length;
			while (index < size) {
				result1[index] = result1[index].replaceAll("</TD>", "");
				result1[index] = result1[index].replaceAll("\n", "");
				result1[index] = result1[index].trim();
				index++;
			}
			index = 1;
			size = result2.length;
			while (index < size) {
				result2[index] = result2[index].replaceAll("</TD>", "");
				result2[index] = result2[index].replaceAll("\n", "");
				result2[index] = result2[index].replaceAll("<TR>", "");
				result2[index] = result2[index].replaceAll("</TR>", "");
				result2[index] = result2[index].replaceAll("<B>", "");
				result2[index] = result2[index].replaceAll("</B>", "");
				result2[index] = result2[index].trim();
				index++;
			}
			
			String train_number = result1[1].replaceAll("\\*", "");
			String from = result1[4];
			String to = result1[5];
			String date = result1[3];
			date = date.replaceAll(" ", "");
			
			String currentStatus = result2[3];
			String travelClass = result1[8];
			travelClass = travelClass.replaceAll(" ", "");
			travelClass = travelClass.substring(0, 2);
			
			// remove all spaces
			currentStatus = currentStatus.replaceAll(" ", "");
			
			if(currentStatus.contains("W/L")){
				currentStatus = currentStatus.replace("W/L", "WL");
			}
			
			JSONObject pnrData = new JSONObject();
			pnrData.put("TrainNo", train_number);
			pnrData.put("FromStation", from);
			pnrData.put("ToStation", to);
			pnrData.put("TravelDate", date);
			pnrData.put("CurrentStatus", currentStatus);
			pnrData.put("TravelClass", travelClass);
			
			return pnrData;
		}
		
		return null;
		
	}

	public void updatePNREnquiryURL() throws Exception {
		
		// Create a new HttpClient and Post Header
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet("http://www.indianrail.gov.in/pnr_Enq.html");
		
		// Execute HTTP Post Request
		HttpResponse response = httpclient.execute(httpget);

		// Open Stream for Reading.
		InputStream is = response.getEntity().getContent();

		// Get Input Stream Reader.
		InputStreamReader isr = new InputStreamReader(is);

		BufferedReader reader = new BufferedReader(isr);
		
		String url = null;
		String line = null;
		while ((line = reader.readLine()) != null) {
			
			if (line.contains("<form id=") &&
				line.contains("http://www.indianrail.gov.in/cgi_bin/") &&
				line.contains("pnr_stat")){
				
				int begin = line.indexOf("action=");
				int end = line.indexOf(".cgi", begin);
				
				url = line.substring(begin + 8, end + 4);
				break;
				
			}
			
		}
		
		if(url == null){
			throw new Exception("Could not read URL");
		}
		
		if(url != null){
			
			db.updatePNREnquiryURL(url);
			
		}
		
	}

	private String getPNREnquiryURL() throws Exception {
		
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet("http://ayansh.com/pnr-prediction/get_pnr_enquiry_url.php");
		
		// Execute HTTP Post Request
		HttpResponse response = httpclient.execute(httpget);

		// Open Stream for Reading.
		InputStream is = response.getEntity().getContent();

		// Get Input Stream Reader.
		InputStreamReader isr = new InputStreamReader(is);

		BufferedReader reader = new BufferedReader(isr);

		StringBuilder builder = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
		}
		
		return builder.toString();
		
	}

}