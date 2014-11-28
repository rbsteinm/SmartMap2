package ch.epfl.smartmap.cache;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * @author jfperren
 */
public interface Localisable extends Stockable {

    String NO_LOCATION_STRING = "Unknown Location";
    String PROVIDER_NAME = "SmartMapServers";
    Location NO_LOCATION = new Location(PROVIDER_NAME);
    MarkerOptions NO_MARKER_OPTIONS = null;

    LatLng getLatLng();

    /**
     * @return GoogleMap Location of the Displayable
     */
    Location getLocation();

    String getLocationString();

    /**
     * @param context
     *            , the application's context
     * @return the options to display the marker
     */
    MarkerOptions getMarkerOptions();

    boolean isShown();

    void setLocation(Location newLocation);
}
