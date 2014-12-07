package ch.epfl.smartmap.cache;

import android.content.Intent;
import android.util.Log;
import ch.epfl.smartmap.R;
import ch.epfl.smartmap.activities.FriendsPagerActivity;
import ch.epfl.smartmap.activities.UserInformationActivity;
import ch.epfl.smartmap.background.ServiceContainer;
import ch.epfl.smartmap.gui.Utils;

/**
 * A class to represent the user's invitations
 * 
 * @author agpmilli
 */
public class FriendInvitation implements Invitation {

    private static final String TAG = FriendInvitation.class.getSimpleName();

    private final long mUserId;
    private int mStatus;

    public FriendInvitation(ImmutableInvitation invitation) {
        super();
        mUserId = invitation.getUser();
        mStatus = invitation.getStatus();
    }

    @Override
    public long getId() {
        return mUserId;
    }

    @Override
    public Intent getIntent() {
        Intent intent = null;
        if ((mStatus == READ) || (mStatus == UNREAD)) {
            intent = new Intent(Utils.sContext, FriendsPagerActivity.class);
            intent.putExtra("INVITATION", true);
        } else if (mStatus == ACCEPTED) {
            intent = new Intent(Utils.sContext, UserInformationActivity.class);
            intent.putExtra("USER", mUserId);
        }
        return intent;
    }

    @Override
    public int getStatus() {
        return mStatus;
    }

    @Override
    public String getText() {
        return Utils.sContext.getResources().getString(R.string.notification_open_friend_list);
    }

    @Override
    public String getTitle() {
        return Utils.sContext.getResources().getString(R.string.notification_open_friend_list) + " "
            + ServiceContainer.getCache().getUser(mUserId).getName();
    }

    @Override
    public ImmutableUser getUser() {
        Log.d(TAG, "User id :" + mUserId + "\n All users : " + ServiceContainer.getCache().getAllFriends());
        return ServiceContainer.getCache().getUser(mUserId).getImmutableCopy();
    }

    @Override
    public void setStatus(int newStatus) {
        mStatus = newStatus;
    }

}