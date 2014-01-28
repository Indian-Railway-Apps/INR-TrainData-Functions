/**
 * 
 */
package com.ayansh.traindatafunctions;

import java.sql.SQLException;

import org.json.JSONObject;

/**
 * @author Varun Verma
 *
 */
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// Create Application Instance
		Application app = Application.getInstance();

		try {

			app.initializeApplication();

		} catch (Exception e) {
			System.out.println(e.getMessage());
			finish();
		}
		
		// Check if we have valid input
		if(args.length != 2){
			// ! No input
			System.out.println("Invalid input");
			finish();
		}
		
		String code = args[0];
		
		JSONObject input = new JSONObject(args[1]);
		
		if(code.contentEquals("FetchAllTrainStops")){
			
			try {
				app.fetchAllTrainStops(input);
			} catch (SQLException | InterruptedException e) {
				System.out.println("Error Occured!");
				System.out.println(e.getMessage());
				finish();
			}
			
		}
		
		if(code.contentEquals("FetchTrainStops")){
			
			try {
				app.fetchTrainStops(input);
			} catch (Exception e) {
				System.out.println("Error Occured!");
				System.out.println(e.getMessage());
				finish();
			}
		}
		
		if(code.contentEquals("DeleteTrainStops")){
			
			try {
				app.deleteTrainStops(input);
			} catch (SQLException e) {
				System.out.println("Error Occured!");
				System.out.println(e.getMessage());
				finish();
			}
		}
		
		if(code.contentEquals("CorrectBookCancInfo")){
			
			try {
				app.correctBookCancInfo(input);
				System.out.println("Update Completed");
			} catch (SQLException e) {
				System.out.println("Error Occured!");
				System.out.println(e.getMessage());
				finish();
			}
			
		}
		
	}

	private static void finish() {
		
		Application app = Application.getInstance();

		app.close();

		System.exit(0);
		
	}

}