package ch.epfl.smartmap.cache;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import android.graphics.Bitmap;
import android.location.Location;
import ch.epfl.smartmap.R;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * An event that can be seen on the map
 * 
 * @author ritterni
 */
public class PublicEvent implements Event {

    private long mId;
    private String mName;
    private User mCreator;
    private String mDescription;
    private Location mLocation;
    private String mLocationString;
    private GregorianCalendar mStartDate;
    private GregorianCalendar mEndDate;
    private List<Long> mParticipants;

    public final static int DEFAULT_ICON = R.drawable.default_event;

    public static final float MARKER_ANCHOR_X = (float) 0.5;
    public static final float MARKER_ANCHOR_Y = 1;

    protected PublicEvent(ImmutableEvent event) {
        mCreator = Cache.getInstance().getUserById(event.getCreatorId());
        mName = event.getName();
        mStartDate = new GregorianCalendar(TimeZone.getDefault());
        mEndDate = new GregorianCalendar(TimeZone.getDefault());
        mStartDate.setTime(event.getStartDate().getTime());
        mEndDate.setTime(event.getEndDate().getTime());
        mLocation = new Location(event.getLocation());
        mLocationString = event.getLocationString();
        mDescription = event.getDescription();

        // TODO : Handle listeners
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        return (obj != null) && (this.getClass() == obj.getClass())
            && (this.getId() == ((PublicEvent) obj).getId());
    }

    @Override
    public long getCreatorId() {
        return mCreator.getId();
    }

    @Override
    public String getDescription() {
        return mDescription;
    }

    @Override
    public Calendar getEndDate() {
        return (Calendar) mEndDate.clone();
    }

    @Override
    public long getId() {
        return mId;
    }

    @Override
    public Bitmap getImage() {
        return Event.DEFAULT_IMAGE;
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.Localisable#getLatLng()
     */
    @Override
    public LatLng getLatLng() {
        return new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
    }

    @Override
    public Location getLocation() {
        return mLocation;
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.Localisable#getLocationString()
     */
    @Override
    public String getLocationString() {
        return mLocationString;
    }

    /*
     * (non-Javadoc)
     * @see
     * ch.epfl.smartmap.cache.Displayable#getMarkerOptions(android.content.Context
     * )
     * @author hugo-S
     */
    @Override
    public MarkerOptions getMarkerOptions() {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()))
            .title(this.getName())
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            .anchor(MARKER_ANCHOR_X, MARKER_ANCHOR_Y);
        return markerOptions;

    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public GregorianCalendar getStartDate() {
        return mStartDate;
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.Displayable#getSubtitle()
     */
    @Override
    public String getSubtitle() {
        return mDescription;
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.Displayable#getTitle()
     */
    @Override
    public String getTitle() {
        return mName;
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.Event#getType()
     */
    @Override
    public Type getType() {
        return Type.PUBLIC;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return (int) mId;
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.Localisable#isShown()
     */
    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public void update(ImmutableEvent event) {

    }
}
