package ch.prokopovi.ui.main;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
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
public class NearFragment extends SupportMapFragment
        implements UpdateListener,
        ClusterManager.OnClusterClickListener<NearFragment.NearPlace>,
        ClusterManager.OnClusterInfoWindowClickListener<NearFragment.NearPlace>,
        ClusterManager.OnClusterItemClickListener<NearFragment.NearPlace>,
        ClusterManager.OnClusterItemInfoWindowClickListener<NearFragment.NearPlace>,
        GoogleMap.OnCameraChangeListener {

    private static final int MAX_NON_CLUSTERED_SIZE = 6;

    private static final String LOG_TAG = "NearFragment";

    private static final float ZOOM_CLASTER_THRESHOLD = 18.0f;

    private GoogleAnalyticsTracker tracker;

    private Updater updater;

    private Region selectedRegion;

    private final SparseArray<NearPlace> places = new SparseArray<>();

    private Map<CurrencyCode, Map<OperationType, Double>> bestRates;

    private boolean firstTimeOpen = true;

    private NearRenderer nearRenderer;
    private ClusterManager<NearPlace> mClusterManager;

    private NearPlace selectedClusterItem;

    class NearPlace implements ClusterItem {

        private RatePoint ratePoint;

        public NearPlace(RatePoint p) {
            ratePoint = p;
        }

        @Override
        public LatLng getPosition() {
            return new LatLng(ratePoint.x, ratePoint.y);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.d(LOG_TAG, "onActivityCreated");

        if (getActivity() == null)
            return;

        this.tracker = this.updater.getTracker();
        this.tracker.trackPageView("/near");

        if (!checkReady())
            return;

        getMap().setMyLocationEnabled(true);

        if (mClusterManager == null) {

            mClusterManager = new ClusterManager<>(getActivity(), getMap());
            nearRenderer = new NearRenderer(getActivity(), getMap(), mClusterManager);
            mClusterManager.setRenderer(nearRenderer);

            mClusterManager.getMarkerCollection()
                    .setOnInfoWindowAdapter(new NearInfoWindowAdapter(savedInstanceState));

            getMap().setOnCameraChangeListener(this);
            //getMap().setOnCameraChangeListener(mClusterManager);

            getMap().setOnMarkerClickListener(mClusterManager);
            getMap().setOnInfoWindowClickListener(mClusterManager.getMarkerManager());
            getMap().setInfoWindowAdapter(mClusterManager.getMarkerManager());

            mClusterManager.setOnClusterClickListener(this);
            mClusterManager.setOnClusterInfoWindowClickListener(this);
            mClusterManager.setOnClusterItemClickListener(this);
            mClusterManager.setOnClusterItemInfoWindowClickListener(this);
        }

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

    @Override
    public void onResume() {

        Log.d(LOG_TAG, "onResume");

        super.onResume();

        if (!checkReady())
            return;

        this.updater.addUpdateListener(this);

        this.firstTimeOpen = true;

        LatLng currentPosition = this.updater.getMapPosition();
        if (currentPosition != null)
            getMap().moveCamera(CameraUpdateFactory.newLatLng(
                    currentPosition));
    }

    @Override
    public void onPause() {

        Log.d(LOG_TAG, "onPause");

        super.onPause();

        if (checkReady()) {
            this.updater.removeUpdateListener(this);
        }
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
        if (getMap() == null) {
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

        int size = places.size();

        boolean sameRegion = positionRegion.equals(oldRegion);
        if (!sameRegion || size == 0) {
            Log.d(LOG_TAG, "full region position update");
            this.selectedRegion = positionRegion;

            updatePlaces();

            this.bestRates = NearFragment.this.updater
                    .getBestRates(NearFragment.this.selectedRegion);
        }

        showNearestWindow(position);
    }


    @Override
    public void onCameraChange(CameraPosition position) {

            if (!checkReady())
                return;

        newPosition(position.target);

        mClusterManager.onCameraChange(position);
    }

    @Override
    public boolean onClusterClick(Cluster<NearPlace> сluster) {
        NearFragment.this.tracker.trackPageView("/clusterClick");

        if (!checkReady())
            return true;

        goToBounds(сluster.getItems());

        return true;
    }

    private void goToBounds(Collection<NearPlace> items) {
        if (!checkReady())
            return;

        Builder builder = LatLngBounds.builder();
        for (NearPlace item : items) {
            builder.include(item.getPosition());
        }
        LatLngBounds bounds = builder.build();
        getMap().animateCamera(CameraUpdateFactory
                .newLatLngBounds(bounds, getResources()
                        .getDimensionPixelSize(R.dimen.padding)));
    }

    @Override
    public void onClusterInfoWindowClick(Cluster<NearPlace> cluster) {
        // Does nothing, but you could go to a list of the users.
    }

    @Override
    public boolean onClusterItemClick(NearPlace item) {
        selectedClusterItem = item;
        return false;
    }

    @Override
    public void onClusterItemInfoWindowClick(NearPlace item) {
        // Does nothing, but you could go into the user's profile page, for example.
    }

    /**
     * Draws profile photos inside markers (using IconGenerator).
     * When there are multiple people in the cluster, draw multiple photos (using MultiDrawable).
     */
    private class NearRenderer extends DefaultClusterRenderer<NearPlace> {
        private final IconGenerator iconFactory;

        public NearRenderer(Context context,
                            GoogleMap map,
                            ClusterManager<NearPlace> clusterManager) {
            super(context, map, clusterManager);
            iconFactory = new IconGenerator(context);
        }

        @Override
        protected void onClusterItemRendered(NearPlace clusterItem, Marker marker) {
            super.onClusterItemRendered(clusterItem, marker);
        }

        @Override
        protected void onBeforeClusterItemRendered(NearPlace item, MarkerOptions markerOptions) {

            List<RateItem> list = NearFragment.this.updater.getRates(item.ratePoint.id);

            String text;
            if (!list.isEmpty()) {
                RateItem rate = list.get(0);

                String title = getResources().getString(
                        rate.currency.getTitleRes());

                text = title + " " + rate.value;
            } else {
                text = "?";
            }

            iconFactory.setStyle(IconGenerator.STYLE_BLUE);
            Bitmap bitmap = iconFactory.makeIcon(text);

            markerOptions
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap));
        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster<NearPlace> cluster) {
            return cluster.getSize() > MAX_NON_CLUSTERED_SIZE;
        }
    }

    private class NearInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

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

                if (selectedClusterItem == null) {
                    return this.mWindow;
                }

                RatePoint place = selectedClusterItem.ratePoint;

                List<RateItem> list = NearFragment.this.updater.getRates(place.id);

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

                        String diff;
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
    private synchronized void reloadMapItems(SparseArray<NearPlace> places) {

        if (!checkReady())
            return;

        // TODO this.map.clear();
        this.mClusterManager.clearItems();

        Set<Integer> added = new HashSet<Integer>();

        // Loop through all the items that are available to be placed on the
        // map
        for (int i = 0; i < places.size(); i++) {
            NearPlace place = places.valueAt(i);

            int id = place.ratePoint.id;

            if (added.contains(id)) {
                continue;
            }

            mClusterManager.addItem(place);
            added.add(id);
        }
    }

    // TODO handle case when same point have more then one marker (with and
    // without rates data)
    private Collection<NearPlace> getNearestPlaces(LatLng toPos, SparseArray<NearPlace> places) {

        if (toPos != null) {

            final double myLat = toPos.latitude;
            final double myLng = toPos.longitude;

            PriorityQueue<NearPlace> q = new PriorityQueue<>(MAX_NON_CLUSTERED_SIZE, new Comparator<NearPlace>() {
                @Override
                public int compare(NearPlace lhs, NearPlace rhs) {

                    float[] result1 = new float[1];
                    Location.distanceBetween(myLat, myLng, lhs.ratePoint.x, lhs.ratePoint.y, result1);

                    float[] result2 = new float[1];
                    Location.distanceBetween(myLat, myLng, rhs.ratePoint.x, rhs.ratePoint.y, result2);

                    return result1[0] < result2[0] ? 1 : -1;
                }
            });

            for (int i = 0; i < places.size(); i++) {
                NearPlace p = places.valueAt(i);

                q.add(p);

                if (q.size() > MAX_NON_CLUSTERED_SIZE) {
                    q.poll();
                }
            }

            return Arrays.asList(q.toArray(new NearPlace[q.size()]));
        } else {
            Log.d(LOG_TAG,
                    "since current position is undefined, nearest markers cant't be found.");
        }

        return Collections.emptyList();
    }

    @Override
    public synchronized void onUpdate() {

        Log.d(LOG_TAG, "onUpdate");

        if (!checkReady())
            return;

        showNearestWindow(this.updater.getMapPosition());
    }

    private void showNearestWindow(LatLng currentPosition) {

        if (currentPosition != null && this.firstTimeOpen) {
            Collection<NearPlace> nearestPlaces = getNearestPlaces(currentPosition,
                    this.places);

            if (!nearestPlaces.isEmpty()) {

                goToBounds(nearestPlaces);

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
            this.places.put(place.id, new NearPlace(place));
        }

        reloadMapItems(this.places);
    }
}
