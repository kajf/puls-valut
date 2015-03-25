package ch.prokopovi.ui.main;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MenuItemCompat.OnActionExpandListener;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.OptionsMenuItem;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ch.prokopovi.PrefsUtil;
import ch.prokopovi.R;
import ch.prokopovi.Util;
import ch.prokopovi.api.provider.PlacesProvider;
import ch.prokopovi.api.struct.Titled;
import ch.prokopovi.db.BestRatesTable.ColumnBestRates;
import ch.prokopovi.exported.RatesPlacesTable.ColumnRatesPlaces;
import ch.prokopovi.provider.places.PlacesProviderFactory;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.OperationType;
import ch.prokopovi.struct.Master.Region;
import ch.prokopovi.ui.main.ChoiceDialog.ChoiceCallback;
import ch.prokopovi.ui.main.api.Converter;
import ch.prokopovi.ui.main.api.CurrencyOperationType;
import ch.prokopovi.ui.main.api.OpenListener;
import ch.prokopovi.ui.main.api.RegionListener;
import ch.prokopovi.ui.main.api.UpdateListener;
import ch.prokopovi.ui.main.api.Updater;

@EFragment
@OptionsMenu(R.menu.best_menu)
public class BestRatesFragment extends ListFragment implements
        CurrencyOperationType,
        UpdateListener,
        RegionListener,
        OnScrollListener,
        SwipeRefreshLayout.OnRefreshListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = "BestRatesFragment";

    private static final int PAGE_SIZE = 20;

    private class SortEntry {
        int id;
        Double rate;
        int type;
        Integer distance; // meters, nulls possible
    }

    private Region selectedRegion;
    private CurrencyCode selectedCurrencyCode;
    private OperationType selectedExchangeType;

    private GoogleAnalyticsTracker tracker;

    private Updater updater;
    private OpenListener openListener;
    private Converter converter;
    private Double worstRateValue;

    private int[] positionMap;
    private Integer[] distanceMap;

    private int firstVisiblePosition = 0;
    private int itemsAllowedInList = 0;
    private String searchQuery = null; // holds the current query...

    private SearchView searchView;

    @OptionsMenuItem(R.id.menu_exchange_type)
    MenuItem menuExchangeType;

    @OptionsMenuItem(R.id.menu_currency)
    MenuItem menuCurrency;

    @ViewById(R.id.swipe_container)
    SwipeRefreshLayout swipeLayout;

    @Override
    public CurrencyCode getCurrencyCode() {
        return selectedCurrencyCode;
    }

    @Override
    public OperationType getOperationType() {
        return selectedExchangeType;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        return new AsyncTaskLoader<Cursor>(getActivity()){

            @Override
            public Cursor loadInBackground() {
                BestRatesFragment brf = BestRatesFragment.this;

                Cursor cursor = brf.updater.getData(brf.selectedRegion,
                        brf.selectedExchangeType, brf.selectedCurrencyCode,
                        brf.searchQuery, brf.itemsAllowedInList);

                updateDistanceMaps(cursor);

                Cursor cursorWrapper = new ReorderingCursorWrapper(cursor,
                        brf.positionMap);
                return cursorWrapper;
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        CursorAdapter adapter = (CursorAdapter) getListAdapter();
        adapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> args) {
        CursorAdapter adapter = (CursorAdapter) getListAdapter();
        adapter.changeCursor(null);
    }

    @Override
    public synchronized void onUpdate() {

        // check/update selected currency
        PlacesProvider provider = PlacesProviderFactory
                .find(this.selectedRegion);
        if (!provider.isSupported(this.selectedCurrencyCode)) {
            CurrencyCode[] supportedCurrencies = getSupportedCurrencies(this.selectedRegion);
            BestRatesFragment.this.selectedCurrencyCode = supportedCurrencies[0];
        }

        // re-fetch worst rate value depending on selected region/operation
        // type/currency
        this.worstRateValue = this.updater.getWorstRate(this.selectedRegion,
                this.selectedExchangeType, this.selectedCurrencyCode);

        // filter ui values
        updateUiFilterValues();

        updateListViewData();

        swipeLayout.setRefreshing(false);
    }

    public void updateListViewData() {
        getActivity().getSupportLoaderManager().initLoader(0, null, this).forceLoad();
    }

    /**
     * update distance and ordering-by-distance maps according location and
     * cursor
     *
     * @param cursor
     */
    private void updateDistanceMaps(Cursor cursor) {

        int size = cursor.getCount();
        List<SortEntry> list = new ArrayList<>(size);

        Location myLastLocation = this.updater.getLocation();

        // collect
        while (cursor.moveToNext()) {
            SortEntry entry = new SortEntry();
            entry.id = cursor.getInt(cursor
                    .getColumnIndex(ColumnBestRates.BEST_RATES_ID.name()));
            entry.rate = cursor.getDouble(cursor
                    .getColumnIndex(ColumnBestRates.VALUE.name()));
            entry.type = cursor.getInt(cursor
                    .getColumnIndex(ColumnBestRates.EXCHANGE_TYPE_ID.name()));

            entry.distance = null;
            if (myLastLocation != null) {

                double x = cursor.getDouble(cursor
                        .getColumnIndex(ColumnRatesPlaces.X.name()));
                double y = cursor.getDouble(cursor
                        .getColumnIndex(ColumnRatesPlaces.Y.name()));

                float[] result = new float[1];
                Location.distanceBetween(myLastLocation.getLatitude(),
                        myLastLocation.getLongitude(), x, y, result);

                entry.distance = (int) result[0];
            }

            list.add(entry);
        }

        cursor.moveToPosition(-1); // reset position

        // initial order
        int[] initial = new int[size];
        for (int i = 0; i < size; i++) {
            initial[i] = list.get(i).id;
        }

        // sort
        Collections.sort(list, new Comparator<SortEntry>() {
            @Override
            public int compare(SortEntry lhs, SortEntry rhs) {
                int res = 0;

                // compare rate value
                Double diffRate = lhs.rate - rhs.rate;

                boolean isRateDiffZero = Util.isZero(diffRate);
                boolean isDistanceEmpty = lhs.distance == null
                        || rhs.distance == null;

                if (OperationType.BUY.getId() == lhs.type && !isRateDiffZero) {
                    diffRate *= -1;
                } // change direction

                if (isRateDiffZero) {

                    if (!isDistanceEmpty) {

                        // if rate value is the same - compare distance
                        int diffDist = lhs.distance - rhs.distance;

                        res = diffDist;
                    }

                } else {
                    res = diffRate > 0 ? 1 : -1;
                }

                return res;
            }
        });

        // build map
        this.positionMap = new int[size];
        this.distanceMap = new Integer[size];
        for (int i = 0; i < size; i++) {

            for (int j = 0; j < size; j++) {
                SortEntry entry = list.get(j);

                if (entry.id == initial[i]) {
                    this.positionMap[j] = i;
                    this.distanceMap[j] = entry.distance;
                    break;
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(LOG_TAG, "onCreateView");

        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.best_layout, container, false);

        v.setLayoutParams(new LinearLayout.LayoutParams(0,
                LayoutParams.MATCH_PARENT, TabsActivity.DUAL_PANE_RATIO));

        return v;
    }

    @Override
    public void onAttach(Activity activity) {

        Log.d(LOG_TAG, "onAttach");

        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            this.updater = (Updater) activity;

            this.openListener = (OpenListener) activity;
            this.converter = (Converter) activity;

        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement Updater");
        }
    }

    @Override
    public void onRegionChange(Region newRegion) {

        if (!this.selectedRegion.equals(newRegion)) {

            Log.d(LOG_TAG, "updating region " + newRegion);

            this.itemsAllowedInList = PAGE_SIZE;

            this.converter.close();

            this.selectedRegion = newRegion;

            swipeLayout.setRefreshing(true);
            this.updater.read(this.selectedRegion, false);
        }
    }

    @AfterViews
    void init() {
        Log.d(LOG_TAG, "init");

        this.tracker = this.updater.getTracker();
        this.tracker.trackPageView("/bestRates");

        // restore previously selected values
        restoreSelections();

        if (getListAdapter() == null) {
            Log.d(LOG_TAG, "new list adapter is created");

            setListAdapter(new BestListAdapter(this));
        } // first launch

        getListView().setOnScrollListener(this);

        swipeLayout.setOnRefreshListener(this);

        // workaround for
        // Issue 77712: SwipeRefreshLayout indicator does not shown
        // see https://code.google.com/p/android/issues/detail?id=77712
        TypedValue typed_value = new TypedValue();
        getActivity().getTheme().resolveAttribute(android.support.v7.appcompat.R.attr.actionBarSize, typed_value, true);
        swipeLayout.setProgressViewOffset(false, 0, getResources().getDimensionPixelSize(typed_value.resourceId));
        // ---------
        swipeLayout.setRefreshing(true);

        this.updater.read(this.selectedRegion, false);
    }

    /**
     * collect currencies supported for provider region
     *
     * @param region
     * @return
     */
    private CurrencyCode[] getSupportedCurrencies(Region region) {

        PlacesProvider provider = PlacesProviderFactory.find(region);

        Set<CurrencyCode> supported = new LinkedHashSet<>();
        for (CurrencyCode curr : CurrencyCode.values()) {
            if (provider.isSupported(curr)) {
                supported.add(curr);
            }
        }

        return supported.toArray(new CurrencyCode[supported.size()]);
    }

    @OptionsItem
    void menuExchangeType() {
        this.tracker.trackPageView("/bestExchangeType");

        final BestRatesFragment parent = BestRatesFragment.this;

        parent.converter.close();

        parent.selectedExchangeType = OperationType.BUY
                .equals(parent.selectedExchangeType) ? OperationType.SELL
                : OperationType.BUY;

        Log.d(LOG_TAG, "selected exchange type " + parent.selectedExchangeType);

        parent.updater.read(this.selectedRegion, false);
    }

    @OptionsItem
    void menuCurrency() {
        this.tracker.trackPageView("/bestCurrency");

        final Context context = getActivity();
        final BestRatesFragment parent = BestRatesFragment.this;

        CurrencyCode[] supportedCurrencies = getSupportedCurrencies(this.selectedRegion);

        new ChoiceDialog<CurrencyCode>()
                .init(context, R.string.lbl_choose_currency,
                        supportedCurrencies, //
                        new ChoiceCallback() {
                            @Override
                            public void choice(Titled item) {
                                Log.d(LOG_TAG, "filter by: " + item);

                                CurrencyCode newChoise = (CurrencyCode) item;

                                if (newChoise
                                        .equals(parent.selectedCurrencyCode)) {
                                    return;
                                }// real change

                                parent.converter.close();

                                parent.selectedCurrencyCode = newChoise;

                                Log.d(LOG_TAG, "new currency "
                                        + parent.selectedCurrencyCode);

                                parent.updater.read(parent.selectedRegion,
                                        false);

                            }
                        }).create().show();
    }

    @Override
    public void onRefresh() {
        Log.d(LOG_TAG, "updating...");

        this.tracker.trackPageView("/menuUpdate");

        this.updater.read(this.selectedRegion, true);
    }

    private void updateUiFilterValues() {
        if (menuExchangeType != null) {
            menuExchangeType.setTitle(this.selectedExchangeType.getTitleRes());
            menuCurrency.setTitle(this.selectedCurrencyCode.getTitleRes());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        saveSelections();
    }

    /**
     * restore DDLs selections from prefs if nothing selected
     */
    private void restoreSelections() {

        if (this.selectedRegion == null || this.selectedExchangeType == null
                || this.selectedCurrencyCode == null) {

            SharedPreferences prefs = getActivity().getSharedPreferences(
                    PrefsUtil.PREFS_NAME, Context.MODE_PRIVATE);

            int exTypeId = prefs.getInt(getString(R.string.pref_best_ex_type),
                    OperationType.BUY.getId());
            int currId = prefs.getInt(getString(R.string.pref_best_curr),
                    CurrencyCode.USD.getId());
            int regionId = prefs.getInt(getString(R.string.pref_best_region),
                    Region.MINSK.getId());

            this.selectedExchangeType = OperationType.get(exTypeId);
            this.selectedCurrencyCode = CurrencyCode.get(currId);
            this.selectedRegion = Region.get(regionId);

            Log.d(LOG_TAG, "restoring prev/initial selection: "
                    + this.selectedExchangeType + ", "
                    + this.selectedCurrencyCode + ", " + this.selectedRegion);
        }
    }

    /**
     * save DDLs selection to prefs
     */
    private void saveSelections() {

        Context context = getActivity();

        if (context == null)
            return;

        SharedPreferences.Editor prefs = context.getSharedPreferences(
                PrefsUtil.PREFS_NAME, Context.MODE_PRIVATE).edit();

        prefs.putInt(getString(R.string.pref_best_ex_type),
                this.selectedExchangeType.getId());

        prefs.putInt(getString(R.string.pref_best_curr),
                this.selectedCurrencyCode.getId());

        prefs.putInt(getString(R.string.pref_best_region),
                this.selectedRegion.getId());

        prefs.apply();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem searchItem = menu.findItem(R.id.menu_search);

        this.searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        this.searchView.setOnQueryTextListener(new OnQueryTextListener() {

            @Override
            public boolean onQueryTextChange(String newText) {

                BestRatesFragment parent = BestRatesFragment.this;
                if (TextUtils.isEmpty(newText)) {
                    // Toast.makeText(getActivity(), "List ...",
                    // Toast.LENGTH_SHORT)
                    // .show();
                    parent.searchQuery = null;
                } else {
                    // Toast.makeText(getActivity(),
                    // "Searching for: " + newText + "...", Toast.LENGTH_SHORT)
                    // .show();
                    parent.searchQuery = newText;

                }
                parent.updater.read(parent.selectedRegion, false);

                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                // Toast.makeText(getActivity(), "Searching for: " + query +
                // "...",
                // Toast.LENGTH_SHORT).show();
                BestRatesFragment.this.searchView.clearFocus();
                return true;
            }
        });

        MenuItemCompat.setOnActionExpandListener(searchItem,
                new OnActionExpandListener() {

                    @Override
                    public boolean onMenuItemActionExpand(MenuItem arg0) {
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem arg0) {
                        BestRatesFragment parent = BestRatesFragment.this;
                        parent.searchQuery = null;
                        parent.updater.read(parent.selectedRegion, false);
                        return true;
                    }

                });

        updateUiFilterValues();
    }

    @Override
    public void onScroll(AbsListView view, int firstVisible, int visibleCount,
                         int totalCount) {

        // ---------- add more rows -------------
        int padding = 3;
        boolean loadMore = padding +
                firstVisible + visibleCount >= totalCount;

        if (loadMore && this.itemsAllowedInList == totalCount) {
            Log.d(LOG_TAG, "load more...");
            this.itemsAllowedInList += PAGE_SIZE;
            updateListViewData();
        }

        // ----------- hide converter if down --------------
        final int newFirstVisiblePosition = getListView()
                .getFirstVisiblePosition();

        // scrolling down
        if (newFirstVisiblePosition > this.firstVisiblePosition) {

            this.converter.close();
        }

        this.firstVisiblePosition = newFirstVisiblePosition;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}

    public Integer[] getDistanceMap() {
        return distanceMap;
    }

    public GoogleAnalyticsTracker getTracker() {
        return tracker;
    }

    public OpenListener getOpenListener() {
        return openListener;
    }

    public Converter getConverter() {
        return converter;
    }

    public Double getWorstRateValue() {
        return worstRateValue;
    }
}
