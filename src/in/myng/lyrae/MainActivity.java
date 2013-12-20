package in.myng.lyrae;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.HttpResponse;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.facebook.*;
import com.facebook.model.*;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import data.JsonUser;

import android.support.v4.app.*;

@SuppressLint("NewApi")
public class MainActivity extends FragmentActivity
{

	private static final int SPLASH = 0;
	private static final int SELECTION = 1;
	private static final int SETTINGS = 2;
	private static final int FRAGMENT_COUNT = SETTINGS +1;
	
	private MenuItem settings;

	private Fragment[] fragments = new Fragment[FRAGMENT_COUNT];
	private boolean isResumed = false;
	
	public final static String EXTRA_MESSAGE = "in.myng.lyrae.MESSAGE";
	public final static String GROUP_MESSAGE = "in.myng.lyrae.GROUPMESSAGE";
	public final static String UID_MESSAGE = "in.myng.lyrae.UIDMESSAGE";
	public final static String NAME_MESSAGE = "in.myng.lyrae.NAMEMESSAGE";
	static Intent intent;
	static LocationManager locationManager = null;
	static LocationListener locationListener = null;
	static LocationData locationData = LocationData.getInstance();
	static JsonUser curUser = new JsonUser();
	private static final int HF_MINUTES = 1000 * 30;
	String url = "http://ec2-54-204-122-234.compute-1.amazonaws.com:3000";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    intent = new Intent(this, MapActivity.class);
	    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
	    StrictMode.setThreadPolicy(policy);
	    
	    uiHelper = new UiLifecycleHelper(this, callback);
	    uiHelper.onCreate(savedInstanceState);

	    setContentView(R.layout.main);

	    ///////////////////////////////////////////////////////////////
	    Spinner spinner = (Spinner) findViewById(R.id.spinnner);
	    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,new String[]{"Eat","Tea Time","Chat","Gym","Museum","Trip","Anything"});
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(adapter);
	    spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener()
	    {
	    	public void onItemSelected(AdapterView adapterView, View view, int position, long id)
	    	{
	    		curUser.activity = adapterView.getSelectedItem().toString();
	    	}
	    	public void onNothingSelected(AdapterView arg0) 
	    	{
	    		curUser.activity = arg0.getSelectedItem().toString();
	    	}
	    });
	    ///////////////////////////////////////////////////////////////
	    
	    FragmentManager fm = getSupportFragmentManager();
	    fragments[SPLASH] = fm.findFragmentById(R.id.splashFragment);
	    fragments[SELECTION] = fm.findFragmentById(R.id.selectionFragment);
	    fragments[SETTINGS] = fm.findFragmentById(R.id.userSettingsFragment);

	    FragmentTransaction transaction = fm.beginTransaction();
	    for(int i = 0; i < fragments.length; i++)
	    {
	        transaction.hide(fragments[i]);
	    }
	    transaction.commit();
	    startLocationListener();
	}
	
	private void showFragment(int fragmentIndex, boolean addToBackStack) 
	{
	    FragmentManager fm = getSupportFragmentManager();
	    FragmentTransaction transaction = fm.beginTransaction();
	    for (int i = 0; i < fragments.length; i++) 
	    {
	        if (i == fragmentIndex) 
	        {
	            transaction.show(fragments[i]);
	        }
	        else
	        {
	            transaction.hide(fragments[i]);
	        }
	    }
	    if (addToBackStack) 
	    {
	        transaction.addToBackStack(null);
	    }
	    transaction.commit();
	}
	
	@Override
	public void onResume()
	{
	    super.onResume();
	    uiHelper.onResume();
	    isResumed = true;
	}

	@Override
	public void onPause() 
	{
	    super.onPause();
	    uiHelper.onPause();
	    isResumed = false;
	}
	
	private void onSessionStateChange(Session session, SessionState state, Exception exception)
	{
	    // Only make changes if the activity is visible
	    if (isResumed) 
	    {
	        FragmentManager manager = getSupportFragmentManager();
	        // Get the number of entries in the back stack
	        int backStackSize = manager.getBackStackEntryCount();
	        // Clear the back stack
	        for (int i = 0; i < backStackSize; i++) 
	        {
	            manager.popBackStack();
	        }
	        if (state.isOpened()) 
	        {
	            // If the session state is open:
	            // Show the authenticated fragment
	            showFragment(SELECTION, false);
	        } 
	        else if (state.isClosed()) 
	        {
	            // If the session state is closed:
	            // Show the login fragment
	            showFragment(SPLASH, false);
	        }
	    }
	}
	
	@Override
	protected void onResumeFragments() 
	{
	    super.onResumeFragments();
	    Session session = Session.getActiveSession();

	    if (session != null && session.isOpened()) 
	    {
	        // if the session is already open,
	        // try to show the selection fragment
	        showFragment(SELECTION, false);
	    } 
	    else 
	    {
	        // otherwise present the splash screen
	        // and ask the person to login.
	        showFragment(SPLASH, false);
	    }
	}
	
	private UiLifecycleHelper uiHelper;
	private Session.StatusCallback callback = 
	    new Session.StatusCallback() 
	{
	    @Override
	    public void call(Session session, 
	            SessionState state, Exception exception) 
	    {
	        onSessionStateChange(session, state, exception);
	    }
	};
	
	@Override
	protected void onSaveInstanceState(Bundle outState) 
	{
	    super.onSaveInstanceState(outState);
	    uiHelper.onSaveInstanceState(outState);
	}

	@Override 
	public void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		super.onActivityResult(requestCode, resultCode, data);
		Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
		uiHelper.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	public void onDestroy() 
	{
	    super.onDestroy();
	    uiHelper.onDestroy();
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
	    // only add the menu when the selection fragment is showing
	    if (fragments[SELECTION].isVisible()) {
	        if (menu.size() == 0) {
	            settings = menu.add(R.string.settings);
	        }
	        return true;
	    } else {
	        menu.clear();
	        settings = null;
	    }
	    return false;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    if (item.equals(settings)) {
	        showFragment(SETTINGS, true);
	        return true;
	    }
	    return false;
	}
	
//	private static String convertStreamToString(InputStream is) {
//	    /*
//	     * To convert the InputStream to String we use the BufferedReader.readLine()
//	     * method. We iterate until the BufferedReader return null which means
//	     * there's no more data to read. Each line will appended to a StringBuilder
//	     * and returned as String.
//	     */
//	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//	    StringBuilder sb = new StringBuilder();
//
//	    String line = null;
//	    try {
//	        while ((line = reader.readLine()) != null) {
//	            sb.append(line + "\n");
//	        }
//	    } catch (IOException e) {
//	        e.printStackTrace();
//	    } finally {
//	        try {
//	            is.close();
//	        } catch (IOException e) {
//	            e.printStackTrace();
//	        }
//	    }
//	    return sb.toString();
//	}
	
	/** Called when the user clicks the Send button */
	public void sendMessage(View view) 
	{
		//final Intent intent = new Intent(this, DisplayFragment.class);
		final Session session = Session.getActiveSession();
		
	    if (session != null && session.isOpened()) 
	    {
	        // Get the user's data
	    	Request request = Request.newMeRequest(session, new Request.GraphUserCallback() 
		    {
		        @Override
		        public void onCompleted(GraphUser user, Response response)
		        {
		        	
		            // If the response is successful
		            if (session == Session.getActiveSession()) 
		            {
		                if (user != null) 
		                {
		                	curUser.uid=user.getId();
		                	curUser.name=user.getName();
		                	Thread createUserThread = new Thread(new CreateUserThread());
		                	createUserThread.start();
		                	//message = message+" "+user.getId()+" "+ user.getName();
		                }
		            }
		            if (response.getError() != null) {
		                // Handle errors, will do so later.
		            }
		        }
		    });
		    request.executeAsync();
	    }
    	Thread matchThread = new Thread(new UpdateUserThread());
    	matchThread.start();
    	Thread getGroupThread = new Thread(new GetGroupThread());
    	getGroupThread.start();
//    	Intent intent = new Intent(getApplicationContext(),MapActivity.class);
//	    startActivity(intent);
	}
	
	private void startLocationListener() {
	    // Get the location manager
	    locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
	    locationListener = new LocationListener() {
	        public void onLocationChanged(Location location) {
	            // Called when a new location is found by the network location provider.
	            if(locationData.getCurrLocation() != null)
	            {
	                boolean better = isBetterLocation(location, locationData.getCurrLocation());
	                if(better) {
	                	locationData.getCurrLocation().set(location);
	                }
	            }
	            else
	            {
	                locationData.setCurrLocation(location);
	            }
		      	try {
		      		HttpClient client = new DefaultHttpClient();
		      		if(curUser.uid != null)
		      		{
		      			HttpPost post = new HttpPost(url+"/user/"+curUser.uid);
//		      			Log.e("id",curUser.id);
			      		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			      		nameValuePairs.add(new BasicNameValuePair("latitude", String.valueOf(locationData.getCurrLocation().getLatitude())));
			      		nameValuePairs.add(new BasicNameValuePair("longitude", String.valueOf(locationData.getCurrLocation().getLongitude())));
			      		post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			      		
			      		HttpResponse response = client.execute(post);
			      		BufferedReader rd = new BufferedReader(new InputStreamReader(
			      				response.getEntity().getContent()));
			      		String line = "";
			      		String result = "";
			      		while ((line = rd.readLine()) != null) {
			      			result += line;
			      			System.out.println(line);
			      		}
			      		Log.e("T_LOCATION",result);
		      		}
		      	} catch (IOException e) {
	      			e.printStackTrace();
		      	}
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
  
  class CreateUserThread implements Runnable {   
      public void run() {
    	  HttpClient client = new DefaultHttpClient();
    	  if(curUser.uid==null)
    		  return;
    	  HttpPost post = new HttpPost(url+"/user/");
    	  try {
    		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("uid", String.valueOf(curUser.uid)));
			nameValuePairs.add(new BasicNameValuePair("name", String.valueOf(curUser.name)));
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			
			HttpResponse response = client.execute(post);
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));
			String line = "";
			String result = "";
			while ((line = rd.readLine()) != null) {
				result += line;
				System.out.println(line);
			}
			//Log.e("T_User",result);			

		  } catch (IOException e) {
				e.printStackTrace();
		  }
      }   
  }
  
  class UpdateUserThread implements Runnable {   
      public void run() {
    	  HttpClient client = new DefaultHttpClient();
    	  if(curUser.uid==null)
    		  return;
    	  HttpPost post = new HttpPost(url+"/user/"+curUser.uid);
    	  try {
    		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("latitude", String.valueOf(locationData.getCurrLocation().getLatitude())));
			nameValuePairs.add(new BasicNameValuePair("longitude", String.valueOf(locationData.getCurrLocation().getLongitude())));
			nameValuePairs.add(new BasicNameValuePair("matching", "true"));
			nameValuePairs.add(new BasicNameValuePair("activity", curUser.activity));
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			
			HttpResponse response = client.execute(post);
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));
			String line = "";
			String result = "";
			while ((line = rd.readLine()) != null) {
				result += line;
				System.out.println(line);
			}
			//Log.e("T_User",result);			

		  } catch (IOException e) {
				e.printStackTrace();
		  }
      }   
  }
  
  class GetGroupThread implements Runnable {
		Intent intent = new Intent(getApplicationContext(),MapActivity.class);
		
		public void run() {
			HttpClient client = new DefaultHttpClient();
			boolean flag = true;
			while(flag){
				if(curUser.uid==null){
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				HttpGet get = new HttpGet(url + "/match/" + curUser.uid);
				try {
					HttpResponse response = client.execute(get);
					BufferedReader rd = new BufferedReader(new InputStreamReader(
							response.getEntity().getContent()));
					String line = "";
					String result = "";
					while ((line = rd.readLine()) != null) {
						result += line;
						System.out.println(line);
					}
					Log.e("T_Group",result);
					Gson gson = new Gson();
					JsonUser guser = gson.fromJson(result, JsonUser.class);
						
						if (guser.gid != null && !guser.gid.equals(""))
						{
							flag=false;
							intent.putExtra(UID_MESSAGE, curUser.uid);
							intent.putExtra(GROUP_MESSAGE, guser.gid);
							intent.putExtra(NAME_MESSAGE, guser.name);
							startActivity(intent);
						}
						else
						{
							//Log.e("T_Group","Not match");
						}
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}
  }
}