package ch.prokopovi.ui.main;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.*;
import android.widget.*;
import android.widget.LinearLayout.LayoutParams;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.maps.android.clustering.*;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

import java.util.*;

import ch.prokopovi.R;
import ch.prokopovi.struct.Master.*;
import ch.prokopovi.struct.best.RateItem;
import ch.prokopovi.struct.best.RatePoint;
import ch.prokopovi.ui.main.api.*;

/**
 * near places map fragment
 * <p/>
 * for separate impl. of sherlock map activity see
 * http://stackoverflow.com/questions
 * /13721929/using-actionbarsherlock-with-the-new-supportmapfragment
 *
 * @author Pavel_Letsiaha
 */
public class NearFragment extends SupportMapFragment implements
        UpdateListener,
        RegionListener,
        OpenListener,
        ClusterManager.OnClusterClickListener<NearFragment.NearPlace>,
        ClusterManager.OnClusterInfoWindowClickListener<NearFragment.NearPlace>,
        ClusterManager.OnClusterItemClickListener<NearFragment.NearPlace>,
        ClusterManager.OnClusterItemInfoWindowClickListener<NearFragment.NearPlace>,
        GoogleMap.OnCameraChangeListener {

    private static final int DEFAULT_ZOOM = 15;

    private static final int MAX_NON_CLUSTERED_SIZE = 4;

    private static final String LOG_TAG = "NearFragment";

    private Updater updater;

    private CurrencyOperationType currencyOperationType;

    private Region selectedRegion;

    private final SparseArray<NearPlace> places = new SparseArray<>();
    private final Set<Integer> placesOnMap = new HashSet<>();

    private Map<CurrencyCode, Map<OperationType, Double>> bestRates;

    private boolean firstTimeOpen = true;

    private ClusterManager<NearPlace> mClusterManager;

    private NearPlace selectedClusterItem;

    private LatLng selectedPosition;

    class NearPlace implements ClusterItem {

        private final RatePoint ratePoint;

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

        this.updater.getTracker().trackPageView("/near");

        if (isNotReady())
            return;

        getMap().setMyLocationEnabled(true);
        getMap().getUiSettings().setZoomControlsEnabled(true);

        if (mClusterManager == null) {

            mClusterManager = new ClusterManager<>(getActivity(), getMap());
            NearRenderer nearRenderer = new NearRenderer(getActivity(), getMap(), mClusterManager);
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

        v.setLayoutParams(new LinearLayout.LayoutParams(0,
                LayoutParams.MATCH_PARENT, 1 - TabsActivity.DUAL_PANE_RATIO));

        return v;
    }

    @Override
    public void onResume() {

        Log.d(LOG_TAG, "onResume");

        super.onResume();

        this.firstTimeOpen = true;

        onOpen();
    }

    @Override
    public synchronized void onUpdate() {

        Log.d(LOG_TAG, "onUpdate");

        onOpen();
    }

    @Override
    public void onOpen(LatLng latLng) {

        selectedPosition = latLng;

        onOpen();
    }

    private void onOpen() {
        if (isNotReady())
            return;

        updatePlaces(); // new currency / operation

        LatLng pos = getMapPosition();

        if (pos != null)
            getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(
                    pos, DEFAULT_ZOOM));
    }

    @Override
    public void onAttach(Activity activity) {

        Log.d(LOG_TAG, "onAttach");

        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            this.updater = (Updater) activity;
            this.currencyOperationType = (CurrencyOperationType) activity;

        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement Updater");
        }
    }

    /**
     * Checks if the map is ready (which depends on whether the Google Play
     * services APK is available. This should be called prior to calling any
     * methods on GoogleMap.
     *
     * also checks is fragment added to activity
     */
    private boolean isNotReady() {
        if (getMap() == null || !isAdded()) {
            Log.w(LOG_TAG, "map is not ready");
            return true;
        }
        return false;
    }

    @Override
    public void onRegionChange(Region newRegion) {

        if (newRegion == null) {
            Log.e(LOG_TAG, "new region should not be null");
            return;
        }

        selectedPosition = newRegion.getCoords();
        firstTimeOpen = true; // for showNearest...
        getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(
                newRegion.getCoords(), DEFAULT_ZOOM));

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

        if (isNotReady())
            return;

        Region positionRegion = TabsActivity.findRegion(position.latitude,
                position.longitude);
        if (positionRegion == null) {

            Toast.makeText(
                    getActivity(),
                    R.string.lbl_region_too_far,
                    Toast.LENGTH_LONG).show();

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
    }

    @Override
    public void onCameraChange(CameraPosition position) {

        if (isNotReady())
            return;

        newPosition(position.target);

        // adding only visible
        boolean isChanged = false;
        Projection projection = getMap().getProjection();
        LatLngBounds bounds = projection.getVisibleRegion().latLngBounds;
        for (int i = 0; i < places.size(); i++) {
            NearPlace nearPlace = places.valueAt(i);

            // not visible
            if (!bounds.contains(nearPlace.getPosition()))
                continue;

            // already added
            if (placesOnMap.contains(nearPlace.ratePoint.id))
                continue;

            mClusterManager.addItem(nearPlace);
            placesOnMap.add(nearPlace.ratePoint.id);
            isChanged = true;
        }

        if (isChanged)
            mClusterManager.cluster();

        Log.w(LOG_TAG, "addMarkers :" + placesOnMap.size());
        // --------------

        showNearestWindow(position.target);

        mClusterManager.onCameraChange(position);
    }

    @Override
    public boolean onClusterClick(Cluster<NearPlace> сluster) {
        NearFragment.this.updater.getTracker().trackPageView("/clusterClick");

        if (isNotReady())
            return true;

        goToBounds(сluster.getItems());

        return true;
    }

    private void goToBounds(Collection<NearPlace> items) {
        if (isNotReady())
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

            if (isNotReady())
                return;
            CurrencyOperationType filter = NearFragment.this.currencyOperationType;
            List<RateItem> list = NearFragment.this.updater.getRates(item.ratePoint.id);

            String text = getResources().getString(filter.getCurrencyCode().getTitleRes());

            iconFactory.setStyle(IconGenerator.STYLE_WHITE);
            for (RateItem rate : list) {

                if (rate.currency.equals(filter.getCurrencyCode()) &&
                        rate.operationType.equals(filter.getOperationType())) {


                    text += " " + TabsActivity.FMT_RATE_VALUE.format(rate.value);

                    Double diffVal = getDiffBestOrNull(rate);
                    if (diffVal == null) {
                        iconFactory.setStyle(IconGenerator.STYLE_GREEN);
                    }

                    break;
                }
            }

            setStyleIfSelected(item);

            Bitmap bitmap = iconFactory.makeIcon(text);

            markerOptions
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap));
        }

        private void setStyleIfSelected(NearPlace item) {
            if (item.getPosition().equals(selectedPosition)) {
                iconFactory.setStyle(IconGenerator.STYLE_BLUE);
            } // selected marker
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
        public View getInfoWindow (Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents (Marker marker) {

            if (isNotReady())
                return this.mWindow;

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

                        OperationType operation = item.operationType;

                        Double rateVal = item.value;

                        Double diffVal = getDiffBestOrNull(item);

                        String value = TabsActivity.FMT_RATE_VALUE
                                .format(rateVal);

                        String diff;
                        if (diffVal == null) {
                            // zero diff
                            diff = getResources().getString(
                                    R.string.lbl_best);
                        } else {
                            diff = TabsActivity.FMT_RATE_VALUE
                                    .format(diffVal);
                        }

                        View row = getRow(table, currency.getId());

                        boolean buy = OperationType.BUY.equals(operation);

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

    private Double getDiffBestOrNull(RateItem item) {
        Double bestVal = bestRates.get(item.currency).get(item.operationType);

        Double diffVal = bestVal - item.value;

        return (diffVal > -0.0001 && diffVal < 0.0001) ? null : diffVal;
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
                    "since current position is undefined, nearest markers can't be found.");
        }

        return Collections.emptyList();
    }

    private LatLng getMapPosition() {

        if (this.selectedPosition == null && this.updater.getLocation() != null) {

            this.selectedPosition = new LatLng(this.updater.getLocation().getLatitude(),
                    this.updater.getLocation().getLongitude());
        }

        return this.selectedPosition;
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

        if (this.selectedRegion == null)
            return;

        this.places.clear();

        SparseArray<RatePoint> newPlaces = this.updater
                .getPlaces(this.selectedRegion);
        for (int i = 0; i < newPlaces.size(); i++) {
            RatePoint place = newPlaces.valueAt(i);
            this.places.put(place.id, new NearPlace(place));
        }

        placesOnMap.clear();
        this.mClusterManager.clearItems();
    }
}
