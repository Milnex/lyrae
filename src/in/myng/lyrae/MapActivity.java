package in.myng.lyrae;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public class MapActivity extends Activity {
  private static final int HF_MINUTES = 1000 * 30;
  protected static final int MAPUPDATEIDENTIFIER = 0x101;  
  static final LatLng HAMBURG = new LatLng(53.558, 9.927);
  static final LatLng KIEL = new LatLng(53.551, 9.993);
  private GoogleMap map;
  
  static LocationManager locationManager = null;
  static LatLng myPosition = null;
  static Location currLocation = null;
  
  static Map<String,LinkedList<LatLng>> userLocations = new HashMap<String,LinkedList<LatLng>>();
  static LocationListener locationListener = null;
 
  Handler myHandler = new Handler() {  
      public void handleMessage(Message msg) {   
           switch (msg.what) {   
                case MapActivity.MAPUPDATEIDENTIFIER:   
                	Iterator<Entry<String, LinkedList<LatLng>>> iter = userLocations.entrySet().iterator();
                	float color = 0;
                	while(iter.hasNext()){
                		Entry<String, LinkedList<LatLng>> entry = (Entry<String, LinkedList<LatLng>>) iter.next();
                		String userName = entry.getKey();
                		LinkedList<LatLng> postions = (LinkedList<LatLng>) entry.getValue();
                		map.clear();
                		map.addMarker(new MarkerOptions().position(postions.getLast())
                				.title(userName)
                				.icon(BitmapDescriptorFactory.defaultMarker(color)));
                		color+=30;
                	}
                    break;   
           }
           super.handleMessage(msg);   
      }   
  };

  class UpdateServerThread implements Runnable {   
      public void run() {  
           while (!Thread.currentThread().isInterrupted()) {    
                /*
                 *  Send My Location to server
                 *  Get Locations back
                 *  Update Structure and map
                 */
        	    
        	            	   
                Message message = new Message();   
                message.what = MapActivity.MAPUPDATEIDENTIFIER;   
                MapActivity.this.myHandler.sendMessage(message);   
                try {   
                     Thread.sleep(5000);    
                } catch (InterruptedException e) {   
                     Thread.currentThread().interrupt();   
                }   
           }   
      }   
 }  

  @SuppressLint("NewApi")  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_map);
    map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
        .getMap();
    
    /*
     * Get Group information from parent activity
     * Get username from parent activity
     */
    Intent intent = getIntent();
    //String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
    
    // Test Marker
    Marker hamburg = map.addMarker(new MarkerOptions().position(HAMBURG)
        .title("Hamburg"));

    // Move the camera instantly to hamburg with a zoom of 15.
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(HAMBURG, 15));

    // Zoom in, animating the camera.
    map.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);
    
    // Start to get my location
    this.startLocationListener();
    
    openGPS();
    
    // Start update thread
    UpdateServerThread upThread = new UpdateServerThread(); 
    Thread t = new Thread(upThread);
    t.start();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.display, menu);
		return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      case android.R.id.home:
          NavUtils.navigateUpFromSameTask(this);
          return true;
      }
      return super.onOptionsItemSelected(item);
  }
  
  @Override
  public void onResume(){
	  super.onResume();
  }
  
  @Override
  public void onPause(){
	  super.onPause();
	  locationManager.removeUpdates(locationListener);
  }
  
  private void startLocationListener() {
	    // Get the location manager
	    locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

	    locationListener = new LocationListener() {
	        public void onLocationChanged(Location location) {
	            // Called when a new location is found by the network location provider.
	            if(currLocation != null)
	            {
	                boolean better = isBetterLocation(location, currLocation);
	                if(better) {
	                    currLocation.set(location);
	                    myPosition = new LatLng(currLocation.getLatitude(), currLocation.getLongitude());
	                }
	            }
	            else
	            {
	                currLocation = location;
	                myPosition = new LatLng(currLocation.getLatitude(), currLocation.getLongitude());
	                map.addMarker(new MarkerOptions().position(myPosition));
	            }
	            map.moveCamera(CameraUpdateFactory.newLatLngZoom(myPosition, 15));
	        }

	        public void onStatusChanged(String provider, int status, Bundle extras) {}

	        public void onProviderEnabled(String provider) {}

	        public void onProviderDisabled(String provider) {}
	    };
	    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
	    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, locationListener);
	}
  
  /** Determines whether one Location reading is better than the current Location fix
    * @param location  The new Location that you want to evaluate
    * @param currentBestLocation  The current Location fix, to which you want to compare the new one
    */
  protected boolean isBetterLocation(Location location, Location currentBestLocation) {
      if (currentBestLocation == null) {
          // A new location is always better than no location
          return true;
      }

      // Check whether the new location fix is newer or older
      long timeDelta = location.getTime() - currentBestLocation.getTime();
      boolean isSignificantlyNewer = timeDelta > HF_MINUTES;
      boolean isSignificantlyOlder = timeDelta < -HF_MINUTES;
      boolean isNewer = timeDelta > 0;

      // If it's been more than two minutes since the current location, use the new location
      // because the user has likely moved
      if (isSignificantlyNewer) {
          return true;
      // If the new location is more than two minutes older, it must be worse
      } else if (isSignificantlyOlder) {
          return false;
      }

      // Check whether the new location fix is more or less accurate
      int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
      boolean isLessAccurate = accuracyDelta > 0;
      boolean isMoreAccurate = accuracyDelta < 0;
      boolean isSignificantlyLessAccurate = accuracyDelta > 200;

      // Check if the old and new location are from the same provider
      boolean isFromSameProvider = isSameProvider(location.getProvider(),
              currentBestLocation.getProvider());

      // Determine location quality using a combination of timeliness and accuracy
      if (isMoreAccurate) {
          return true;
      } else if (isNewer && !isLessAccurate) {
          return true;
      } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
          return true;
      }
      return false;
  }

  /** Checks whether two providers are the same */
  private boolean isSameProvider(String provider1, String provider2) {
      if (provider1 == null) {
        return provider2 == null;
      }
      return provider1.equals(provider2);
  }
  
  private void openGPS() {       
	  LocationManager mlocManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

      if (!mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
          AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
          alertDialogBuilder
                  .setMessage("GPS service is disabled in your device. Enable it?")
                  .setCancelable(false)
                  .setPositiveButton("Enable GPS",
                          new DialogInterface.OnClickListener() {
                              public void onClick(DialogInterface dialog,
                                      int id) {
                                  Intent callGPSSettingIntent = new Intent(
                                          android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                  startActivity(callGPSSettingIntent);
                              }
                          });
          alertDialogBuilder.setNegativeButton("Cancel",
                  new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int id) {
                          dialog.cancel();
                      }
                  });
          AlertDialog alert = alertDialogBuilder.create();
          alert.show();
      }
  }
} 
