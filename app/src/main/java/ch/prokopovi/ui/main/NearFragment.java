package ch.prokopovi.ui.main;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.androidmapsextensions.ClusterGroup;
import com.androidmapsextensions.ClusteringSettings;
import com.androidmapsextensions.GoogleMap;
import com.androidmapsextensions.GoogleMap.InfoWindowAdapter;
import com.androidmapsextensions.GoogleMap.OnCameraChangeListener;
import com.androidmapsextensions.GoogleMap.OnMarkerClickListener;
import com.androidmapsextensions.Marker;
import com.androidmapsextensions.MarkerOptions;
import com.androidmapsextensions.SupportMapFragment;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.prokopovi.R;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.OperationType;
import ch.prokopovi.struct.Master.Region;
import ch.prokopovi.struct.best.RateItem;
import ch.prokopovi.struct.best.RatePoint;
import ch.prokopovi.ui.main.api.UpdateListener;
import ch.prokopovi.ui.main.api.Updater;

/**
 * near places map fragment
 * <p/>
 * for separate impl. of sherlock map activity see
 * http://stackoverflow.com/questions
 * /13721929/using-actionbarsherlock-with-the-new-supportmapfragment
 *
 * @author Pavel_Letsiaha
 */
public class NearFragment extends SupportMapFragment implements UpdateListener {

    private static final int DEFAULT_ZOOM = 12;

    private static final String LOG_TAG = "NearFragment";

    private static final float ZOOM_CLASTER_THRESHOLD = 18.0f;

    private GoogleAnalyticsTracker tracker;

    private GoogleMap map;

    private Updater updater;

    private NearInfoWindowAdapter infoWindowAdapter;
    private final NearCameraListener cameraListener = new NearCameraListener();
    private final NearMarkerClickListener markerClickListener = new NearMarkerClickListener();

    private Region selectedRegion;

    private final SparseArray<RatePoint> places = new SparseArray<RatePoint>();

    private Map<CurrencyCode, Map<OperationType, Double>> bestRates;

    private boolean firstTimeOpen = true;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.d(LOG_TAG, "onActivityCreated");

        if (getActivity() == null)
            return;

        this.tracker = this.updater.getTracker();
        this.tracker.trackPageView("/near");

        this.map = getExtendedMap();

        if (!checkReady())
            return;

        this.map.setClustering(buildClusteringSettings(true));

        this.map.setMyLocationEnabled(true);

        this.infoWindowAdapter = new NearInfoWindowAdapter(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(LOG_TAG, "onCreateView");

        View v = super.onCreateView(inflater, container, savedInstanceState);

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        v.setLayoutParams(new LinearLayout.LayoutParams(0,
                LayoutParams.MATCH_PARENT, 1 - TabsActivity.DUAL_PANE_RATIO));

        return v;
    }

    private ClusteringSettings buildClusteringSettings(boolean withClustering) {

        ClusteringSettings settings = new ClusteringSettings().enabled(
                withClustering).addMarkersDynamically(true);

        if (withClustering) {
            settings = settings //
                    .clusterOptionsProvider(
                            new ClusterDataProvider(getResources())) //
                    .clusterSize(96);
        }

        return settings;
    }

    private void listenersOn() {
        Log.d(LOG_TAG, "listenersOn");

        if (!checkReady())
            return;

        this.map.setInfoWindowAdapter(this.infoWindowAdapter);

        this.updater.addUpdateListener(this);
        this.map.setOnCameraChangeListener(this.cameraListener);
        this.map.setOnMarkerClickListener(this.markerClickListener);
    }

    private void listenersOff() {
        Log.d(LOG_TAG, "listenersOff");

        if (!checkReady())
            return;

        this.map.setInfoWindowAdapter(null);

        this.map.setOnMarkerClickListener(null);
        this.map.setOnCameraChangeListener(null);
        this.updater.removeUpdateListener(this);
    }

    @Override
    public void onResume() {

        Log.d(LOG_TAG, "onResume");

        super.onResume();

        if (!checkReady())
            return;

        listenersOn();

        this.firstTimeOpen = true;

        LatLng currentPosition = this.updater.getMapPosition();
        if (currentPosition != null)
            this.map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    currentPosition, DEFAULT_ZOOM));
    }

    @Override
    public void onPause() {

        Log.d(LOG_TAG, "onPause");

        super.onPause();

        listenersOff();
    }

    @Override
    public void onAttach(Activity activity) {

        Log.d(LOG_TAG, "onAttach");

        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            this.updater = (Updater) activity;
            this.updater.addUpdateListener(this);

        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement Updater");
        }
    }

    /**
     * Checks if the map is ready (which depends on whether the Google Play
     * services APK is available. This should be called prior to calling any
     * methods on GoogleMap.
     */
    private boolean checkReady() {
        if (this.map == null) {
            Log.w(LOG_TAG, "map is not ready");
            return false;
        }
        return true;
    }

    /**
     * update UI with new location
     *
     * @param position
     */
    private void newPosition(LatLng position) {
        Log.d(LOG_TAG, "--- new position: " + position);

        if (position == null) {
            Log.w(LOG_TAG, "null position is not allowed");
            return;
        }

        if (!checkReady())
            return;

        Region positionRegion = TabsActivity.findRegion(position.latitude,
                position.longitude);
        if (positionRegion == null) {
            Log.w(LOG_TAG, "region for new position is not found");
            return;
        }

        Region oldRegion = this.selectedRegion;

        int markersSize = this.map.getMarkers().size();

        boolean sameRegion = positionRegion.equals(oldRegion);
        if (!sameRegion || markersSize == 0) {
            Log.d(LOG_TAG, "full region position update");
            this.selectedRegion = positionRegion;

            updatePlaces();

            this.bestRates = NearFragment.this.updater
                    .getBestRates(NearFragment.this.selectedRegion);
        }

        showNearestWindow();
    }

    private class NearCameraListener implements OnCameraChangeListener {

        @Override
        public void onCameraChange(CameraPosition position) {

            if (!checkReady())
                return;

            boolean withClustering = position.zoom < ZOOM_CLASTER_THRESHOLD;
            NearFragment.this.map
                    .setClustering(buildClusteringSettings(withClustering));

            newPosition(position.target);
        }
    }

    private class NearMarkerClickListener implements OnMarkerClickListener {

        @Override
        public boolean onMarkerClick(Marker marker) {

            NearFragment.this.tracker.trackPageView("/markerClick");

            if (!checkReady())
                return true;

            boolean cluster = marker.isCluster();

            if (cluster) {
                List<Marker> markers = marker.getMarkers();
                Builder builder = LatLngBounds.builder();
                for (Marker m : markers) {
                    builder.include(m.getPosition());
                }
                LatLngBounds bounds = builder.build();
                NearFragment.this.map.animateCamera(CameraUpdateFactory
                        .newLatLngBounds(bounds, getResources()
                                .getDimensionPixelSize(R.dimen.padding)));
            }

            return cluster; // consume event or not
        }
    }

    private class NearInfoWindowAdapter implements InfoWindowAdapter {

        private final LayoutInflater inflater;
        private final View mWindow;

        NearInfoWindowAdapter(Bundle savedInstanceState) {
            this.inflater = getLayoutInflater(savedInstanceState);

            this.mWindow = this.inflater.inflate(R.layout.custom_info_window,
                    null);
        }

        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }

        @Override
        public View getInfoWindow(Marker marker) {

            try {
                Integer id = Integer.valueOf(marker.getSnippet());

                RatePoint place = NearFragment.this.places.get(id);

                List<RateItem> list = NearFragment.this.updater.getRates(id);

                if (place != null) {

                    LinearLayout table = (LinearLayout) this.mWindow
                            .findViewById(R.id.rates_table);

                    renderTextView(this.mWindow, R.id.title,
                            place.placeDescription);
                    renderTextView(this.mWindow, R.id.work_hours,
                            place.workHours);

                    // if have rates
                    boolean hasValue = !list.isEmpty();

                    table.removeAllViews(); // cleanup

                    if (hasValue) {

                        for (RateItem item : list) {

                            CurrencyCode currency = item.currency;
                            String title = getResources().getString(
                                    currency.getTitleRes());

                            OperationType operaton = item.operationType;

                            Double rateVal = item.value;
                            Double bestVal = NearFragment.this.bestRates.get(
                                    currency).get(operaton);

                            Double diffVal = bestVal - rateVal;

                            String value = TabsActivity.FMT_RATE_VALUE
                                    .format(rateVal);

                            String diff = "";
                            if (diffVal > -0.0001 && diffVal < 0.0001) {
                                // zero diff
                                diff = getResources().getString(
                                        R.string.lbl_best);
                            } else {
                                diff = TabsActivity.FMT_RATE_VALUE
                                        .format(diffVal);
                            }

                            View row = getRow(table, currency.getId());

                            boolean buy = OperationType.BUY.equals(operaton);

                            int valueId = buy ? R.id.curr_buy : R.id.curr_sell;
                            int diffId = buy ? R.id.curr_buy_diff
                                    : R.id.curr_sell_diff;

                            renderTextView(row, R.id.curr_title, title);
                            renderTextView(row, valueId, value);
                            renderTextView(row, diffId, diff);
                        }
                    }

                    UiHelper.applyFont(getActivity(), this.mWindow, null);
                } else {
                    Log.e(LOG_TAG, "can't find place with id: " + id);
                }

            } catch (Exception e) {
                Log.e(LOG_TAG, "can't show marker " + marker, e);
            }

            // Returning the view containing InfoWindow contents
            return this.mWindow;
        }

        private View getRow(LinearLayout table, int id) {

            // search existing
            int len = table.getChildCount();
            for (int i = 0; i < len; i++) {
                View child = table.getChildAt(i);

                if (child.getId() == id) {
                    return child;
                }
            }

            // new row
            View row = this.inflater.inflate(R.layout.info_window_item, null);
            row.setId(id);
            table.addView(row);

            return row;
        }

        private void renderTextView(View parent, int textViewId,
                                    String textViewVal) {
            TextView titleUi = ((TextView) parent.findViewById(textViewId));
            titleUi.setText(textViewVal);
        }

    }

    /**
     * set markers on map according provided places
     *
     * @param places
     */
    private synchronized void reloadMapItems(SparseArray<RatePoint> places) {

        if (!checkReady())
            return;

        this.map.clear();

        Set<Integer> added = new HashSet<Integer>();

        // Loop through all the items that are available to be placed on the
        // map
        for (int i = 0; i < places.size(); i++) {
            RatePoint place = places.valueAt(i);

            int id = place.id;

            if (added.contains(id)) {
                continue;
            }

            this.map.addMarker(new MarkerOptions() //
                    .snippet(String.valueOf(id)) //
                    .position(new LatLng(place.x, place.y)) //
            );

            added.add(id);
        }
    }

    // TODO handle case when same point have more then one marker (with and
    // without rates data)
    private Marker getNearestMarker(LatLng toPos, SparseArray<RatePoint> places) {
        Marker marker = null;

        if (toPos != null) {

            RatePoint nearestPlace = null;
            float nearestDistance = -1;

            for (int i = 0; i < places.size(); i++) {
                RatePoint p = places.valueAt(i);

                double myLat = toPos.latitude;
                double myLng = toPos.longitude;
                float[] result = new float[1];
                double lat = p.x;
                double lng = p.y;

                Location.distanceBetween(myLat, myLng, lat, lng, result);

                if (nearestDistance == -1 || nearestDistance > result[0]) {
                    nearestDistance = result[0];
                    nearestPlace = p;
                }
            }

            Log.d(LOG_TAG, "nearest place: " + nearestPlace);

            marker = recursiveFind(String.valueOf(nearestPlace.id),
                    this.map.getMarkers());

        } else {
            Log.d(LOG_TAG,
                    "since current position is undefined, nearest marker cant't be found.");
        }

        return marker;
    }

    /**
     * find marker by snippet recursing by cluster
     *
     * @param snippet
     * @param markers list to iterate
     * @return first marker with provided snippet or null if nothing found
     */
    private static Marker recursiveFind(String snippet, List<Marker> markers) {

        Marker res = null;

        for (Marker marker : markers) {
            if (marker.isCluster()) {
                res = recursiveFind(snippet, marker.getMarkers());
                break;
            } else {
                if (snippet.equals(marker.getSnippet())) {
                    res = marker;
                    break;
                }
            }
        }

        return res;
    }

    /**
     * list non-cluster markers in user-viewable region of the map
     *
     * @return
     */
    private List<Marker> getViewportMarkers() {
        List<Marker> markers = new ArrayList<Marker>();

        if (!checkReady())
            return markers;

        // This is the current user-viewable region of the map
        LatLngBounds bounds = this.map.getProjection().getVisibleRegion().latLngBounds;

        List<Marker> displayedMarkers = this.map.getDisplayedMarkers();
        for (Marker m : displayedMarkers) {
            boolean inBounds = bounds.contains(m.getPosition());
            if (inBounds && !m.isCluster() && m.isVisible()) {
                markers.add(m);
            }
        }

        return markers;
    }

    @Override
    public synchronized void onUpdate() {

        Log.d(LOG_TAG, "onUpdate");

        if (!checkReady())
            return;

        showNearestWindow();
    }

    private void showNearestWindow() {
        LatLng currentPosition = this.updater.getMapPosition();
        if (currentPosition != null && this.firstTimeOpen) {
            Marker nearestMarker = getNearestMarker(currentPosition,
                    this.places);

            if (nearestMarker != null) {
                showInfoWindow(nearestMarker);

                this.firstTimeOpen = false;
            }
        }
    }

    private void updatePlaces() {

        this.places.clear();

        SparseArray<RatePoint> newPlaces = this.updater
                .getPlaces(this.selectedRegion);
        for (int i = 0; i < newPlaces.size(); i++) {
            RatePoint place = newPlaces.valueAt(i);
            this.places.put(place.id, place);
        }

        reloadMapItems(this.places);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.near_menu, menu);
    }

    private void showInfoWindow(final Marker marker) {

        if (!checkReady())
            return;

        float zoomLevel = this.map.getMinZoomLevelNotClustered(marker);

        zoomLevel = zoomLevel < 1.0 ? ZOOM_CLASTER_THRESHOLD : zoomLevel; // too
        // big

        float origZoom = this.map.getCameraPosition().zoom;

        zoomLevel = origZoom > zoomLevel ? origZoom : zoomLevel; // do not zoom
        // out

        Log.d(LOG_TAG, "showing marker: " + marker + " at zoom:" + zoomLevel);

        this.map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                marker.getPosition(), zoomLevel));

        marker.setClusterGroup(ClusterGroup.NOT_CLUSTERED);

        marker.showInfoWindow();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Activity context = this.getActivity();

        if (context == null)
            return false;

        // item don't need map
        int menuItemId = item.getItemId();
        switch (menuItemId) {
            case R.id.menu_refresh:
                Log.d(LOG_TAG, "updating...");

                this.tracker.trackPageView("/menuUpdate");

                this.updater.read(this.selectedRegion, true);

                return true;
        }

        // ------

        // map dependent items
        if (!checkReady())
            return false;

        // filter not cluster markers
        List<Marker> markers = getViewportMarkers();
        int size = markers.size();

        switch (menuItemId) {
            case R.id.menu_near_prev:
                Log.d(LOG_TAG, "prev marker");

                this.tracker.trackPageView("/nearPrev");

                if (size == 0) {
                    return true;
                }

                for (int j = size - 1; j >= 0; j--) {
                    Marker m = markers.get(j);

                    if (m.isInfoWindowShown()) {
                        m.hideInfoWindow();

                        if (j > 0) {
                            // show prev marker
                            final Marker marker = markers.get(j - 1);
                            showInfoWindow(marker);
                            return true;
                        }

                        break;
                    }
                }

                // if get there (no info windows shown or no prev marker) - show
                // last marker
                final Marker marker = markers.get(size - 1);
                showInfoWindow(marker);

                return true;
            case R.id.menu_near_next:
                Log.d(LOG_TAG, "next marker");

                this.tracker.trackPageView("/nearNext");

                if (size == 0) {
                    return true;
                }

                for (int i = 0; i < size; i++) {
                    Marker m = markers.get(i);

                    if (m.isInfoWindowShown()) {
                        m.hideInfoWindow();

                        if (i < size - 1) {
                            // show next marker
                            final Marker nextMarker = markers.get(i + 1);
                            showInfoWindow(nextMarker);

                            return true;
                        }

                        break;
                    }
                }

                // if get there (no info windows shown or no next marker) - show
                // first marker
                final Marker firstMarker = markers.get(0);
                showInfoWindow(firstMarker);

                return true;
        }

        return false; // should never happen
    }
}
