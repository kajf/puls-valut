package ch.prokopovi.ui.main;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.android.gms.maps.model.LatLng;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.NonConfigurationInstance;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.StringRes;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import ch.prokopovi.PrefsUtil;
import ch.prokopovi.R;
import ch.prokopovi.StatsHelper;
import ch.prokopovi.VersionHelper;
import ch.prokopovi.api.struct.Titled;
import ch.prokopovi.db.BestRatesDbAdapter;
import ch.prokopovi.db.BestRatesTable.ColumnBestRates;
import ch.prokopovi.db.DbHelper;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.OperationType;
import ch.prokopovi.struct.Master.Region;
import ch.prokopovi.struct.best.RateItem;
import ch.prokopovi.struct.best.RatePoint;
import ch.prokopovi.ui.AbstractWidgetConfigure;
import ch.prokopovi.ui.main.ConverterFragment.ConverterParams;
import ch.prokopovi.ui.main.RateAppFragment.RateAppListener;
import ch.prokopovi.ui.main.api.Closable;
import ch.prokopovi.ui.main.api.Converter;
import ch.prokopovi.ui.main.api.CurrencyOperationType;
import ch.prokopovi.ui.main.api.OpenListener;
import ch.prokopovi.ui.main.api.RegionListener;
import ch.prokopovi.ui.main.api.UpdateListener;
import ch.prokopovi.ui.main.api.Updater;
import ch.prokopovi.ui.main.resolvers.PaneResolver;
import ch.prokopovi.ui.main.resolvers.PaneResolverFactory;

@EActivity(R.layout.fragment_tabs)
public class TabsActivity extends ActionBarActivity implements
        Updater,
        CurrencyOperationType,
        OpenListener,
        RateAppListener, LocationListener, Converter,
        Closable {

    private static final long EXPIRATION_PERIOD = 10 * DateUtils.HOUR_IN_MILLIS;
	private static final int REGION_NEAR_THRESHOLD = 16 * 1000; // meters
	static final float DUAL_PANE_RATIO = 0.4f;

	private static final long LOCATION_FRESH_PERIOD = DateUtils.MINUTE_IN_MILLIS * 2;
	private static final long LOCATION_UPDATE_PERIOD = DateUtils.SECOND_IN_MILLIS * 5;
	private static final long LOCATION_UPDATE_RANGE = 10; // meters

    private static final Region[] REGIONS = new Region[]{

            Region.MINSK, //

            Region.BREST, Region.GOMEL, Region.GRODNO, Region.MOGILEV,
            Region.VITEBSK,

            Region.BARANOVICHI, //
            Region.BOBRUISK, //
            Region.BORISOV, //
            Region.LIDA, //
            Region.MOZIR, //
            Region.NOVOPOLOCK,//
            Region.ORSHA, //
            Region.PINSK, //
            Region.POLOCK, //
            Region.SOLIGORSK, //

            Region.MOLODZE4NO, //
            Region.SVETLOGORSK, //
            Region.ZLOBIN, //
            Region.RE4ICA, //
            Region.SLUCK, //
            Region.ZODINO, //
    };

    private static final String LOG_TAG = "TabsActivity";

	static final DecimalFormat FMT_RATE_VALUE = new DecimalFormat("#.####");

	private BestRatesDbAdapter dbAdapter;

	private GoogleAnalyticsTracker tracker;

	private Location myLastLocation;

	private boolean regionByLocationIsSet = false;

    @StringRes(R.string.pref_rate_app_launches)
	String prefRateAppLaunches;

	@StringRes(R.string.pref_ads_on)
	String prefAdsOn;

    @StringRes(R.string.btn_region)
    String mTitleRegion;

    @StringRes(R.string.btn_settings)
    String mTitleSettings;

    @StringRes(R.string.btn_share_app)
    String mTitleShareApp;

    @StringRes(R.string.btn_rate_app)
    String mTitleRateApp;

    @StringRes(R.string.lbl_best_rates)
    String mTitleBest;

    @StringRes(R.string.lbl_near_rates)
    String mTitleNear;

    @StringRes(R.string.about_title)
    String mTitleAbout;

    PaneResolver paneResolver;

    @ViewById(R.id.left_drawer)
    ListView mDrawerList;

    @ViewById(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    ActionBarDrawerToggle mDrawerToggle;

    private final List<String> drawerItems = new ArrayList<>();

    /*
     * Using @NonConfigurationInstance on a @Bean will automatically update the
	 * context ref on configuration changes, if the bean is not a singleton
	 */
	@NonConfigurationInstance
	@Bean
	UpdateTask task;

    @Override
    public CurrencyCode getCurrencyCode() {
        Fragment best = getSupportFragmentManager().findFragmentByTag(
                FragmentTag.BEST.tag);

        if (best != null && best instanceof CurrencyOperationType) {
            return ((CurrencyOperationType) best).getCurrencyCode();
        }

        return null;
    }

    @Override
    public OperationType getOperationType() {
        Fragment best = getSupportFragmentManager().findFragmentByTag(
                FragmentTag.BEST.tag);

        if (best != null && best instanceof CurrencyOperationType) {
            return ((CurrencyOperationType) best).getOperationType();
        }

        return null;
    }

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

        TabsActivity ctx = TabsActivity.this;
        boolean newLocationIsBetter = isBetterLocation(location,
                ctx.myLastLocation);

        if (!newLocationIsBetter) {
			Log.d(LOG_TAG, "new location is not better. skip");
			return;
		}

        ctx.myLastLocation = location;

        if (!ctx.regionByLocationIsSet) {

            Region region = findRegion(location.getLatitude(),
                    location.getLongitude());

            if (region != null) {
				Log.d(LOG_TAG, "setting region by location: " + region);

                fireRegionUpdate(region);

                ctx.regionByLocationIsSet = true;

                read(region, false);
			}
		} // set first time region (but do not update after first)

		// update data-location calculations in best rates tab
        BestRatesFragment brf = (BestRatesFragment) getSupportFragmentManager()
                .findFragmentByTag(FragmentTag.BEST.tag);
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

        SharedPreferences prefs = getSharedPreferences(
                PrefsUtil.PREFS_NAME, Context.MODE_PRIVATE);

        boolean adsOn = prefs.getBoolean(this.prefAdsOn, true);
        paneResolver = PaneResolverFactory.createPaneResolver(this, FragmentTag.NEAR, adsOn);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowTitleEnabled(paneResolver.isDisplayShowTitleEnabled());
        actionBar.setDisplayHomeAsUpEnabled(true);

        FragmentTransaction ft = getSupportFragmentManager()
                .beginTransaction();

        UiHelper.addOrAttachFragment(this, ft, FragmentTag.BEST);

        paneResolver.onCreate(ft);

        ft.commit();

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

			// launches count
			int launches = prefs.getInt(this.prefRateAppLaunches, 5);
			Log.d(LOG_TAG, "rate app launches: " + launches);

			if (launches == 0) {

				FragmentTransaction ftRate = getSupportFragmentManager().beginTransaction();
                UiHelper.addOrAttachFragment(this, ftRate, FragmentTag.RATE);
                ftRate.commit();

            } else {

				if (launches > 0) {
					prefs.edit().putInt(this.prefRateAppLaunches, launches - 1)
                            .apply();
                } // update count

                // ads if allowed

                if (adsOn) {

					FragmentTransaction ftBanner = getSupportFragmentManager().beginTransaction();
                    UiHelper.addOrAttachFragment(this, ftBanner, FragmentTag.BANNER);
                    ftBanner.commit();
                } else {
					this.tracker.trackPageView("/adsOff");
				}
			}

			// full screen
			PrefsUtil.initFullscreen(this);
		}
	}

    @AfterViews
    void initDrawer() {

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        Region region = loadSelectedRegion();
        String regionTitle = formatRegionTitle(this, region);

        drawerItems.add(regionTitle);

        paneResolver.addDrawerItems(drawerItems);

        drawerItems.add(mTitleSettings);
        drawerItems.add(mTitleShareApp);
        drawerItems.add(mTitleAbout);

        // rate-app action
        int launches = getSharedPreferences(PrefsUtil.PREFS_NAME,
                Context.MODE_PRIVATE).getInt(this.prefRateAppLaunches, 5);
        if (launches >= 0) {
            drawerItems.add(mTitleRateApp);
        }

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, drawerItems));
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

    /**
     * restore DDLs selections from prefs if nothing selected
     */
    private Region loadSelectedRegion() {

        SharedPreferences prefs = getSharedPreferences(
                PrefsUtil.PREFS_NAME, Context.MODE_PRIVATE);

        int regionId = prefs.getInt(getString(R.string.pref_best_region),
                Region.MINSK.getId());

        Region region = Region.get(regionId);

        updateRegionTitle(this, region);

        Log.d(LOG_TAG, "restoring prev/initial selection: "
                + region);

        return region;

    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, final int position, long id) {

            final TabsActivity ctx = TabsActivity.this;
            String selected = (String) parent.getItemAtPosition(position);

            if (mTitleSettings.equals(selected)) {
                ctx.tracker.trackPageView("/settings");

                PrefsActivity_.intent(ctx).start();

            } else if (mTitleShareApp.equals(selected)) {
                String appUri = "market://details?id=" + getPackageName();
                String appName = getResources().getString(R.string.app_name);

                Intent intentShare = new Intent(Intent.ACTION_SEND);
                intentShare.setType("text/plain");
                intentShare.putExtra(Intent.EXTRA_TEXT, appUri);
                intentShare.putExtra(android.content.Intent.EXTRA_SUBJECT, appName);

                startActivity(Intent.createChooser(intentShare,
                        getResources().getString(R.string.btn_share_app)));
            } else if (mTitleAbout.equals(selected)) {
                ctx.tracker.trackPageView("/info");

                UiHelper.showFragment(ctx, FragmentTag.ABOUT);

            } else if (mTitleRateApp.equals(selected)) {
                ctx.tracker.trackPageView("/menuRateApp");

                rateApp(ctx);

                afterRating(-1);

            } else if (mTitleBest.equals(selected)) {
                Log.d(LOG_TAG, "open best rates list");

                paneResolver.showBest();

            } else if (mTitleNear.equals(selected)) {
                Log.d(LOG_TAG, "open near rates");

                if (myLastLocation != null) {

                    Region myRegion = findRegion(
                            myLastLocation.getLatitude(),
                            myLastLocation.getLongitude());

                    if (myRegion != null) {
                        fireRegionUpdate(myRegion);
                    } else {

                        Toast toast = Toast.makeText(
                                ctx, R.string.lbl_region_too_far, Toast.LENGTH_LONG);

                        toast.show();
                    }
                }

                UiHelper.showFragment(ctx, FragmentTag.NEAR);
            } else if (selected.contains(mTitleRegion)) {

                getTracker().trackPageView("/bestRegion");

                ListAdapter adapter = AbstractWidgetConfigure.buildAdapter(ctx,
                        REGIONS);

                AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                builder.setTitle(R.string.lbl_choose_region)
                        .setAdapter(adapter, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(LOG_TAG, "filter by: " + which);

                                Region region = REGIONS[which];

                                fireRegionUpdate(region);
                            }
                        }).show();
            }

            // Highlight the selected item, update the title, and close the drawer
            mDrawerList.setItemChecked(position, true);
            mDrawerLayout.closeDrawer(mDrawerList);

        }
    }

    private void fireRegionUpdate(Region region) {

        Collection<RegionListener> regListeners =
                getListeners(FragmentTag.BEST, FragmentTag.NEAR);
        for (RegionListener regListener : regListeners) {
            regListener.onRegionChange(region);
        }

        updateRegionTitle(this, region);
    }

    private <T> Collection<T> getListeners(FragmentTag... ftags) {

        Collection<T> res = new ArrayList<>();

        for (FragmentTag ftag : ftags) {
            Fragment f = getSupportFragmentManager()
                    .findFragmentByTag(ftag.tag);

            if (f == null)
                continue;

            res.add((T) f);
        }

        return res;
    }

    private String formatRegionTitle(Context context, Titled region) {
        String regionTitle =
                context.getResources().getString(region.getTitleRes());
        String title = mTitleRegion + ": " + regionTitle;

        return title;
    }

    private void updateRegionTitle(Context context, Titled region) {

        String text = formatRegionTitle(context, region);

        for (int i = 0; i < drawerItems.size(); i++) {
            String drawerItem = drawerItems.get(i);

            if (drawerItem.contains(mTitleRegion)) {
                drawerItems.set(i, text);
                ((ArrayAdapter) mDrawerList.getAdapter()).notifyDataSetChanged();
            }
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

        final List<String> enabledProviders = getLocationManager().getProviders(true);
        enabledProviders.remove(LocationManager.PASSIVE_PROVIDER); // not enough

        boolean locationIsOn = !enabledProviders.isEmpty();

        if (!locationIsOn) {

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

		return locationIsOn;
	}

	private LocationManager getLocationManager() {

		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		return locationManager;
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

		this.tracker.dispatch();
		this.tracker.stopSession();

		Log.d(LOG_TAG, "closed");
	}

    void fireDataUpdate() {
        try {

            Collection<UpdateListener> upListeners =
                    getListeners(FragmentTag.BEST, FragmentTag.NEAR);
            for (UpdateListener upListener : upListeners) {
                upListener.onUpdate();
            }

        } catch (Exception e) {
			Log.e(LOG_TAG, "error on fragments update", e);
		}
	}

	@Override
	public void onOpen(LatLng latLng) {
        paneResolver.onOpen(latLng);
    }

    @Override
	public void open(ConverterParams params) {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

		ft.setCustomAnimations(R.anim.abc_slide_in_top, 0);

        ConverterFragment_ converterFragment = UiHelper.addOrAttachFragment(
                this, ft, FragmentTag.CONVERTER);

        converterFragment.setParams(params);

        ft.commit();

		// update values if converter already open
		converterFragment.reload();
	}

	@Override
	public void close() {
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

				this.task.go(this.dbAdapter, region);
			} // already running
		} else {
			// skip update
            fireDataUpdate();
        }
	}

	@Override
	public GoogleAnalyticsTracker getTracker() {
		return this.tracker;
	}

	@Override
	public Location getLocation() {
		return this.myLastLocation;
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

        if (paneResolver.isBestActive()){
            super.onBackPressed();
        } else {
            paneResolver.showBest();
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

}
