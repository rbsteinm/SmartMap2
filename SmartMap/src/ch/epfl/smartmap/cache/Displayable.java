package ch.epfl.smartmap.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;

/**
 * Objects that can be displayed with image, title and subtitle.
 * 
 * @author ritterni
 * @author jfperren
 */
public interface Displayable extends Stockable {

    // final Bitmap NO_IMAGE = Bitmap.createBitmap(R.drawable.default_event);
    String NO_TITLE = "";
    String NO_SUBTITLE = "";
    Bitmap DEFAULT_IMAGE = null;

    String NO_LOCATION_STRING = "Unknown Location";

    String PROVIDER_NAME = "SmartMapServers";

    Location NO_LOCATION = new Location(PROVIDER_NAME);

    BitmapDescriptor NO_MARKER_ICON = null;

    /**
     * @param context
     *            The application's context, needed to access the memory
     * @return The object's picture
     */
    Bitmap getImage();

    LatLng getLatLng();

    /**
     * @return GoogleMap Location of the Displayable
     */
    Location getLocation();

    String getLocationString();

    /**
     * @return Text containing various information (description, last seen,
     *         etc.)
     */
    String getSubtitle();

    /**
     * @return A name for the panel (e.g. the username, event name, etc.)
     */
    String getTitle();

    boolean isVisible();

    BitmapDescriptor getMarkerIcon(Context context);

}
