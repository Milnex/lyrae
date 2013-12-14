package in.myng.lyrae;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.facebook.*;
import com.facebook.model.*;

import android.support.v4.app.*;

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
	private String message = "null";
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
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
	    		message = adapterView.getSelectedItem().toString();
	    	}
	    	public void onNothingSelected(AdapterView arg0) 
	    	{
	    		message = arg0.getSelectedItem().toString();
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
	
	/** Called when the user clicks the Send button */
	public void sendMessage(View view) 
	{
		final Intent intent = new Intent(this, DisplayFragment.class);
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
		                	message = message+" "+user.getId()+" "+ user.getName();
		                	intent.putExtra(EXTRA_MESSAGE, message);
		            	    startActivity(intent);
		                }
		            }
		            if (response.getError() != null) {
		                // Handle errors, will do so later.
		            }
		        }
		    });
		    request.executeAsync();
	    }
	}
}