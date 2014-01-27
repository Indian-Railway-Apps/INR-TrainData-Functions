package com.ayansh.traindatafunctions;

import java.sql.SQLException;
import java.util.List;

public interface DBServer {

	public void setUpConnection(String user, String pwd) throws SQLException;

	public void close() throws SQLException;

	public void saveTrainStops(List<TrainStop> trainStops) throws SQLException;

	public void deleteTrainStops(String trainNo) throws SQLException;

	public List<TrainStop> getTrainStops(String trainNo) throws SQLException;

	public List<String> getPendingTrainList(int max) throws SQLException;

	public List<AvailabilityInfo> getMasterList() throws SQLException;

	public List<AvailabilityInfo> getAvailabilityInfo(AvailabilityInfo ai) throws SQLException;

	public int getRACQuota(AvailabilityInfo ml) throws SQLException;

	public void saveAvailabilityInfo(AvailabilityInfo ai) throws SQLException;

}