package com.ayansh.traindatafunctions.test;

import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.List;

import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ayansh.traindatafunctions.Application;
import com.ayansh.traindatafunctions.TrainStop;

public class ApplicationTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		
		// Set up.
		Application app = Application.getInstance();
		app.initializeApplication();
	}

	@After
	public void tearDown() throws Exception {
		
		// Finish Testing.
		Application.getInstance().close();
	}

	@Test
	public final void testFetchTrainStops() {
		
		JSONObject input = new JSONObject();
		input.put("TrainNo", "12627");
		
		Application app = Application.getInstance();
		
		try {
			
			app.fetchTrainStops(input);
			
			List<TrainStop> trainStops = app.getTrainStops("12627");
			if(trainStops.size() < 3){
				fail("Could not fetch Train Stops");
			}
			
		} catch (Exception e) {
			fail(e.getMessage());
		}
		
	}

	@Test
	public final void testDeleteTrainStops() {
	
		JSONObject input = new JSONObject();
		input.put("TrainNo", "12627");
		
		Application app = Application.getInstance();
		
		try {
			
			app.deleteTrainStops(input);
			
			List<TrainStop> trainStops = app.getTrainStops("12627");
			if(trainStops.size() > 0){
				fail("Could not delete Train Stops");
			}
			
		} catch (SQLException e) {
			fail(e.getMessage());
		}
		
	}
	
	@Test
	public final void testPNREnquiryURL() {
		
		Application app = Application.getInstance();
		
		try{
			
			app.updatePNREnquiryURL();
			
		} catch (Exception e) {
			fail(e.getMessage());
		}
		
	}

}
