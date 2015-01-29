package ch.prokopovi.ui.main;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.adsdk.sdk.Ad;
import com.adsdk.sdk.AdListener;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.model.LatLng;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.NonConfigurationInstance;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.StringRes;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.prokopovi.PrefsUtil;
import ch.prokopovi.R;
import ch.prokopovi.StatsHelper;
import ch.prokopovi.VersionHelper;
import ch.prokopovi.db.BestRatesDbAdapter;
import ch.prokopovi.db.BestRatesTable.ColumnBestRates;
import ch.prokopovi.db.DbHelper;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.OperationType;
import ch.prokopovi.struct.Master.Region;
import ch.prokopovi.struct.best.RateItem;
import ch.prokopovi.struct.best.RatePoint;
import ch.prokopovi.ui.main.ConverterFragment.ConverterParams;
import ch.prokopovi.ui.main.RateAppFragment.RateAppListener;
import ch.prokopovi.ui.main.api.Closable;
import ch.prokopovi.ui.main.api.Converter;
import ch.prokopovi.ui.main.api.OpenListener;
import ch.prokopovi.ui.main.api.UpdateListener;
import ch.prokopovi.ui.main.api.Updater;

@EActivity(R.layout.fragment_tabs)
@OptionsMenu(R.menu.main_menu)
public class TabsActivity extends ActionBarActivity implements Updater,
		OpenListener, RateAppListener, AdListener, LocationListener, Converter,
		Closable {

	private static final long EXPIRATION_PERIOD = 10 * DateUtils.HOUR_IN_MILLIS;
	private static final int REGION_NEAR_THRESHOLD = 16 * 1000; // meters
	static final float DUAL_PANE_RATIO = 0.4f;

	private static final long LOCATION_FRESH_PERIOD = DateUtils.MINUTE_IN_MILLIS * 2;
	private static final long LOCATION_UPDATE_PERIOD = DateUtils.SECOND_IN_MILLIS * 5;
	private static final long LOCATION_UPDATE_RANGE = 10; // meters

	private static final String LOG_TAG = "TabsActivity";

	static final DecimalFormat FMT_RATE_VALUE = new DecimalFormat("#.####");

	enum FragmentTag {
		CONVERTER(ConverterFragment_.class.getName(), R.id.top_container), //
		BEST(BestRatesFragment_.class.getName(), R.id.main_container), //
		NEAR(NearFragment.class.getName(), R.id.main_container), //
		RATE(RateAppFragment_.class.getName(), R.id.bottom_container), //
		BANNER(BannerFragment_.class.getName(), R.id.bottom_container);

		String tag;
		String className;
		int container;

		private FragmentTag(String clazz, int container) {
			this.tag = name();
			this.className = clazz;
			this.container = container;
		}
	}

	private ProgressDialog progressDialog;

	private BestRatesDbAdapter dbAdapter;

	private GoogleAnalyticsTracker tracker;

	private final Set<UpdateListener> updateListeners = new HashSet<UpdateListener>();

	private boolean dualPane = false;

	private Location myLastLocation;

	private LatLng mapPosition;

	private boolean regionByLocationIsSet = false;

	@StringRes(R.string.pref_rate_app_launches)
	String prefRateAppLaunches;

	@StringRes(R.string.pref_ads_on)
	String prefAdsOn;

    @StringRes(R.string.btn_settings)
    String strResSettins;

    @StringRes(R.string.btn_share_app)
    String strResShareApp;

    @StringRes(R.string.btn_info)
    String strResInfo;

    @StringRes(R.string.btn_rate_app)
    String strResRateApp;

    @ViewById(R.id.left_drawer)
    ListView mDrawerList;

    @ViewById(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    ActionBarDrawerToggle mDrawerToggle;

    /*
     * Using @NonConfigurationInstance on a @Bean will automatically update the
	 * context ref on configuration changes, if the bean is not a singleton
	 */
	@NonConfigurationInstance
	@Bean
	UpdateTask task;

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.d(LOG_TAG, "--- location update: " + location);

		boolean newLocationIsBetter = isBetterLocation(location,
				TabsActivity.this.myLastLocation);

		if (!newLocationIsBetter) {
			Log.d(LOG_TAG, "new location is not better. skip");
			return;
		}

		TabsActivity.this.myLastLocation = location;

		Region region = findRegion(location.getLatitude(),
				location.getLongitude());

		BestRatesFragment brf = (BestRatesFragment) getSupportFragmentManager()
				.findFragmentByTag(FragmentTag.BEST.tag);

		if (!TabsActivity.this.regionByLocationIsSet) {

			if (region != null) {
				Log.d(LOG_TAG, "setting region by location: " + region);

				// set best tab's region

				brf.updateSelectedRegion(region);

				TabsActivity.this.regionByLocationIsSet = true;

				read(region, false);
			}
		} // set first time region (but do not update after first)

		// update data-location calculations in best rates tab
		if (brf != null && brf.isVisible()) {
			brf.updateListViewData();
		}
	}

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	protected boolean isBetterLocation(Location location,
			Location currentBestLocation) {

		if (location == null) {
			// A null new location is always worser any other
			return false;
		}

		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > LOCATION_FRESH_PERIOD;
		boolean isSignificantlyOlder = timeDelta < -LOCATION_FRESH_PERIOD;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	/**
	 * create intent to launch main activity like from app launcher
	 * 
	 * @param c
	 * @return
	 */
	public static Intent getLauncherIntent(Context c) {

		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.setComponent(new ComponentName(c, TabsActivity_.class));
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		return intent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		// skin
		PrefsUtil.initSkin(this);

		super.onCreate(savedInstanceState);

		this.dualPane = isDualPane();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowTitleEnabled(this.dualPane);
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (this.dualPane) {
            FragmentTransaction ft = getSupportFragmentManager()
					.beginTransaction();

			addOrAttachFragment(ft, this, FragmentTag.BEST);
			addOrAttachFragment(ft, this, FragmentTag.NEAR);

			ft.commit();

		} else {
			shiftFragments();
		}

		// DB ---
		DbHelper dbHelper = DbHelper.getInstance(this);
		SQLiteDatabase database = dbHelper.getDb();
		this.dbAdapter = new BestRatesDbAdapter(database);

		// tracker
		this.tracker = GoogleAnalyticsTracker.getInstance();
		this.tracker.setAnonymizeIp(true);

		this.tracker.startNewSession(StatsHelper.PROPERTY_ID, this);

		// rate me
		if (savedInstanceState == null) { // only freshly created activity

			SharedPreferences prefs = getSharedPreferences(
					PrefsUtil.PREFS_NAME, Context.MODE_PRIVATE);

			// launches count
			int launches = prefs.getInt(this.prefRateAppLaunches, 5);
			Log.d(LOG_TAG, "rate app launches: " + launches);

			if (launches == 0) {

				FragmentManager fm = getSupportFragmentManager();

				FragmentTransaction ft = fm.beginTransaction();
				addOrAttachFragment(ft, this, FragmentTag.RATE);

				ft.commit();
			} else {

				if (launches > 0) {
					prefs.edit().putInt(this.prefRateAppLaunches, launches - 1)
                            .apply();
                } // update count

                // ads if allowed
				if (prefs.getBoolean(this.prefAdsOn, true)) {
					FragmentManager fm = getSupportFragmentManager();

					FragmentTransaction ft = fm.beginTransaction();
					addOrAttachFragment(ft, this, FragmentTag.BANNER);

					ft.commit();
				} else {
					this.tracker.trackPageView("/adsOff");
				}
			}

			// full screen
			PrefsUtil.initFullscreen(this);
		}

		// show progress dialog on orientation change
		if (this.task.isInProgress()) {
			getProgressDialog().show();
		}
	}

    @AfterViews
    void initDrawer() {

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        Resources rs = getResources();

        List<String> pages = new ArrayList<>();
        pages.add(strResSettins);
        pages.add(strResShareApp);
        pages.add(strResInfo);

        // rate-app action
        int launches = getSharedPreferences(PrefsUtil.PREFS_NAME,
                Context.MODE_PRIVATE).getInt(this.prefRateAppLaunches, 5);
        if (launches >= 0) {
            pages.add(strResRateApp);
        }

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, pages));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                //getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                //getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerToggle.syncState();
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {

            TabsActivity ctx = TabsActivity.this;
            String selected = (String) parent.getItemAtPosition(position);

            if (strResSettins.equals(selected)) {
                ctx.tracker.trackPageView("/settings");

                PrefsActivity_.intent(ctx).start();
            } else if (strResShareApp.equals(selected)) {
                String appUri = "market://details?id=" + getPackageName();
                String appName = getResources().getString(R.string.app_name);

                Intent intentShare = new Intent(Intent.ACTION_SEND);
                intentShare.setType("text/plain");
                intentShare.putExtra(Intent.EXTRA_TEXT, appUri);
                intentShare.putExtra(android.content.Intent.EXTRA_SUBJECT, appName);

                startActivity(Intent.createChooser(intentShare,
                        getResources().getString(R.string.btn_share_app)));
            } else if (strResInfo.equals(selected)) {
                ctx.tracker.trackPageView("/info");

                HelpActivity_.intent(ctx).start();
            } else if (strResRateApp.equals(selected)) {
                ctx.tracker.trackPageView("/menuRateApp");

                rateApp(ctx);

                afterRating(-1);
            }

            // Highlight the selected item, update the title, and close the drawer
            mDrawerList.setItemChecked(position, true);
            mDrawerLayout.closeDrawer(mDrawerList);

        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }


    /**
     * ask user to switch location service on if needed
     */
    private boolean askLocationIsOn() {

		final Context context = this;

		LocationManager locationManager = getLocationManager();
		String providerName = getProviderName();

        boolean providerEnabled = (providerName != null) && locationManager
                .isProviderEnabled(providerName);

        Log.d(LOG_TAG, "best location provider enabled: " + providerEnabled);
        if (!providerEnabled) {

			final SharedPreferences prefs = getSharedPreferences(
					PrefsUtil.PREFS_NAME, Context.MODE_PRIVATE);

			boolean isAskLocationAllowed = prefs.getBoolean(
					getString(R.string.pref_ask_location), true);

			Log.d(LOG_TAG, "ask location allowed: " + isAskLocationAllowed);
			if (isAskLocationAllowed) {

				new AlertDialog.Builder(context)
						.setTitle(R.string.no_location_svc_title)
						.setMessage(R.string.no_location_svc_msg)
						.setPositiveButton(R.string.yes,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int i) {

										context.startActivity(new Intent(
												android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));

										dialog.dismiss();
									}
								})
						.setNeutralButton(R.string.no,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {

										dialog.dismiss();
									}
								})
						.setNegativeButton(R.string.btn_do_not_ask,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {

										// do not ask any more
										prefs.edit()
												.putBoolean(
														getString(R.string.pref_ask_location),
														false).commit();

										dialog.dismiss();
									}
								}).create().show();
			}
		}

		return providerEnabled;
	}

	private LocationManager getLocationManager() {

		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		return locationManager;
	}

	private String getProviderName() {

		Criteria locationCritera = new Criteria();
		locationCritera.setAccuracy(Criteria.ACCURACY_COARSE);
		locationCritera.setAltitudeRequired(false);
		locationCritera.setBearingRequired(false);
		locationCritera.setCostAllowed(true);
		locationCritera.setPowerRequirement(Criteria.NO_REQUIREMENT);

		String providerName = getLocationManager().getBestProvider(
				locationCritera, true);

		Log.d(LOG_TAG, "best location provider: " + providerName);

		return providerName;
	}

	/**
	 * search best last known location between all providers
	 * 
	 * @return
	 */
	private Location getLastKnownLocation() {
		LocationManager locationManager = getLocationManager();

		Location bestLocation = null;

		List<String> providers = locationManager.getAllProviders();
		for (String provider : providers) {
			Location loc = locationManager.getLastKnownLocation(provider);
			Log.d(LOG_TAG, "last known location, provider: " + provider
					+ ", location: " + loc);

			boolean betterLocation = isBetterLocation(loc, bestLocation);
			if (betterLocation) {
				bestLocation = loc;
			}
		}

		Log.d(LOG_TAG, "best last known location: " + bestLocation);

		return bestLocation;
	}

	/**
	 * get whether layout is dual-paned or single-paned
	 * 
	 * @return
	 */
	private boolean isDualPane() {

		int statusCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		boolean mapsAvailable = (statusCode == ConnectionResult.SUCCESS);
		Log.d(LOG_TAG, "maps available: " + mapsAvailable);

		Configuration cfg = getResources().getConfiguration();

		boolean isLandscape = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE;
		Log.d(LOG_TAG, "is landscape: " + isLandscape);

		int masked = cfg.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
		boolean isLarge = masked == Configuration.SCREENLAYOUT_SIZE_LARGE
				|| VersionHelper.isXlarge(cfg);
		Log.d(LOG_TAG, "is large: " + isLarge);

		boolean res = mapsAvailable && isLandscape && isLarge;

		Log.d(LOG_TAG, "dual pane: " + res);

		return res;
	}

	/**
	 * find nearest region by location
	 * 
	 * @param inLat
	 *            latitude
     * @param inLng
     *            longitude
     *
     * @return nearest region or null, if no regions in some radius
	 */
	static Region findRegion(double inLat, double inLng) {
		Region myRegion = null;

		float[] result = new float[1];

		float smallest = REGION_NEAR_THRESHOLD; // meters
		for (Region region : Region.values()) {

			double lat = region.getCoords().latitude;
			double lng = region.getCoords().longitude;

			Location.distanceBetween(inLat, inLng, lat, lng, result);

			// Log.d(LOG_TAG, "result: " + result[0]);
			if (smallest > result[0]) {
				smallest = result[0];
				myRegion = region;
				Log.d(LOG_TAG, "near region: " + region + ", dist: " + smallest);
			}
		}

		return myRegion;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	private void initLocation() {
		boolean locationIsOn = askLocationIsOn();

		if (locationIsOn) {

			if (this.myLastLocation == null) {
				Location lastKnownLocation = getLastKnownLocation();
				onLocationChanged(lastKnownLocation); // initial fast update
			}

			LocationManager lm = getLocationManager();
			List<String> providers = lm.getProviders(true);
			for (String provider : providers) {
				lm.requestLocationUpdates(provider, LOCATION_UPDATE_PERIOD,
						LOCATION_UPDATE_RANGE, this);
				Log.d(LOG_TAG, provider + " provider listener registered");
			}
		}
	}

	@Override
	protected void onResume() {

		Log.d(LOG_TAG, "onResume");

		super.onResume();

		initLocation(); // location on/off

		this.dualPane = isDualPane(); // maps installed
	}

	@Override
	protected void onPause() {
		Log.d(LOG_TAG, "onPause");

		super.onPause();

		getLocationManager().removeUpdates(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (getProgressDialog().isShowing()) {
			getProgressDialog().dismiss();
		}

		this.tracker.dispatch();
		this.tracker.stopSession();

		Log.d(LOG_TAG, "closed");
	}

	void fireUpdate() {
		try {

			Log.d(LOG_TAG, "fire update for " + this.updateListeners.size()
					+ " listeners");

			for (UpdateListener listener : this.updateListeners) {

				listener.onUpdate();
			}

		} catch (Exception e) {
			Log.e(LOG_TAG, "error on fragments update", e);
		}
	}

	/**
	 * lazy progress dialog creating
     *
     * @return
     */
    ProgressDialog getProgressDialog() {
		if (this.progressDialog == null) {
			this.progressDialog = new ProgressDialog(this);
			this.progressDialog.setMessage("Обновляем ...");
			this.progressDialog.setCancelable(false);
		}

		return this.progressDialog;
	}

	@Override
	public void onOpen(LatLng latLng) {

		this.mapPosition = latLng;

		shiftFragments();
	}

	@Override
	public void open(ConverterParams params) {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

		ft.setCustomAnimations(R.anim.abc_slide_in_top, 0);

		ConverterFragment_ converterFragment = (ConverterFragment_) addOrAttachFragment(
				ft, this, FragmentTag.CONVERTER);

		converterFragment.setParams(params);

		ft.commit();

		// update values if converter already open
		converterFragment.reload();
	}

	@Override
	public void сlose() {
		// close keyboard
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

		View currentFocus = getCurrentFocus();
		if (currentFocus != null && currentFocus.getId() == R.id.et_i_have) {
			imm.hideSoftInputFromWindow(currentFocus.getWindowToken(),
					InputMethodManager.HIDE_NOT_ALWAYS);
		}
		// ---

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

		detachFragment(ft, FragmentTag.CONVERTER.tag);

		ft.commit();
	}

	/**
	 * check if rates fore region are expired (based on maxAge)
	 * 
	 * @param region
	 * @param maxAge
	 * 
	 * @return true - if update needed, false - otherwise
	 */
	private boolean isExpired(Region region, Long maxAge) {

		Long now = new Date().getTime();
		Long lastUpdateTime = this.dbAdapter.fetchLastUpdateTime(region);

		Long age = now - lastUpdateTime;

		return (age > maxAge);
	}

	@Override
	public void read(Region region, boolean now) {

		boolean expired = isExpired(region, EXPIRATION_PERIOD);
		if (now || expired) {

			if (!this.task.isInProgress()) {
				getProgressDialog().show();
				this.task.go(this.dbAdapter, region);
			} // already running
		} else {
			// skip update
			fireUpdate();
		}
	}

	@Override
	public GoogleAnalyticsTracker getTracker() {
		return this.tracker;
	}

	@Override
	public void addUpdateListener(UpdateListener updateListener) {
		this.updateListeners.add(updateListener);
	}

	@Override
	public void removeUpdateListener(UpdateListener updateListener) {
		this.updateListeners.remove(updateListener);
	}

	@Override
	public Location getLocation() {
		return this.myLastLocation;
	}

	@Override
	public LatLng getMapPosition() {

		if (this.mapPosition == null && this.myLastLocation != null) {

			this.mapPosition = new LatLng(this.myLastLocation.getLatitude(),
					this.myLastLocation.getLongitude());
		}

		return this.mapPosition;
	}

	@Override
	public Double getWorstRate(Region region, OperationType operation,
			CurrencyCode currency) {
		return this.dbAdapter.fetchWorstRate(region, currency, operation);
	}

	@Override
	public Cursor getData(Region region, OperationType operationType,
			CurrencyCode currencyCode, String searchQuery, int limit) {
		return this.dbAdapter.fetch(region, operationType, currencyCode,
                searchQuery, limit);
    }

    @Override
	public SparseArray<RatePoint> getPlaces(Region region) {
		return this.dbAdapter.fetchPoints(region);
	}

	@Override
	public Map<CurrencyCode, Map<OperationType, Double>> getBestRates(
			Region region) {
		return this.dbAdapter.fetchBestRates(region);
	}

	@Override
	public List<RateItem> getRates(int pointId) {
		return this.dbAdapter.fetchRates(ColumnBestRates.RATES_PLACE_ID,
                pointId);
    }

    @Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		// switch actions
		MenuItem bestItem = menu.findItem(R.id.menu_best);
		if (bestItem != null)
			bestItem.setVisible(!this.dualPane);

		MenuItem nearItem = menu.findItem(R.id.menu_near);
		if (nearItem != null)
			nearItem.setVisible(!this.dualPane);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void shiftFragments() {

		if (this.dualPane)
			return; // nothing to shift if dual-pane-layout

		FragmentManager fm = getSupportFragmentManager();

		Fragment best = fm.findFragmentByTag(FragmentTag.BEST.tag);

		boolean toBest = best == null || !best.isVisible();

        //getSupportActionBar().setDisplayHomeAsUpEnabled(!toBest);

        //
        FragmentTransaction ft = fm.beginTransaction();

		ft.setCustomAnimations(R.anim.abc_slide_in_top, 0);

		if (toBest) {
			detachFragment(ft, FragmentTag.NEAR.tag);
			addOrAttachFragment(ft, this, FragmentTag.BEST);
		} else {
			detachFragment(ft, FragmentTag.CONVERTER.tag);
			detachFragment(ft, FragmentTag.BEST.tag);
			addOrAttachFragment(ft, this, FragmentTag.NEAR);
		}

		ft.commit();

		fm.executePendingTransactions();
	}

	/**
	 * add-new or attach-existing fragment
	 * 
	 * @param ft
	 *            transaction
	 * @param context
	 * @param info
	 *            fragment info
	 * 
	 * @return resulting fragment
	 */
	private Fragment addOrAttachFragment(FragmentTransaction ft,
			Context context, FragmentTag info) {
		FragmentManager fm = getSupportFragmentManager();
		Fragment fragment = fm.findFragmentByTag(info.tag);

		if (fragment == null) {

			fragment = Fragment.instantiate(context, info.className);

			ft.add(info.container, fragment, info.tag);
		} else {
			if (fragment.isDetached()) {
				ft.attach(fragment);
			}
		}

		return fragment;
	}

	/**
	 * fragment detach-if-exists
	 * 
	 * @param ft
	 * @param tag
	 */
	private void detachFragment(FragmentTransaction ft, String tag) {
		FragmentManager fm = getSupportFragmentManager();
		Fragment fragment = fm.findFragmentByTag(tag);

		if (fragment != null) {
			ft.detach(fragment);
		}
	}

	/**
	 * fragment remove-if-exists in separate transaction
	 * 
	 * @param fTag
	 */
	private void removeFragment(FragmentTag fTag) {
		FragmentManager fm = getSupportFragmentManager();
		Fragment rateFragment = fm.findFragmentByTag(fTag.tag);
		if (rateFragment != null) {
			// commitAllowingStateLoss() is used to prevent
			// IllegalStateException if app is closed before noAdFound() event.
			// see
			// http://www.androiddesignpatterns.com/2013/08/fragment-transaction-commit-state-loss.html
			fm.beginTransaction().remove(rateFragment)
					.commitAllowingStateLoss();
		}
	}

	@Override
	public void onBackPressed() {

        if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
            mDrawerLayout.closeDrawer(mDrawerList);
            return;
        }

        Fragment best = getSupportFragmentManager().findFragmentByTag(
                FragmentTag.BEST.tag);
		if (best == null || !best.isVisible()) { // go main list
			shiftFragments();
		} else { // finish
			super.onBackPressed();
        }
    }

    @OptionsItem(android.R.id.home)
    void menuHome() {
        if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
            mDrawerLayout.closeDrawer(mDrawerList);
        } else {
            mDrawerLayout.openDrawer(mDrawerList);
        }
    }

    private static void rateApp(Context context) {

		String appUri = "market://details?id=" + context.getPackageName();

		Log.d(LOG_TAG, "rate app: " + appUri);

		Intent intentRateApp = new Intent(Intent.ACTION_VIEW, Uri.parse(appUri));
		context.startActivity(intentRateApp);
	}

	/**
	 * actions after rating choice is made
	 * 
	 * @param launches
	 *            new rate-app-launches property value
	 */
	private void afterRating(int launches) {

		if (launches < 0) {
			VersionHelper.invalidateOptionsMenu(this); // update action bar
			// items
		}

		// set property
		getSharedPreferences(PrefsUtil.PREFS_NAME, Context.MODE_PRIVATE).edit()
				.putInt(this.prefRateAppLaunches, launches).commit();

		// swith off dialog if exists
		removeFragment(FragmentTag.RATE);
	}

	@Override
	public void onRate(int buttonId) {

		int launches = 0;

		switch (buttonId) {
		case R.id.b_rate:
			this.tracker.trackPageView("/rated");

			rateApp(this);

			launches = -1;// no more asking

			break;
		case R.id.b_not_now:
			this.tracker.trackPageView("/rateNotNow");

			Log.d(LOG_TAG, "rate app action is postponed");

			launches = 3; // wait for 3 more launches
			break;
		case R.id.b_do_not_ask:
			this.tracker.trackPageView("/rateNever");

			Log.d(LOG_TAG, "rate app action is declined");

			launches = -1; // no more asking

			break;
		}

		afterRating(launches);
	}

	@Override
	public void adClicked() {
	}

	@Override
	public void adClosed(Ad arg0, boolean arg1) {
	}

	@Override
	public void adLoadSucceeded(Ad arg0) {
		Log.d(LOG_TAG, "ad load");
		this.tracker.trackPageView("/adLoaded");
	}

	@Override
	public void adShown(Ad arg0, boolean arg1) {
		Log.d(LOG_TAG, "ad shown");
	}

	@Override
	public void noAdFound() {
		Log.d(LOG_TAG, "no ad found");
		this.tracker.trackPageView("/noAdFound");

		removeFragment(FragmentTag.BANNER);
	}

}
