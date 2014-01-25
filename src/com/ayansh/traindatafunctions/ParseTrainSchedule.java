/**
 * Licensed to Varun Verma 
 */
/**
 * @author Varun Verma
 */

package com.ayansh.traindatafunctions;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ParseTrainSchedule extends DefaultHandler {
	
	private boolean reading;
	List<TrainStop> train_stops;
	TrainStop train_stop;
	String name, value, data;
	int index;
	private String trainNo;
	
	public ParseTrainSchedule(String tno){
		train_stops = new ArrayList<TrainStop>();
		trainNo = tno;
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes){
		
		if(qName.equals("TD")
		  ){
			reading = true;
			data = "";
		}
		else if(qName.equals("TR")){
			train_stop = new TrainStop(trainNo);
			reading = false;
			index = 0;
		}
		else {
			reading = false;
		}
		
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException{
		
		if (qName.equals("TD")){
			value = data.replaceAll("  ", "");
			data = "";
			switch (index){
			case 0: train_stop.station_number = value; break;
			case 1: train_stop.station_code = value; break;
			case 2: train_stop.station_name = value; break;
			case 3: train_stop.route_no = value; break;
			case 4: train_stop.arrival_time = value; break;
			case 5: train_stop.dep_time = value; break;
			case 6: train_stop.halt = value; break;
			case 7: train_stop.distance = value; break;
			case 8: train_stop.day = Integer.parseInt(value); break;
			case 9: break;
			}
			index++;
		}
		else if (qName.equals("TR")){
			if(!train_stop.station_code.equals("")){
				train_stops.add(train_stop);
			}
		}
		else {
			
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length){
		
		if (reading){
			
			int i = 0;
			int index = start;
			do{
				data = data + ch[index];
				i = i+1;
				index = index + 1;
			}while(i<length);
		}
	}
}