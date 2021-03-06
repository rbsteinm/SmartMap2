package ch.epfl.smartmap.activities;

import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.Toast;
import ch.epfl.smartmap.R;
import ch.epfl.smartmap.background.ServiceContainer;
import ch.epfl.smartmap.cache.User;
import ch.epfl.smartmap.callbacks.NetworkRequestCallback;
import ch.epfl.smartmap.callbacks.SearchRequestCallback;
import ch.epfl.smartmap.gui.FriendListItemAdapter;
import ch.epfl.smartmap.gui.FriendListItemAdapter.FriendViewHolder;

/**
 * This Activity displays a list of users from the DB and lets you send them
 * friend requests
 * 
 * @author rbsteinm
 */
public class AddFriendActivity extends ListActivity {

    private SearchView mSearchBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_add_friend);

        ServiceContainer.initSmartMapServices(this);

        // Set action bar color to main color
        this.getActionBar().setBackgroundDrawable(
            new ColorDrawable(this.getResources().getColor(R.color.main_blue)));
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        long userId = ((FriendViewHolder) view.getTag()).getUserId();
        Intent intent =
            new Intent(AddFriendActivity.this.getApplicationContext(), UserInformationActivity.class);
        intent.putExtra("USER", userId);
        this.startActivity(intent);
        this.finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.getMenuInflater().inflate(R.menu.add_friend, menu);
        mSearchBar = (SearchView) menu.findItem(R.id.add_friend_activity_searchBar).getActionView();
        this.setSearchBarListener();
        MenuItem searchMenuItem = menu.findItem(R.id.add_friend_activity_searchBar);
        searchMenuItem.expandActionView();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.add_friend_activity_searchBar) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * sets a listener on the searchbar that updates the views (users) displayed
     * every time the text chnanges or is
     * submitted
     */
    private void setSearchBarListener() {
        mSearchBar.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                ServiceContainer.getSearchEngine().findStrangersByName(newText, new FindFriendsCallback());
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String newText) {
                ServiceContainer.getSearchEngine().findStrangersByName(newText, new FindFriendsCallback());
                return true;
            }
        });
    }

    /**
     * Display a confirmation dialog.
     * 
     * @param name
     * @param userId
     */
    public static void displayConfirmationDialog(final Activity activity, String name, final long userId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(activity.getResources().getString(R.string.add) + " " + name + " "
            + activity.getResources().getString(R.string.as_a_friend));

        // Add positive button
        builder.setPositiveButton(activity.getResources().getString(R.string.add),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    // invite friend
                    ServiceContainer.getCache().inviteUser(userId, new AddFriendCallback(activity));
                }
            });
        // Add negative button
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        // display the AlertDialog
        builder.create().show();
    }

    /**
     * Callback that describes connection with network
     * 
     * @author agpmilli
     */
    private static class AddFriendCallback implements NetworkRequestCallback<Void> {

        private final Activity mActivity;

        public AddFriendCallback(Activity activity) {
            mActivity = activity;
        }

        @Override
        public void onFailure(Exception e) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mActivity, mActivity.getString(R.string.invite_friend_failure),
                        Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onSuccess(Void result) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mActivity, mActivity.getString(R.string.invite_friend_success),
                        Toast.LENGTH_SHORT).show();
                    mActivity.finish();
                }
            });
        }
    }

    /**
     * Callback that describes connection with SearchRequest
     * 
     * @author Pamoi
     */
    private class FindFriendsCallback implements SearchRequestCallback<Set<User>> {

        @Override
        public void onNetworkError(Exception e) {
            AddFriendActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(AddFriendActivity.this,
                        AddFriendActivity.this.getResources().getString(R.string.add_friend_network_error),
                        Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onNotFound() {
            AddFriendActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(AddFriendActivity.this,
                        AddFriendActivity.this.getResources().getString(R.string.add_friend_not_found),
                        Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onResult(final Set<User> result) {
            AddFriendActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AddFriendActivity.this.setListAdapter(new FriendListItemAdapter(AddFriendActivity.this,
                        new ArrayList<User>(result)));
                }
            });
        }
    }
}