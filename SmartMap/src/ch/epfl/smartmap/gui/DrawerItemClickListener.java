package ch.epfl.smartmap.gui;

import ch.epfl.smartmap.activities.FriendsActivity;
import ch.epfl.smartmap.activities.ShowEventsActivity;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * Listener handling the clics on each element in the left drawer menu
 * @author rbsteinm
 */
public class DrawerItemClickListener implements ListView.OnItemClickListener {

    private static final int INDEX_PROFILE = 0;
    private static final int INDEX_FRIENDS = 1;
    private static final int INDEX_EVENTS = 2;
    private static final int INDEX_FILTERS = 3;
    private static final int INDEX_SETTINGS = 4;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case INDEX_PROFILE:
                break;
            case INDEX_FRIENDS:
                view.getContext().startActivity(new Intent(view.getContext(), FriendsActivity.class));
                break;
            case INDEX_EVENTS:
                view.getContext().startActivity(new Intent(view.getContext(), ShowEventsActivity.class));
                break;
            case INDEX_FILTERS:
                break;
            case INDEX_SETTINGS:
                break;
            default:
                break;
        }
    }
}
