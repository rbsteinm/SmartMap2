package ch.epfl.smartmap.activities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import ch.epfl.smartmap.R;
import ch.epfl.smartmap.background.ServiceContainer;
import ch.epfl.smartmap.cache.Event;
import ch.epfl.smartmap.cache.User;
import ch.epfl.smartmap.callbacks.NetworkRequestCallback;
import ch.epfl.smartmap.callbacks.SearchRequestCallback;
import ch.epfl.smartmap.gui.FriendPickerListAdapter;
import ch.epfl.smartmap.gui.FriendPickerListAdapter.ViewHolder;
import ch.epfl.smartmap.listeners.OnCacheListener;
import ch.epfl.smartmap.search.CachedSearchEngine;
import ch.epfl.smartmap.util.Utils;

/**
 * This activity shows an event in a complete screens. It display in addition
 * two buttons: one to invite friends, and
 * one to see the event on the map.
 * 
 * @author SpicyCH
 * @author agpmilli
 */
public class EventInformationActivity extends ListActivity {

    /**
     * Used to get the event id the getExtra of the starting intent, and to pass
     * the retrieved event from doInBackground
     * to onPostExecute.
     */
    public static final String EVENT_KEY = "EVENT";

    private static final String TAG = EventInformationActivity.class.getSimpleName();

    private Event mEvent;

    private TextView mEventTitle;

    private TextView mEventCreator;
    private TextView mStartDate;
    private TextView mEndDate;
    private TextView mStartHour;
    private TextView mEndHour;
    private TextView mEventDescription;
    private CheckBox mGoingCheckBox;
    private boolean mGoingChecked;
    private List<User> mParticipantsList;
    private TextView mPlaceNameAndCountry;

    /**
     * For the moment we don't offer a way to delete created events.
     */
    private static final boolean DELETE_EVENTS_ENABLE = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_show_event_information);
        this.getActionBar().setBackgroundDrawable(this.getResources().getDrawable(R.color.main_blue));

        ServiceContainer.getCache().addOnCacheListener(new CacheChangesListener());
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        FriendPickerListAdapter.ViewHolder viewHolder = (ViewHolder) v.getTag();
        if (viewHolder.getId() != ServiceContainer.getSettingsManager().getUserId()) {
            Intent userInfoIntent = new Intent(EventInformationActivity.this, UserInformationActivity.class);
            userInfoIntent.putExtra("USER", viewHolder.getId());
            EventInformationActivity.this.startActivity(userInfoIntent);
        }

    }

    /**
     * Triggered when the user clicks the "Invite friends" button.<br />
     * It launches InviteFriendsActivity for a result.
     * 
     * @param v
     * @author SpicyCH
     */
    public void inviteFriendsToEvent(View v) {
        // Hack so that SonarQube doesn't complain that v is not used
        Log.d(TAG, "View with id " + v.getId() + " clicked");
        Intent inviteFriends = new Intent(this, InviteFriendsActivity.class);
        inviteFriends.putExtra("EVENT", mEvent.getId());
        this.startActivity(inviteFriends);
    }

    @Override
    public void onBackPressed() {
        this.onNotificationOpen();
        this.finish();
    }

    /**
     * Triggered when "Attending" checkbox is clicked. Updates the displayed
     * list of participants.
     * 
     * @param v
     *            the checkbox whose status changed
     * @author agpmilli
     */
    public void onCheckboxClicked(View v) {

        if (!(v instanceof CheckBox)) {
            throw new IllegalArgumentException("This method requires v to be a CheckBox");
        }

        CheckBox checkBox = (CheckBox) v;

        if (v.getId() == R.id.event_info_going_checkbox) {
            if (checkBox.isChecked()) {
                ServiceContainer.getCache().addParticipantsToEvent(
                    new HashSet<Long>(Arrays.asList(ServiceContainer.getSettingsManager().getUserId())),
                    mEvent, new EventInscriptionCallback());
            } else {
                ServiceContainer.getCache().removeParticipantsFromEvent(
                    new HashSet<Long>(Arrays.asList(ServiceContainer.getSettingsManager().getUserId())),
                    mEvent, new EventUnsubscriptionCallback());
            }

        }
        this.updateCurrentList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.getMenuInflater().inflate(R.menu.show_event_information, menu);

        // Disable the delete event button if we are not the creator
        if ((mEvent == null) || !mEvent.isOwn() || !DELETE_EVENTS_ENABLE) {
            MenuItem item = menu.findItem(R.id.event_info_delete_button);
            item.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case android.R.id.home:
                this.onNotificationOpen();
                this.finish();
                break;
            case R.id.event_info_delete_button:
                ServiceContainer.getCache().removeEvent(mEvent.getId());
                this.finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        // This activity needs a (positive) event id to process. If none given,
        // we finish it.
        if (this.getIntent().getLongExtra(EVENT_KEY, -1) > 0) {

            long eventId = this.getIntent().getLongExtra(EVENT_KEY, -1);

            mEvent = ServiceContainer.getCache().getEvent(eventId);

            if (mEvent == null) {
                // This may occur when opening an invitation to a finished event
                Toast.makeText(this, this.getString(R.string.event_info_toast_eventno_longer_exists),
                    Toast.LENGTH_LONG).show();
                this.finish();
            } else {
                this.initializeGUI();
            }

        } else {
            Log.e(TAG, "No event id put in the putextra of the intent that started this activity.");
            Toast.makeText(EventInformationActivity.this,
                EventInformationActivity.this.getString(R.string.error_client_side), Toast.LENGTH_SHORT)
                .show();
            this.finish();
        }

    }

    /**
     * Triggered when the button 'Shop on the map' is pressed. Opens the map at
     * the location of the event.
     * 
     * @author SpicyCH
     */
    public void openMapAtEventLocation(View v) {
        Log.d(TAG, "View with id " + v.getId() + " pressed. Opening map at event location.");
        Intent showEventIntent = new Intent(this, MainActivity.class);
        showEventIntent.putExtra(AddEventActivity.LOCATION_EXTRA, mEvent.getLocation());
        this.startActivity(showEventIntent);
    }

    /**
     * Initializes the different views of this activity.
     * 
     * @author SpicyCH
     */
    private void initializeGUI() {
        if (mEvent == null) {
            // This may occur when cache listener updates the activity as it is
            // not destroyed when the activity finishes
            this.finish();
        } else {
            EventInformationActivity.this.setTitle(mEvent.getName());

            mEventTitle =
                (TextView) EventInformationActivity.this.findViewById(R.id.show_event_info_event_name);
            mEventTitle.setText(mEvent.getName());

            mEventCreator =
                (TextView) EventInformationActivity.this.findViewById(R.id.show_event_info_creator);
            mEventCreator.setText(mEvent.getCreator().getName());

            mStartDate =
                (TextView) EventInformationActivity.this.findViewById(R.id.show_event_info_start_date);
            mStartHour =
                (TextView) EventInformationActivity.this.findViewById(R.id.show_event_info_start_hour);
            mEndDate = (TextView) EventInformationActivity.this.findViewById(R.id.show_event_info_end_date);
            mEndHour = (TextView) EventInformationActivity.this.findViewById(R.id.show_event_info_end_hour);

            mGoingCheckBox =
                (CheckBox) EventInformationActivity.this.findViewById(R.id.event_info_going_checkbox);

            if (mEvent.isGoing()) {
                mGoingChecked = true;
                mGoingCheckBox.setChecked(mGoingChecked);
            }

            String startDate = Utils.getDateString(mEvent.getStartDate());
            String startHour = Utils.getTimeString(mEvent.getStartDate());
            String endDate = Utils.getDateString(mEvent.getEndDate());
            String endHour = Utils.getTimeString(mEvent.getEndDate());

            mStartDate.setText(startDate);
            mStartHour.setText(startHour);
            mEndDate.setText(endDate);
            mEndHour.setText(endHour);

            mEventDescription =
                (TextView) EventInformationActivity.this.findViewById(R.id.show_event_info_description);
            if ((mEvent.getDescription() == null) || mEvent.getDescription().isEmpty()) {
                mEventDescription.setText(EventInformationActivity.this
                    .getString(R.string.show_event_info_event_no_description));
            } else {
                mEventDescription.setText(mEvent.getDescription());
            }

            mPlaceNameAndCountry =
                (TextView) EventInformationActivity.this.findViewById(R.id.show_event_info_town_and_country);
            mPlaceNameAndCountry.setText(mEvent.getLocationString() + ", "
                + Utils.getCountryFromLocation(mEvent.getLocation()));

            EventInformationActivity.this.updateCurrentList();
        }
    }

    /**
     * Called when we want to quit this tab and if it was open by a
     * notification.
     */
    private void onNotificationOpen() {
        if (this.getIntent().getBooleanExtra("NOTIFICATION", false)) {
            this.startActivity(new Intent(this, MainActivity.class));
        }
    }

    /**
     * Update list of participant when we click on Going checkBox.
     */
    private void updateCurrentList() {

        if (ServiceContainer.getSearchEngine() == null) {
            ServiceContainer.setSearchEngine(new CachedSearchEngine());
        }

        Log.d(TAG, "Search Engine : " + ServiceContainer.getSearchEngine());
        Log.d(TAG, "Event : " + mEvent);
        Log.d(TAG, "Participants id : " + mEvent.getParticipantIds());

        ServiceContainer.getSearchEngine().findUsersByIds(mEvent.getParticipantIds(),
            new FindUsersIdSearchRequestCallback());

    }

    /**
     * Listener on cache changes used to update the event's
     * <code>ListView</code> when new events are added in the
     * cache.
     * 
     * @author SpicyCH
     */
    private class CacheChangesListener extends OnCacheListener {
        @Override
        public void onEventListUpdate() {
            EventInformationActivity.this.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    EventInformationActivity.this.initializeGUI();
                }

            });
        }

        @Override
        public void onUserListUpdate() {
            EventInformationActivity.this.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    EventInformationActivity.this.initializeGUI();
                }

            });
        }
    }

    /**
     * Called after the server responded to the event joining request.
     * 
     * @author SpicyCH
     */
    private class EventInscriptionCallback implements NetworkRequestCallback<Void> {
        @Override
        public void onFailure(Exception e) {
            EventInformationActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(EventInformationActivity.this,
                        EventInformationActivity.this.getString(R.string.event_going_failure),
                        Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onSuccess(Void result) {
            EventInformationActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(EventInformationActivity.this,
                        EventInformationActivity.this.getString(R.string.event_going_success),
                        Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Called after the server responded to the event leaving request.
     * 
     * @author SpicyCH
     */
    private class EventUnsubscriptionCallback implements NetworkRequestCallback<Void> {
        @Override
        public void onFailure(Exception e) {
            EventInformationActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(EventInformationActivity.this,
                        EventInformationActivity.this.getString(R.string.event_quit_failure),
                        Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onSuccess(Void result) {
            EventInformationActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(EventInformationActivity.this,
                        EventInformationActivity.this.getString(R.string.event_quit_success),
                        Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Called when the result has arrived.
     * 
     * @author SpicyCH
     */
    private class FindUsersIdSearchRequestCallback implements SearchRequestCallback<Set<User>> {
        @Override
        public void onNetworkError(Exception e) {
            EventInformationActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(EventInformationActivity.this,
                        EventInformationActivity.this.getString(R.string.refresh_participants_network_error),
                        Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onNotFound() {
            EventInformationActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(EventInformationActivity.this,
                        EventInformationActivity.this.getString(R.string.refresh_participants_not_found),
                        Toast.LENGTH_SHORT).show();
                }
            });

        }

        @Override
        public void onResult(Set<User> result) {
            mParticipantsList = new ArrayList<User>(result);
            EventInformationActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    EventInformationActivity.this.setListAdapter(new FriendPickerListAdapter(
                        EventInformationActivity.this, mParticipantsList));
                }
            });
        }
    }
}