package ch.epfl.smartmap.gui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import ch.epfl.smartmap.R;
import ch.epfl.smartmap.cache.User;
import ch.epfl.smartmap.database.DatabaseHelper;
import ch.epfl.smartmap.gui.FriendListItemAdapter.FriendViewHolder;
import ch.epfl.smartmap.servercom.NetworkSmartMapClient;
import ch.epfl.smartmap.servercom.NotificationBag;
import ch.epfl.smartmap.servercom.SmartMapClientException;

/**
 * FIXME What to do with this class? Remove it? If not, should implement FriendsListener instead of sending
 * its
 * own requests?
 * Fragment diplaying your invitations in FriendsActivity
 * 
 * @author marion-S
 */
public class InvitationsTab extends ListFragment {

    private final Context mContext;

    private final NetworkSmartMapClient mNetworkClient;

    private final DatabaseHelper mDataBaseHelper;

    @SuppressWarnings("deprecation")
    public InvitationsTab(Context context) {
        mContext = context;
        mNetworkClient = NetworkSmartMapClient.getInstance();
        mDataBaseHelper = new DatabaseHelper(mContext);
    }

    /*
     * (non-Javadoc)
     * @see
     * android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater
     * , android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.list_fragment_invitations_tab, container, false);
        new RefreshInvitationsList().execute();
        return view;
    }

    /*
     * (non-Javadoc)
     * @see
     * android.support.v4.app.ListFragment#onListItemClick(android.widget.ListView
     * , android.view.View, int, long) When a list item is clicked, display a
     * dialog to ask whether to accept or decline the invitation
     */
    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        long userId = ((FriendViewHolder) view.getTag()).getUserId();
        RelativeLayout rl = (RelativeLayout) view;
        TextView tv = (TextView) rl.getChildAt(1);
        assert (tv instanceof TextView) && (tv.getId() == R.id.activity_friends_name);
        String name = tv.getText().toString();
        this.displayAcceptFriendDialog(name, userId);
    }

    /*
     * (non-Javadoc)
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();
        new RefreshInvitationsList().execute();
    }

    /**
     * A dialog to ask whether to accept or decline the invitation of the given
     * user
     * 
     * @param name
     *            the user's name
     * @param userId
     *            the user's id
     */
    private void displayAcceptFriendDialog(String name, final long userId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        builder.setMessage("Accept " + name + " to become your friend?");

        // Add positive button
        builder.setPositiveButton("Yes, accept", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                new AcceptInvitation().execute(userId);
            }
        });

        // Add negative button
        builder.setNegativeButton("No, decline", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                new DeclineInvitation().execute(userId);
            }
        });

        // display the AlertDialog
        builder.create().show();
    }

    /**
     * AsyncTask that notifies the server when the user accepts a friend request
     * also stores the new friend in the application cache
     * 
     * @author Marion-S
     */
    private class AcceptInvitation extends AsyncTask<Long, Void, String> {

        @Override
        protected String doInBackground(Long... params) {
            String confirmString = "";
            try {
                mNetworkClient.acceptInvitation(params[0]);
                mDataBaseHelper.addUser(NetworkSmartMapClient.getInstance().getFriendInfo(params[0]));
                confirmString = "Accepted";
            } catch (SmartMapClientException e) {
                confirmString = "Error";
            }
            return confirmString;
        }

        @Override
        protected void onPostExecute(String confirmString) {
            Toast.makeText(InvitationsTab.this.getActivity(), confirmString, Toast.LENGTH_LONG).show();
            new RefreshInvitationsList().execute();
        }

    }

    /**
     * AsyncTask that confirms the server that accepted invitations were
     * received
     * 
     * @author marion-S
     */
    private class AckAcceptedInvitations extends AsyncTask<Long, Void, Void> {

        @Override
        protected Void doInBackground(Long... params) {
            try {
                mNetworkClient.ackAcceptedInvitation(params[0]);
            } catch (SmartMapClientException e) {
            }
            return null;
        }

    }

    /**
     * AsyncTask that notifies the server when the user declines a friend
     * request
     * 
     * @author marion-S
     */
    private class DeclineInvitation extends AsyncTask<Long, Void, String> {

        @Override
        protected String doInBackground(Long... params) {
            String confirmString = "";
            try {
                NetworkSmartMapClient.getInstance().declineInvitation(params[0]);
                confirmString = "Declined";
            } catch (SmartMapClientException e) {
                confirmString = "Error";
            }
            return confirmString;
        }

        @Override
        protected void onPostExecute(String confirmString) {
            Toast.makeText(InvitationsTab.this.getActivity(), confirmString, Toast.LENGTH_LONG).show();
            new RefreshInvitationsList().execute();
        }

    }

    /**
     * AsyncTask that refreshes the invitations list after the user answered to
     * an invitation and each time the activity is resumed. It also retrieves
     * accepted invitations and store them in the application cache.
     * 
     * @author marion-S
     */
    private class RefreshInvitationsList extends AsyncTask<String, Void, NotificationBag> {

        @Override
        protected NotificationBag doInBackground(String... params) {
            try {

                return mNetworkClient.getInvitations();

            } catch (SmartMapClientException e) {
                // FIXME what to return??
                return null;
            }
        }

        @Override
        protected void onPostExecute(NotificationBag notificationBag) {
            super.onPostExecute(notificationBag);
            InvitationsTab.this.setListAdapter(new FriendListItemAdapter(mContext, notificationBag
                .getInvitingUsers()));
            for (User newFriend : notificationBag.getNewFriends()) {
                mDataBaseHelper.addUser(newFriend);
                new AckAcceptedInvitations().execute(newFriend.getId());
            }
        }

    }
}