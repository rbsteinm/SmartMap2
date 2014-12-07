package ch.epfl.smartmap.gui;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import ch.epfl.smartmap.R;
import ch.epfl.smartmap.background.ServiceContainer;
import ch.epfl.smartmap.cache.FriendInvitation;
import ch.epfl.smartmap.cache.ImmutableUser;
import ch.epfl.smartmap.cache.Invitation;
import ch.epfl.smartmap.database.DatabaseHelper;
import ch.epfl.smartmap.servercom.SmartMapClientException;

/**
 * TODO listen to invitation list?
 * Fragment diplaying your invitations in FriendsActivity
 * 
 * @author marion-S
 */
public class InvitationsTab extends ListFragment {

    private final Context mContext;
    private List<FriendInvitation> mInvitationList;

    // private final NetworkSmartMapClient mNetworkClient;

    private DatabaseHelper mDBHelper;

    public InvitationsTab(Context context) {
        mContext = context;

        // mNetworkClient = NetworkSmartMapClient.getInstance();
        // mDataBaseHelper = DatabaseHelper.initialize(mContext);
    }

    /*
     * (non-Javadoc)
     * @see
     * android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater
     * , android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.list_fragment_friends_tab, container, false);
        mDBHelper = ServiceContainer.getDatabase();
        mInvitationList = new ArrayList<FriendInvitation>();
        mInvitationList = mDBHelper.getUnansweredFriendInvitations();

        // Create custom Adapter and pass it to the Activity
        FriendInvitationListItemAdapter adapter =
            new FriendInvitationListItemAdapter(mContext, mInvitationList);
        this.setListAdapter(adapter);

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

        FriendInvitation invitation = mInvitationList.get(position);

        this.displayAcceptFriendDialog(invitation);
    }

    /*
     * (non-Javadoc)
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();
        // new RefreshInvitationsList().execute();
        mInvitationList = mDBHelper.getUnansweredFriendInvitations();
        this.setListAdapter(new FriendInvitationListItemAdapter(mContext, mInvitationList));
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
    private void displayAcceptFriendDialog(final FriendInvitation invitation) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        builder.setMessage("Accept " + invitation.getUserName() + " to become your friend?");

        // Add positive button
        builder.setPositiveButton("Yes, accept", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                invitation.setStatus(Invitation.ACCEPTED);
                // TODO add invitation in cache instead of db
                mDBHelper.updateFriendInvitation(invitation);
                ImmutableUser friend;
                try {
                    friend = ServiceContainer.getNetworkClient().getUserInfo(invitation.getUserId());
                    ServiceContainer.getCache().putFriend(friend);
                    mInvitationList = mDBHelper.getUnansweredFriendInvitations();
                } catch (SmartMapClientException e) {
                    // TODO : Handle this case
                }
                InvitationsTab.this.setListAdapter(new FriendInvitationListItemAdapter(mContext,
                    mInvitationList));

            }
        });

        // Add negative button
        builder.setNegativeButton("No, decline", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // new DeclineInvitation().execute(userId);
                invitation.setStatus(Invitation.REFUSED);
                mDBHelper.updateFriendInvitation(invitation);
                mInvitationList = mDBHelper.getUnansweredFriendInvitations();
                InvitationsTab.this.setListAdapter(new FriendInvitationListItemAdapter(mContext,
                    mInvitationList));
            }
        });

        // display the AlertDialog
        builder.create().show();
    }
}