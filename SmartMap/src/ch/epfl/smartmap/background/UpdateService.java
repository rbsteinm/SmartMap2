package ch.epfl.smartmap.background;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import ch.epfl.smartmap.cache.FriendInvitation;
import ch.epfl.smartmap.cache.ImmutableUser;
import ch.epfl.smartmap.cache.Invitation;
import ch.epfl.smartmap.cache.InvitationManager;
import ch.epfl.smartmap.database.DatabaseHelper;
import ch.epfl.smartmap.gui.Utils;
import ch.epfl.smartmap.listeners.OnInvitationListUpdateListener;
import ch.epfl.smartmap.listeners.OnInvitationStatusUpdateListener;
import ch.epfl.smartmap.search.CachedSearchEngine;
import ch.epfl.smartmap.servercom.SmartMapClientException;

/**
 * A background service that updates friends' position periodically
 * 
 * @author ritterni
 */

public class UpdateService extends Service implements OnInvitationListUpdateListener,
    OnInvitationStatusUpdateListener {
    private static final int HANDLER_DELAY = 1000;

    private static final int POS_UPDATE_DELAY = 10000;

    private static final int INVITE_UPDATE_DELAY = 30000;

    private static final float MIN_DISTANCE = 0; // minimum distance to update
                                                 // position

    private static final int RESTART_DELAY = 2000;
    public static final int IMAGE_QUALITY = 100;
    private final Handler mHandler = new Handler();

    private LocationManager mLocManager;

    private boolean mFriendsPosEnabled = true;
    private boolean mOwnPosEnabled = true;

    private final boolean isFriendIDListRetrieved = true;
    private final boolean isDatabaseInitialized = false;
    private DatabaseHelper mHelper;
    private SettingsManager mManager;
    private Geocoder mGeocoder;
    private Set<Long> mInviterIds = new HashSet<Long>();
    // Settings
    private int mPosUpdateDelay;
    private boolean mNotificationsEnabled;
    private boolean mNotificationsForEventInvitations;
    private boolean mNotificationsForFriendRequests;

    private boolean mNotificationsForFriendshipConfirmations;
    private final Runnable friendsPosUpdate = new Runnable() {
        @Override
        public void run() {
            Log.d("SERVICE", "friendposupdate");
            new AsyncFriendsPos().execute();
            mHandler.postDelayed(this, 10000);
        }
    };

    private final Runnable nearEventsUpdate = new Runnable() {
        @Override
        public void run() {
            Log.d("SERVICE", "neareventsupdate");
            new AsyncTask<Void, Void, Void>() {
                @Override
                public Void doInBackground(Void... params) {

                    CachedSearchEngine.getInstance().getAllNearEvents(
                        SettingsManager.getInstance().getLocation(), 100000, null);

                    return null;
                }
            }.execute();
            mHandler.postDelayed(this, 20000);
        }
    };

    private final Runnable updateDatabase = new Runnable() {
        @Override
        public void run() {
            Log.d("SERVICE", "Update Database");
            new AsyncTask<Void, Void, Void>() {
                @Override
                public Void doInBackground(Void... params) {
                    DatabaseHelper.getInstance().updateFromCache();
                    return null;
                }
            }.execute();
            mHandler.postDelayed(this, 60000);
        }
    };

    private final Runnable getInvitations = new Runnable() {
        @Override
        public void run() {
            UpdateService.this.loadSettings();
            new AsyncGetInvitations().execute();
            mHandler.postDelayed(this, INVITE_UPDATE_DELAY);
        }
    };

    public void downloadUserPicture(long id) {
        try {
            mHelper.setUserPicture(ServiceContainer.getNetworkClient().getProfilePicture(id), id);
        } catch (SmartMapClientException e) {
            Log.e("UpdateService", "Couldn't download picture #" + id + "!");
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Utils.sContext == null) {
            Utils.sContext = this;
        }
        mManager = SettingsManager.initialize(this.getApplicationContext());
        mHelper = DatabaseHelper.initialize(this.getApplicationContext());
        mHelper.addOnInvitationListUpdateListener(this);
        mHelper.addOnInvitationStatusUpdateListener(this);
        mGeocoder = new Geocoder(this.getBaseContext(), Locale.US);
        mLocManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        this.updateInvitationSet();
    }

    @Override
    public void onInvitationListUpdate() {

    }

    @Override
    public void onInvitationStatusUpdate(long userID, int newStatus) {
        // if the updated notification is among the pending ones
        // if (mInviterIds.contains(userID)) {
        if (newStatus == Invitation.ACCEPTED) {
            new AsyncAcceptFriends().execute(userID);
        } else if (newStatus == Invitation.REFUSED) {
            new AsyncDeclineFriends().execute(userID);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.loadSettings();
        mHandler.removeCallbacks(friendsPosUpdate);
        mHandler.postDelayed(friendsPosUpdate, HANDLER_DELAY);
        mHandler.removeCallbacks(getInvitations);
        mHandler.postDelayed(getInvitations, HANDLER_DELAY);
        mHandler.removeCallbacks(nearEventsUpdate);
        mHandler.postDelayed(nearEventsUpdate, HANDLER_DELAY);
        mHandler.removeCallbacks(updateDatabase);
        mHandler.postDelayed(updateDatabase, HANDLER_DELAY);
        new AsyncLogin().execute();

        Criteria criteria = new Criteria();
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        mLocManager.requestLocationUpdates(mLocManager.getBestProvider(criteria, true), mPosUpdateDelay,
            MIN_DISTANCE, new MyLocationListener());

        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartService = new Intent(this.getApplicationContext(), this.getClass());
        restartService.setPackage(this.getPackageName());
        PendingIntent restartServicePending =
            PendingIntent.getService(this.getApplicationContext(), 1, restartService,
                PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService =
            (AlarmManager) this.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + RESTART_DELAY,
            restartServicePending);
    }

    // Ugly workaround because of KitKat stopping services when app gets closed
    // (Android issue #63618)
    /**
     * Enables/disables friends position updates
     * 
     * @param isEnabled
     *            True if updates should be enabled
     */
    public void setFriendsPosUpdateEnabled(boolean isEnabled) {
        mFriendsPosEnabled = isEnabled;
        if (isEnabled) {
            mHandler.postDelayed(friendsPosUpdate, HANDLER_DELAY);
        }
    }

    public void updateInvitationSet() {
        mInviterIds = new HashSet<Long>();
        for (FriendInvitation invitation : mHelper.getUnansweredFriendInvitations()) {
            mInviterIds.add(invitation.getUserId());
        }
    }

    private void loadSettings() {
        mPosUpdateDelay = mManager.getRefreshFrequency() * 1000;
        mNotificationsEnabled = mManager.notificationsEnabled();
        mNotificationsForEventInvitations = mManager.notificationsForEventInvitations();
        mNotificationsForFriendRequests = mManager.notificationsForFriendRequests();
        mNotificationsForFriendshipConfirmations = mManager.notificationsForFriendshipConfirmations();
    }

    /**
     * AsyncTask to accept friend requests
     * 
     * @author ritterni
     */
    private class AsyncAcceptFriends extends AsyncTask<Long, Void, Void> {
        @Override
        protected Void doInBackground(Long... ids) {
            try {
                for (long id : ids) {
                    ServiceContainer.getNetworkClient().acceptInvitation(id);
                }
            } catch (SmartMapClientException e) {
                Log.e("UpdateService", "Couldn't accept request!");
            }
            return null;
        }
    }

    /**
     * AsyncTask to decline friend requests
     * 
     * @author ritterni
     */
    private class AsyncDeclineFriends extends AsyncTask<Long, Void, Void> {
        @Override
        protected Void doInBackground(Long... ids) {
            try {
                for (long id : ids) {
                    ServiceContainer.getNetworkClient().declineInvitation(id);
                }
            } catch (SmartMapClientException e) {
                Log.e("UpdateService", "Couldn't decline request!");
            }
            return null;
        }
    }

    /**
     * AsyncTask to send the user's own position to the server
     * 
     * @author ritterni
     */
    private class AsyncFriendsPos extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... args0) {
            try {
                List<ImmutableUser> friendsWithNewLocations =
                    ServiceContainer.getNetworkClient().listFriendsPos();
                ServiceContainer.getCache().updateFriendList(friendsWithNewLocations);
            } catch (SmartMapClientException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * AsyncTask to get invitations.
     * 
     * @author ritterni
     */
    private class AsyncGetInvitations extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... arg0) {

            try {
                InvitationManager.getInstance().update(ServiceContainer.getNetworkClient().getInvitations());
            } catch (SmartMapClientException e) {
                Log.e("UpdateService",
                    "Couldn't retrieve invitations due to a server error: " + e.getMessage());
                // try to re-log
                new AsyncLogin().execute();
            }
            return null;
        }
    }

    /**
     * AsyncTask to log in
     * 
     * @author ritterni
     */
    private class AsyncLogin extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                ServiceContainer.getNetworkClient().authServer(mManager.getUserName(),
                    mManager.getFacebookID(), mManager.getToken());
            } catch (SmartMapClientException e) {
                Log.e("UpdateService", "Couldn't log in!");
            }
            return null;
        }
    }

    /**
     * AsyncTask to send the user's own position to the server
     * 
     * @author ritterni
     */
    private class AsyncOwnPos extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                ServiceContainer.getNetworkClient().updatePos(mManager.getLocation());
            } catch (SmartMapClientException e) {
                Log.e("UpdateService", "Position update failed!");
            }
            return null;
        }
    }

    /**
     * A location listener
     * 
     * @author ritterni
     */
    private final class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location locFromGps) {
            mManager.setLocation(locFromGps);
            if (mOwnPosEnabled) {
                new AsyncOwnPos().execute();
            }
            // Sets the location name
            try {
                List<Address> addresses =
                    mGeocoder.getFromLocation(locFromGps.getLatitude(), locFromGps.getLongitude(), 1);

                String locName = SettingsManager.DEFAULT_LOC_NAME;
                if (!addresses.isEmpty()) {
                    locName = addresses.get(0).getLocality();
                }
                mManager.setLocationName(locName);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            mOwnPosEnabled = false;
        }

        @Override
        public void onProviderEnabled(String provider) {
            mOwnPosEnabled = true;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // stop sending position if provider isn't available
            if ((status == LocationProvider.OUT_OF_SERVICE)
                || (status == LocationProvider.TEMPORARILY_UNAVAILABLE)) {
                mOwnPosEnabled = false;
            } else if (status == LocationProvider.AVAILABLE) {
                mOwnPosEnabled = true;
            }
        }
    }
}