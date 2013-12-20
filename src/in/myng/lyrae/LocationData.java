package in.myng.lyrae;
import android.location.Location;

public class LocationData {
	private static final LocationData INSTANCE = new LocationData();
	private static Location currLocation = null;
	private LocationData(){}

	public Location getCurrLocation() {
		return currLocation;
	}

	public void setCurrLocation(Location currLocation) {
		LocationData.currLocation = currLocation;
	}
	
    public static LocationData getInstance() {
        return INSTANCE;
    }
	
}
