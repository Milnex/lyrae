package in.myng.lyrae;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;

import data.JsonUser;


public class MapActivity extends Activity {
  protected static final int MAPUPDATEIDENTIFIER = 0x101;  
  static final LatLng HAMBURG = new LatLng(53.558, 9.927);
  static final LatLng KIEL = new LatLng(53.551, 9.993);
  static String curgid = "";
  static String curuid = "12345";
  static String url = "http://ec2-54-204-122-234.compute-1.amazonaws.com:3000";
  static boolean first = true;
  private GoogleMap map;
  
  static LocationData locationData = LocationData.getInstance();
  static JsonUser[] groupUsers;
  LatLng origin;
  LatLng dest;
 
  Handler myHandler = new Handler() {  
      public void handleMessage(Message msg) {   
           switch (msg.what) {   
                case MapActivity.MAPUPDATEIDENTIFIER:
                	float color = 30;
                	if(groupUsers.length>0)
                		map.clear();   
                	int i=0;
                	while(i<groupUsers.length){
                		if(groupUsers[i].uid.equals(curuid)){
                			origin = groupUsers[i].location;
                			map.addMarker(new MarkerOptions().position(groupUsers[i].location)
                    				.title(groupUsers[i].name)
                    				.icon(BitmapDescriptorFactory.defaultMarker(0)))
                    				.showInfoWindow();
                			// Move the camera instantly to hamburg with a zoom of 15.
                			if(first){
                				map.moveCamera(CameraUpdateFactory.newLatLngZoom(groupUsers[i].location,18));
                				first=false;
                			}
                			else
                				map.moveCamera(CameraUpdateFactory.newLatLng(groupUsers[i].location));
                		}
                		else {
                			dest = groupUsers[i].location;
	                		map.addMarker(new MarkerOptions().position(groupUsers[i].location)
	                				.title(groupUsers[i].name)
	                				.icon(BitmapDescriptorFactory.defaultMarker(color)));
	                		color+=30;
                		}
                		i++;
                	}
                	 // Getting URL to the Google Directions API
                    String url = MapActivity.this.getDirectionsUrl(origin, dest);
                    DownloadTask downloadTask = new DownloadTask();
 
                    // Start downloading json data from Google Directions API
                    downloadTask.execute(url);
                    break;
           }
           super.handleMessage(msg);   
      }   
  };

  class UpdateServerThread implements Runnable {   
      public void run() {  
           while (!Thread.currentThread().isInterrupted()) {         	            	   
                Message message = new Message();   
                message.what = MapActivity.MAPUPDATEIDENTIFIER;   
                try {
                	HttpClient client = new DefaultHttpClient();
					HttpGet get = new HttpGet(url + "/group/" + curgid);
					HttpResponse response = client.execute(get);
					BufferedReader rd = new BufferedReader(
							new InputStreamReader(response.getEntity()
									.getContent()));
					String line = "";
					String result = "";
					while ((line = rd.readLine()) != null) {
						result += line;
						System.out.println(line);
					}
					Log.e("T_Group", result);
					
					Gson gson = new Gson();
					groupUsers = gson.fromJson(result, JsonUser[].class);
					myHandler.sendMessage(message);
                } catch (Exception e) {
                     Thread.currentThread().interrupt();   
                }
                try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
           }   
      }   
 }  

  @SuppressLint("NewApi")  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_map);
    /*
     * Get Group information from parent activity
     * Get username from parent activity
     */
    Intent intent = getIntent();
	curgid = intent.getStringExtra(MainActivity.GROUP_MESSAGE);
	curuid = intent.getStringExtra(MainActivity.UID_MESSAGE);
    map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
        .getMap();
    
    //String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
    
//    // Test Marker
//    Marker hamburg = map.addMarker(new MarkerOptions().position(HAMBURG)
//        .title("Hamburg"));
//
//    // Move the camera instantly to hamburg with a zoom of 15.
//    map.moveCamera(CameraUpdateFactory.newLatLngZoom(HAMBURG, 15));
//
//    // Zoom in, animating the camera.
//    map.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);
//    
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
	  first=true;
  }
  
  @Override
  public void onPause(){
	  super.onPause();
	  first=true;
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
  
  private String getDirectionsUrl(LatLng origin, LatLng dest)
  {
      // Origin of route
      String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

      // Destination of route
      String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

      // Sensor enabled
      String sensor = "sensor=false";

      // Building the parameters to the web service
      String parameters = str_origin + "&" + str_dest + "&" + sensor+"&units=metric&mode=walking";

      // Output format
      String output = "json";

      // Building the url to the web service
      String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

      return url;
  }

  /** A method to download json data from url */
  private String downloadUrl(String strUrl) throws IOException
  {
      String data = "";
      InputStream iStream = null;
      HttpURLConnection urlConnection = null;
      try
      {
          URL url = new URL(strUrl);

          // Creating an http connection to communicate with url
          urlConnection = (HttpURLConnection) url.openConnection();

          // Connecting to url
          urlConnection.connect();

          // Reading data from url
          iStream = urlConnection.getInputStream();

          BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

          StringBuffer sb = new StringBuffer();

          String line = "";
          while ((line = br.readLine()) != null)
          {
              sb.append(line);
          }

          data = sb.toString();

          br.close();

      } catch (Exception e)
      {
          Log.d("Exception while downloading url", e.toString());
      } finally
      {
          iStream.close();
          urlConnection.disconnect();
      }
      return data;
  }

  // Fetches data from url passed
  private class DownloadTask extends AsyncTask<String, Void, String>
  {
      // Downloading data in non-ui thread
      @Override
      protected String doInBackground(String... url)
      {

          // For storing data from web service
          String data = "";

          try
          {
              // Fetching the data from web service
              data = MapActivity.this.downloadUrl(url[0]);
          } catch (Exception e)
          {
              Log.d("Background Task", e.toString());
          }
          return data;
      }

      // Executes in UI thread, after the execution of
      // doInBackground()
      @Override
      protected void onPostExecute(String result)
      {
          super.onPostExecute(result);

          ParserTask parserTask = new ParserTask();

          // Invokes the thread for parsing the JSON data
          parserTask.execute(result);

      }
  }

  /** A class to parse the Google Places in JSON format */
  private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>>
  {

      // Parsing the data in non-ui thread
      @Override
      protected List<List<HashMap<String, String>>> doInBackground(String... jsonData)
      {
          JSONObject jObject;
          List<List<HashMap<String, String>>> routes = null;

          try
          {
              jObject = new JSONObject(jsonData[0]);
              DirectionsJSONParser parser = new DirectionsJSONParser();

              // Starts parsing data
              routes = parser.parse(jObject);
          } catch (Exception e)
          {
              e.printStackTrace();
          }
          return routes;
      }

      // Executes in UI thread, after the parsing process
      @Override
      protected void onPostExecute(List<List<HashMap<String, String>>> result)
      {
          ArrayList<LatLng> points = null;
          PolylineOptions lineOptions = null;
          MarkerOptions markerOptions = new MarkerOptions();
          String distance = "";
          String duration = "";

          if (result.size() < 1)
          {
              Toast.makeText(MapActivity.this.getBaseContext(), "No Points", Toast.LENGTH_SHORT).show();
              return;
          }

          // Traversing through all the routes
          for (int i = 0; i < result.size(); i++)
          {
              points = new ArrayList<LatLng>();
              lineOptions = new PolylineOptions();

              // Fetching i-th route
              List<HashMap<String, String>> path = result.get(i);

              // Fetching all the points in i-th route
              for (int j = 0; j < path.size(); j++)
              {
                  HashMap<String, String> point = path.get(j);

                  if (j == 0)
                  { // Get distance from the list
                      distance = point.get("distance");
                      continue;
                  } else if (j == 1)
                  { // Get duration from the list
                      duration = point.get("duration");
                      continue;
                  }
                  double lat = Double.parseDouble(point.get("lat"));
                  double lng = Double.parseDouble(point.get("lng"));
                  LatLng position = new LatLng(lat, lng);
                  points.add(position);
              }

              // Adding all the points in the route to LineOptions
              lineOptions.addAll(points);
              lineOptions.width(10);
              lineOptions.color(Color.RED);
          }

          // Drawing polyline in the Google Map for the i-th route
          MapActivity.this.map.addPolyline(lineOptions);
      }
  }
} 
