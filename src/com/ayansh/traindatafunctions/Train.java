/**
 * Licensed to Varun Verma 
 */
/**
 * @author Varun Verma
 */
package com.ayansh.traindatafunctions;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.xml.sax.InputSource;

public class Train {
	
	private String train_number;
	private List<TrainStop> train_stops;
	private String schedule;
	
	public Train(String number) {
		// Constructor
		train_number = number.replaceAll("\\*", "");
		train_stops = new ArrayList<TrainStop>();

	}
	
	public List<TrainStop> saveScheduleToDB() throws Exception {
		
		String response_string = getScheduleFromNet();
		
		if(response_string.contentEquals("")){
			throw new Exception("Train Schedule could not be determined.");
		}
		
		// Prepare response string for parsing.
        response_string = response_string.replaceAll("<FONT COLOR = red>", "");
        int begin = response_string.indexOf(">Remark</TH>");
        response_string = response_string.substring(begin + 12);
        response_string = "<INFO><TR>" + response_string + "</INFO>";
        
        try{
        	
        	SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            SAXParser parser = parserFactory.newSAXParser();
        	
        	ParseTrainSchedule my_handler = new ParseTrainSchedule(train_number);
        	
        	InputSource is = new InputSource(new StringReader(response_string));
        	parser.parse(is, my_handler);
        	train_stops = my_handler.train_stops;
        }
        catch(Exception e){
        	throw e;
        }
        
		return train_stops;
                
	}
	
	private String getScheduleFromNet() throws Exception {
		
		// Create a new HttpClient and Post Header  
        HttpClient httpclient = new DefaultHttpClient();  
        HttpPost httppost = new HttpPost("http://www.indianrail.gov.in/cgi_bin/inet_trnnum_cgi.cgi");

        try {
    		
    		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);  
        	nameValuePairs.add(new BasicNameValuePair("lccp_trnname", train_number));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			httppost.addHeader("Referer", "http://www.indianrail.gov.in/train_Schedule.html");
			
			//Execute HTTP Post Request 
	    	HttpResponse response = httpclient.execute(httppost);
	    	
	    	if (response!= null){
	    		// Read
        		InputStream in = response.getEntity().getContent();
        		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        		boolean read = false, destination = false;
        		String line = null, response_string = "", html_content = "";
        		while((line = reader.readLine()) != null){
        			
        			html_content = html_content.concat(line);
        			html_content = html_content.concat("\n");
        			
        			if(line.contains("You Queried For")){
        				read = true;
        				line = "";
        			}
        			if(line.contains("<table width=")){
    					line = "<table border=" + '\"' + "1" + '\"' + ">";
    				}
        			if(line.contains("<table class=" + '\"' + "heading_table_top" + '\"')){
    					line = "<TR>";
    				}
        			if(line.contains("<TH width=")){
        				int to = line.indexOf(">");
    					line = line.replaceFirst(line.substring(3, to),"");
    				}
    				if(line.contains("Destination")){
        				destination = true;
        			}
    				if(line.contains("Slip Route")){
    					line = line.replaceFirst("</td>", "</TD>");
    				}
        			if(line.contains("</TABLE></td></tr>")){
        				if(destination){
        					read = false;
            				line = "</TABLE>";
        				}
        			}
        			
        			if(read){
        				
        				response_string = response_string.concat(line);
        				response_string = response_string.concat("\n");
        			}
        			
        		}
        		in.close();
        		
        		// Now parse this info.
        		set_schedule(response_string);
        		return response_string;
	    	}
	    	
        } catch (Exception e) {
			String error_message = "Error while fetching train schedule. Reason:" + e.getMessage();
			throw new Exception(error_message);
		}
        
		return "";
        
	}
	
	private void set_schedule(String response_string) {
		// Set Train Schedule
		if(response_string.contentEquals("")){
			response_string = "This train number is wrong.<br>OR<br>This train does not" +
					" run on this day.";
		}
		schedule = "<html><body>\n";
		schedule = schedule + response_string;
		schedule = schedule + "\n</body></html>";
	}
	
	public String get_train_number(){
		return train_number;
	}
	
	public String get_schedule_html(){
		return schedule;
	}
		
}