/**
 * Licensed to Varun Verma 
 */
/**
 * @author Varun Verma
 */
package com.ayansh.traindatafunctions;

public class TrainStop {
	
	String station_number, station_code, station_name, dep_time, arrival_time;
	String sched_arrival, sched_dep, actual_arrival, actual_dep, route_no;
	String halt, distance;
	int day;
	String trainNo;
	
	public TrainStop(String tno){
		station_code = "";
		trainNo = tno;
	}
	
}