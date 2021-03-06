package edu.uw.chather;
/**
 * Main activity that runs in the background of the app. Sets up our bottom navigation.
 */
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;


import com.auth0.android.jwt.JWT;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import edu.uw.chather.databinding.ActivityMainBinding;
import edu.uw.chather.services.PushReceiver;
import edu.uw.chather.ui.chat.ChatMessage;
import edu.uw.chather.ui.chat.ChatViewModel;
import edu.uw.chather.ui.contact.Contact;
import edu.uw.chather.ui.contact.ContactListViewModel;
import edu.uw.chather.ui.contact.ContactRequestViewModel;
import edu.uw.chather.ui.location.LocationViewModel;
import edu.uw.chather.ui.chat.Chatroom;
import edu.uw.chather.ui.model.NewContactCountViewModel;
import edu.uw.chather.ui.model.NewMessageCountViewModel;
import edu.uw.chather.ui.model.PushyTokenViewModel;
import edu.uw.chather.ui.model.UserInfoViewModel;
import edu.uw.chather.ui.weather.WeatherFragment;
import edu.uw.chather.ui.passwordreset.PasswordResetFragment;
import edu.uw.chather.utils.Utils;


public class MainActivity extends AppCompatActivity {
    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // A constant int for the permissions request code. Must be a 16 bit number
    private static final int MY_PERMISSIONS_LOCATIONS = 8414;

    private LocationRequest mLocationRequest;

    //Use a FusedLocationProviderClient to request the location
    private FusedLocationProviderClient mFusedLocationClient;

    // Will use this call back to decide what to do when a location change is detected
    private LocationCallback mLocationCallback;

    //The ViewModel that will store the current location
    private LocationViewModel mLocationModel;

    private AppBarConfiguration mAppBarConfiguration;

    private NavController navController;
    /**
     * Creates our bottom navigation menu from the menu elements we've given it before.
     */
    private MainPushMessageReceiver mPushMessageReceiver;
    private NewMessageCountViewModel mNewMessageModel;
    private NewContactCountViewModel mNewContactModel;
    private ActivityMainBinding binding;


    /**
     * Creates our bottom navigation menu from the menu elements we've given it before.
     * See onCreate parent method for more implementation details.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.onActivityCreateSetTheme(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MainActivityArgs args = MainActivityArgs.fromBundle(getIntent().getExtras());


        //  AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
//        String email = args.getJwt();
//        //Import com.auth0.android.jwt.JWT
//        JWT jwt = new JWT(args.getJwt());

        // Check to see if the web token is still valid or not. To make a JWT expire after a
        // longer or shorter time period, change the expiration time when the JWT is
        // created on the web service.
        // if(!jwt.isExpired(0)) {
        //    new ViewModelProvider(this, new UserInfoViewModel.UserInfoViewModelFactory(jwt)).get(UserInfoViewModel.class);
        //} else {
        //In production code, add in your own error handling/flow for when the JWT is expired
        //    throw new IllegalStateException("JWT is expired!");
        //}

        new ViewModelProvider(this,
                new UserInfoViewModel.UserInfoViewModelFactory(args.getEmail(), args.getJwt(), args.getUsername())
        ).get(UserInfoViewModel.class);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.success, R.id.navigation_contact, R.id.chatListFragment, R.id.weatherFragment)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        mNewMessageModel = new ViewModelProvider(this).get(NewMessageCountViewModel.class);
        mNewContactModel = new ViewModelProvider(this).get(NewContactCountViewModel.class);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.chatListFragment) {
                // When the user navigates to the chats page, reset the new message count.
                // This will need some extra logic for your project as it should have
                // multiple chatrooms.
                mNewMessageModel.reset();
            }
            else if(destination.getId() == R.id.navigate_contact_request){
                mNewContactModel.reset();
            }
        });

        mNewMessageModel.addMessageCountObserver(this, count -> {
            BadgeDrawable badge = binding.navView.getOrCreateBadge(R.id.chatListFragment);
            badge.setMaxCharacterCount(2);
            if (count > 0) {
                // new messages! update and show the notification badge.
                badge.setNumber(count);
                badge.setVisible(true);
            } else {
                // user did some action to clear the new messages, remove the badge
                badge.clearNumber();
                badge.setVisible(false);
            }
        });

        mNewContactModel.addContactCountObserver(this, count -> {
            BadgeDrawable badge = binding.navView.getOrCreateBadge(R.id.navigation_contact);
            BadgeDrawable bdge = binding.navView.getOrCreateBadge(R.id.navigate_contact_request);
            badge.setMaxCharacterCount(2);
            bdge.setMaxCharacterCount(2);
            if (count > 0) {
                // new messages! update and show the notification badge.
                bdge.setNumber(count);
                bdge.setVisible(true);
                badge.setNumber(count);
                badge.setVisible(true);
            } else {
                // user did some action to clear the new messages, remove the badge
                bdge.clearNumber();
                bdge.setVisible(false);
                badge.clearNumber();
                badge.setVisible(false);
            }
        });

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION
                            , Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_LOCATIONS);
        } else {
            //The user has already allowed the use of Locations. Get the current location.
            requestLocation();
        }

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...
                    Log.d("LOCATION UPDATE!", location.toString());
                    if (mLocationModel == null) {
                        mLocationModel = new ViewModelProvider(MainActivity.this)
                                .get(LocationViewModel.class);
                    }
                    mLocationModel.setLocation(location);
                }
            };
        };
        createLocationRequest();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_LOCATIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // locations-related task you need to do.
                    requestLocation();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("PERMISSION DENIED", "Nothing to see or do here.");

                    //Shut down the app. In production release, you would let the user
                    //know why the app is shutting down...maybe ask for permission again?
                    finishAndRemoveTask();
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("REQUEST LOCATION", "User did NOT allow permission to request location!");
        } else {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                Log.d("LOCATION", location.toString());
                                if (mLocationModel == null) {
                                    mLocationModel = new ViewModelProvider(MainActivity.this)
                                            .get(LocationViewModel.class);
                                }
                                mLocationModel.setLocation(location);
                            }
                        }
                    });
        }
    }

    /**
     * Create and configure a Location Request used when retrieving location updates
     */
    private void createLocationRequest() {
        mLocationRequest = LocationRequest.create();
        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback,
                    null /* Looper */);
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }


    @Override
    public boolean onSupportNavigateUp() {
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.drop_down, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.navigate_change_password) {
            navController.navigate(R.id.changePasswordFragment);
        }

        if (id == R.id.navigation_add) {
            navController.navigate(R.id.navigation_add);
        }

        if (id == R.id.navigate_change_theme) {
            navController.navigate(R.id.changeThemeFragment);
        }

        if (id == R.id.navigate_contact_request) {
            navController.navigate(R.id.navigate_contact_request);
        }
        if (id == R.id.navigate_sign_out) {
            signOut();
        }
        if (id == R.id.navigate_button_new_chat) {
            navController.navigate(R.id.chatNewRoomFragment);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPushMessageReceiver == null) {
            mPushMessageReceiver = new MainPushMessageReceiver();
        }
        IntentFilter iFilter = new IntentFilter(PushReceiver.RECEIVED_NEW_MESSAGE);
        registerReceiver(mPushMessageReceiver, iFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPushMessageReceiver != null) {
            unregisterReceiver(mPushMessageReceiver);
        }
    }

    private void signOut() {
        SharedPreferences prefs =
                getSharedPreferences(
                        getString(R.string.keys_shared_prefs),
                        Context.MODE_PRIVATE);
        prefs.edit().remove(getString(R.string.keys_prefs_jwt)).apply();
        //End the app completely
//        finishAndRemoveTask();
        PushyTokenViewModel model = new ViewModelProvider(this)
                .get(PushyTokenViewModel.class);
        //when we hear back from the web service quit
        model.addResponseObserver(this, result -> finishAndRemoveTask());
        model.deleteTokenFromWebservice(
                new ViewModelProvider(this)
                        .get(UserInfoViewModel.class)
                        .getmJwt()
        );
    }

    /**
     * A BroadcastReceiver that listens fro messages sent from PushReceiver
     */
    private class MainPushMessageReceiver extends BroadcastReceiver {

        private final ChatViewModel mModel =
                new ViewModelProvider(MainActivity.this).get(ChatViewModel.class);
        private final ContactRequestViewModel mRequestModel =
                new ViewModelProvider(MainActivity.this).get(ContactRequestViewModel.class);
        private final ContactListViewModel mContact =
                new ViewModelProvider(MainActivity.this).get(ContactListViewModel.class);

        @Override
        public void onReceive(Context context, Intent intent) {
            NavController nc = Navigation.findNavController(MainActivity.this, R.id.nav_host_fragment);
            NavDestination nd = nc.getCurrentDestination();
            if (intent.hasExtra("chatMessage")) {
                ChatMessage cm = (ChatMessage) intent.getSerializableExtra("chatMessage");

                // If the user is not on the chat screen, update the
                // NewMessageCountView Model
                if (!cm.getSender().equals(MainActivityArgs.fromBundle(getIntent().getExtras()).getEmail())) {
                    if (nd.getId() != R.id.chatListFragment) {
                        Log.d("PUSH RECEIVE", cm.getSender() + " : " + MainActivityArgs.fromBundle(getIntent().getExtras()).getEmail());
                        mNewMessageModel.increment();
                    }
                }

                // Inform the view model holding chatroom messages of the new message.
                mModel.addMessage(intent.getIntExtra("chatid", -1), cm);
            } else if (intent.hasExtra("chatroom")) {
                Chatroom cr = (Chatroom) intent.getSerializableExtra("chatroom");
                // if the user is not on the chat screen, update the New chatroom view model
                // TODO TODO
            }
            else if (intent.hasExtra("contactString")){

                if (nd.getId() != R.id.navigate_contact_request) {
                    mNewContactModel.increment();
                }

                // Inform the view model holding chatroom messages of the new message.
                mRequestModel.connectGet();
            }
            else{
                mContact.connectGet();
            }
        }
    }
}