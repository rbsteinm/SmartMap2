package ch.epfl.smartmap.background;

import java.util.GregorianCalendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import ch.epfl.smartmap.R;
import ch.epfl.smartmap.util.Utils;

/**
 * Used to get and set settings and local info using SharedPreferences
 * 
 * @author ritterni
 * @author SpicyCH
 */

public class SettingsManager {
    public static final String PREFS_NAME = "settings";
    public static final String USER_ID = "UserID";
    public static final String USER_NAME = "UserName";
    public static final String PHONE_NUMBER = "PhoneNumber";
    public static final String EMAIL = "Email";
    public static final String SUBTITLE = "Subtitle";
    public static final String LASTSEEN = "LastSeen";
    public static final String TOKEN = "Token";
    public static final String FB_ID = "FacebookID";
    public static final String COOKIE = "Cookie";
    public static final String LONGITUDE = "Longitude";
    public static final String LATITUDE = "Latitude";
    public static final String LOCATION_NAME = "LocName";
    public static final String IMAGE = "Image";

    private static final long DEFAULT_ID = -1;
    private final String defaultName;
    private final String defaultNumber;
    private final String defaultMail;
    private final String defaultSubtitle;
    private static final long DEFAULT_LAST_SEEN = 0;
    private final String defaultToken;
    private static final long DEFAULT_FB_ID = -1;
    private final String defaultCookie;
    private final String defaultLocName;

    private final Context mContext;
    private final SharedPreferences mSharedPref;
    private final SharedPreferences.Editor mEditor;

    /**
     * SettingsManager constructor
     * 
     * @param context
     *            The app's context, needed to access the shared preferences
     */
    public SettingsManager(Context context) {
        mContext = context;
        mSharedPref = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mEditor = mSharedPref.edit();

        defaultName = mContext.getString(R.string.settings_manager_default_name);
        defaultNumber = mContext.getString(R.string.settings_manager_default_number);
        defaultMail = mContext.getString(R.string.settings_manager_default_email);
        defaultSubtitle = mContext.getString(R.string.settings_manager_default_subtitle);
        defaultToken = mContext.getString(R.string.settings_manager_default_token);
        defaultCookie = mContext.getString(R.string.settings_manager_default_cookie);
        defaultLocName = mContext.getString(R.string.settings_manager_default_locname);
    }

    /**
     * Clears the settings.
     * 
     * @return <code>true</code> if the settings were cleared successfully, <code>false</code> otherwise.
     */
    public boolean clearAll() {
        mEditor.clear();
        return mEditor.commit();
    }

    public Context getContext() {
        return mContext;
    }

    /**
     * @return The session cookie if it is found, DEFAULT_COOKIE value otherwise.
     */
    public String getCookie() {
        return mSharedPref.getString(COOKIE, defaultCookie);
    }

    /**
     * @return The local user's email if it is found, DEFAULT_EMAIL value otherwise.
     */
    public String getEmail() {
        return mSharedPref.getString(EMAIL, defaultMail);
    }

    /**
     * @return The local user's Facebook ID if it is found, DEFAULT_FB_ID value otherwise
     * @author SpicyCH
     */
    public long getFacebookID() {
        return mSharedPref.getLong(FB_ID, DEFAULT_FB_ID);
    }

    /**
     * @return the date (in milliseconds) when we were last seen updating our position
     */
    public long getLastSeen() {
        return mSharedPref.getLong(LASTSEEN, DEFAULT_LAST_SEEN);
    }

    /**
     * @return The local user's current position as a Location object
     */
    public Location getLocation() {
        Location loc = new Location("");
        // Shared prefs can't store doubles
        loc.setLongitude(Double.longBitsToDouble(mSharedPref.getLong(LONGITUDE, 0)));
        loc.setLatitude(Double.longBitsToDouble(mSharedPref.getLong(LATITUDE, 0)));
        return loc;
    }

    public String getLocationName() {
        return mSharedPref.getString(LOCATION_NAME, defaultLocName);
    }

    /**
     * @return the maximum distance from the user, in meters, for which we can fetch events.<br />
     *         Special cases: 0 if the user selected 'None' and 41'000'000 if the user selected 'All'.
     * @author SpicyCH
     */
    public int getNearEventsMaxDistance() {
        String defaultValue = mContext.getString(R.string.pref_events_max_distance_default_value);
        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mContext).getString(
            mContext.getString(R.string.settings_key_max_distance_fetch_events), defaultValue));
    }

    /**
     * @return the frequence in milliseconds at which we fetch and upload the datas. Used by the service.
     * @author SpicyCH
     */
    public int getRefreshFrequency() {
        String defaultValue = mContext.getString(R.string.pref_sync_frequency_default_value);
        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mContext).getString(
            mContext.getString(R.string.settings_key_refresh_frequency), defaultValue));
    }

    /**
     * @return the subtitle, or <code>DEFAULT_LAST_SEEN</code> if we were never seen on SmartMap.
     */
    public String getSubtitle() {
        if (this.getLastSeen() == DEFAULT_LAST_SEEN) {
            return defaultSubtitle;
        }

        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(this.getLastSeen());

        return Utils.getLastSeenStringFromCalendar(calendar) + " "
            + this.getContext().getString(R.string.settings_manager_near) + " " + this.getLocationName();
    }

    /**
     * @return the time to wait in milliseconds before hiding inactive friends from the map. Or the int value
     *         -1 if the
     *         user never wants to hide inactive friends.
     * @author SpicyCH
     */
    public int getTimeToWaitBeforeHidingFriends() {
        String defaultValue = mContext.getString(R.string.pref_last_seen_max_default_value);
        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mContext).getString(
            mContext.getString(R.string.settings_key_last_seen_max), defaultValue));
    }

    /**
     * @return The local user's Facebook token if it is found, DEFAULT_TOKEN value otherwise
     */
    public String getToken() {
        return mSharedPref.getString(TOKEN, defaultToken);
    }

    /**
     * @return The local user's phone number if it is found, DEFAULT_NUMBER value otherwise
     */
    public String getUPhoneNumber() {
        return mSharedPref.getString(PHONE_NUMBER, defaultNumber);
    }

    /**
     * @return The local user's ID if it is found, DEFAULT_ID value otherwise
     */
    public long getUserId() {
        return mSharedPref.getLong(USER_ID, DEFAULT_ID);
    }

    /**
     * @return The local user's name if it is found, DEFAULT_NAME value otherwise
     */
    public String getUserName() {
        return mSharedPref.getString(USER_NAME, defaultName);
    }

    /**
     * @return <code>true</code> if the user is offline, <code>false</code> otherwise (if he's online).
     * @author SpicyCH
     */
    public boolean isOffline() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(
            mContext.getString(R.string.settings_key_general_offline), false);
    }

    /**
     * @return <code>true</code> if the user enabled the notifications, <code>false</code> otherwise.
     * @author SpicyCH
     */
    public boolean notificationsEnabled() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(
            mContext.getString(R.string.settings_key_notifications_enabled), true);
    }

    /**
     * @return <code>true</code> if the user enabled the notifications for event invitations and the user
     *         activated the
     *         notifications in general, <code>false</code> otherwise.
     * @author SpicyCH
     */
    public boolean notificationsForEventInvitations() {
        return this.notificationsEnabled() ? PreferenceManager.getDefaultSharedPreferences(mContext)
            .getBoolean(mContext.getString(R.string.settings_key_notifications_event_invitations), true)
            : false;
    }

    /**
     * @return <code>true</code> if the user enabled the notifications for event proximity and the user
     *         activated the
     *         notifications in general, <code>false</code> otherwise.
     * @author SpicyCH
     */
    public boolean notificationsForEventProximity() {
        return this.notificationsEnabled() ? PreferenceManager.getDefaultSharedPreferences(mContext)
            .getBoolean(mContext.getString(R.string.settings_key_notifications_event_proximity), true)
            : false;
    }

    /**
     * @return <code>true</code> if the user enabled the notifications for friend requests and the user
     *         activated the
     *         notifications in general, <code>false</code> otherwise.
     * @author SpicyCH
     */
    public boolean notificationsForFriendRequests() {
        return this.notificationsEnabled() ? PreferenceManager.getDefaultSharedPreferences(mContext)
            .getBoolean(mContext.getString(R.string.settings_key_notifications_friend_requests), true)
            : false;
    }

    /**
     * A friendship confirmation happens when another user accepts your friend request.
     * 
     * @return <code>true</code> if the user enabled the notifications for friendship confirmations and the
     *         user
     * @author SpicyCH
     */
    public boolean notificationsForFriendshipConfirmations() {
        return this.notificationsEnabled() ? PreferenceManager.getDefaultSharedPreferences(mContext)
            .getBoolean(mContext.getString(R.string.settings_key_notifications_friendship_confirmations),
                true) : false;
    }

    /**
     * @return <code>true</code> if the user enabled the notifications vibrations and the user activated the
     *         notifications in general, <code>false</code> otherwise.
     * @author SpicyCH
     */
    public boolean notificationsVibrate() {
        return this.notificationsEnabled() ? PreferenceManager.getDefaultSharedPreferences(mContext)
            .getBoolean(mContext.getString(R.string.settings_key_notifications_vibrate), true) : false;
    }

    /**
     * Stores the session cookie.
     * 
     * @param newCookie
     *            The new cookie
     * @return True if the new value was successfully saved
     */
    public boolean setCookie(String newCookie) {
        mEditor.putString(COOKIE, newCookie);
        return mEditor.commit();
    }

    /**
     * Stores the user's email.
     * 
     * @param newEmail
     *            The new email address
     * @return True if the new value was successfully saved
     */
    public boolean setEmail(String newEmail) {
        mEditor.putString(EMAIL, newEmail);
        return mEditor.commit();
    }

    /**
     * Stores the user's Facebook ID.
     * 
     * @param newID
     *            The user's new Facebook ID
     * @return True if the new value was successfully saved
     */
    public boolean setFacebookID(long newID) {
        mEditor.putLong(FB_ID, newID);
        return mEditor.commit();
    }

    /**
     * @param newLastSeen
     *            the time in milliseconds when we were last seen
     * @return <code>true</code> if we could successfully modify this value
     */
    public boolean setLastSeen(long newLastSeen) {
        mEditor.putLong(LASTSEEN, newLastSeen);
        return mEditor.commit();
    }

    /**
     * Stores the local user's location.
     * 
     * @param loc
     *            The location to be stored
     * @return True if the location was stored successfully
     */
    public boolean setLocation(Location loc) {
        mEditor.putLong(LONGITUDE, Double.doubleToRawLongBits(loc.getLongitude()));
        mEditor.putLong(LATITUDE, Double.doubleToRawLongBits(loc.getLatitude()));
        return mEditor.commit();
    }

    /**
     * Change the location name of the local user.
     * 
     * @param locName
     *            The new location name (e.g. city)
     * @return If the name was stored successfully
     */
    public boolean setLocationName(String locName) {
        mEditor.putString(LOCATION_NAME, locName);
        return mEditor.commit();
    }

    /**
     * @param newSubtitle
     *            the new subtitle to set
     * @return <code>true</code> if we could modify the value, <code>false</code> otherwise.
     */
    public boolean setSubtitle(String newSubtitle) {
        mEditor.putString(SUBTITLE, newSubtitle);
        return mEditor.commit();
    }

    /**
     * Stores the Facebook token
     * 
     * @param newToken
     *            The new token
     * @return True if the new value was successfully saved
     */
    public boolean setToken(String newToken) {
        mEditor.putString(TOKEN, newToken);
        return mEditor.commit();
    }

    /**
     * Stores the user's phone number
     * 
     * @param newNumber
     *            The user's new number
     * @return True if the new value was successfully saved
     */
    public boolean setUPhoneNumber(String newNumber) {
        mEditor.putString(PHONE_NUMBER, newNumber);
        return mEditor.commit();
    }

    /**
     * Stores the user's ID
     * 
     * @param newID
     *            The user's new ID
     * @return True if the new value was successfully saved
     */
    public boolean setUserID(long newID) {
        mEditor.putLong(USER_ID, newID);
        return mEditor.commit();
    }

    /**
     * Stores the local user's name
     * 
     * @param newName
     *            The user's new name
     * @return True if the new value was successfully saved
     */
    public boolean setUserName(String newName) {
        mEditor.putString(USER_NAME, newName);
        return mEditor.commit();
    }

    /**
     * @return <code>true</code> if the user wants to see his private events on his map, <code>false</code>
     *         otherwise.
     * @author SpicyCH
     */
    public boolean showPrivateEvents() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(
            mContext.getString(R.string.settings_key_events_show_private), true);
    }

    /**
     * @return <code>true</code> if the user wants to see public events on his map, <code>false</code>
     *         otherwise.
     * @author SpicyCH
     */
    public boolean showPublicEvents() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(
            mContext.getString(R.string.settings_key_events_show_public), true);
    }
}