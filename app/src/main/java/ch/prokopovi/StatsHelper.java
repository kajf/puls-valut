package ch.prokopovi;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseArray;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.ProviderCode;
import ch.prokopovi.struct.Master.RateType;
import ch.prokopovi.struct.Master.WidgetSize;
import ch.prokopovi.struct.WidgetPreferences;
import ch.prokopovi.ui.WidgetBroker;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.firebase.analytics.FirebaseAnalytics;

public class StatsHelper {

	private static final String LOG_TAG = "StatsHelper";

	interface Collector {
		void collect(FirebaseAnalytics tracker);
	}

	private static synchronized void periodicCollect(ContextWrapper ctx,
			String key, long expirationPeriod, Collector collector) {

		SharedPreferences prefs = ctx.getSharedPreferences(
				PrefsUtil.PREFS_NAME, Context.MODE_PRIVATE);

		long lastCollectTime = prefs.getLong(key, 0L);

		long now = new Date().getTime();

		boolean collectExpired = (now - lastCollectTime) > expirationPeriod;

		if (collectExpired) {
			Log.d(LOG_TAG, "collecting widget stats");

			FirebaseAnalytics tracker = FirebaseAnalytics.getInstance(ctx);

			collector.collect(tracker);

			prefs.edit().putLong(key, now).commit();
		}
	}

	public static synchronized void dailyCollect(final ContextWrapper ctx) {

		periodicCollect(ctx,
				ctx.getString(R.string.pref_last_app_stats_collect),
				DateUtils.DAY_IN_MILLIS, new Collector() {

					@Override
					public void collect(FirebaseAnalytics tracker) {

						// just stats
						tracker.logEvent("online", null);

						// maps availability
						int statusCode = GooglePlayServicesUtil
								.isGooglePlayServicesAvailable(ctx);
						boolean mapsAvailable = (statusCode == ConnectionResult.SUCCESS);
						if (!mapsAvailable) {
							tracker.logEvent("no_maps", null);
						}

						// large screen
						Configuration cfg = ctx.getResources()
								.getConfiguration();
						int masked = cfg.screenLayout
								& Configuration.SCREENLAYOUT_SIZE_MASK;
						boolean isLarge = masked == Configuration.SCREENLAYOUT_SIZE_LARGE
								|| VersionHelper.isXlarge(cfg);
						if (isLarge) {
							tracker.logEvent("large_screen", null);
						}

						// theme
						String prefAppSkinKey = ctx
								.getString(R.string.pref_app_skin);
						String skin = ctx.getSharedPreferences(
								PrefsUtil.PREFS_NAME, Context.MODE_PRIVATE)
								.getString(prefAppSkinKey, null);
						if (ctx.getString(R.string.pref_app_skin_value_dark)
								.equals(skin)) {
							tracker.logEvent("theme_dark", null);
						} else {
							tracker.logEvent("theme_light", null);
						}

						Set<WidgetSize> widgetSizes = new HashSet<WidgetSize>();
						Set<ProviderCode> providerCodes = new HashSet<ProviderCode>();
						Set<CurrencyCode> currencyCodes = new HashSet<CurrencyCode>();
						Set<RateType> rateTypes = new HashSet<RateType>();

						AppWidgetManager widgetManager = AppWidgetManager
								.getInstance(ctx);

						SparseArray<WidgetPreferences> widgetPrefsMap = WidgetBroker
								.getWidgetPrefsMap(ctx);
						int widgetCount = widgetPrefsMap.size();
						for (int i = 0; i < widgetCount; i++) {
							int widgetId = widgetPrefsMap.keyAt(i);

							// widget size
							WidgetSize widgetSize = WidgetBroker.getWidgetSize(
									widgetManager, widgetId);
							widgetSizes.add(widgetSize);

							WidgetPreferences widgetPrefs = widgetPrefsMap
									.get(widgetId);

							// provider
							ProviderCode providerCode = widgetPrefs
									.getProviderCode();
							providerCodes.add(providerCode);

							// currency
							Set<CurrencyCode> tmpCc = widgetPrefs
									.getCurrencyCodes();
							currencyCodes.addAll(tmpCc);

							// rate_type
							RateType rateType = widgetPrefs.getRateType();
							rateTypes.add(rateType);
						}

						// get stats info
						Log.d(LOG_TAG, "widgetSizes: " + widgetSizes);
						for (WidgetSize widgetSize : widgetSizes) {
							tracker.logEvent(formatAsEvent(widgetSize), null);
						}

						Log.d(LOG_TAG, "providerCodes: " + providerCodes);
						for (ProviderCode providerCode : providerCodes) {
							tracker.logEvent(formatAsEvent(providerCode), null);
						}

						Log.d(LOG_TAG, "rateTypes: " + rateTypes);
						for (RateType rateType : rateTypes) {
							tracker.logEvent(formatAsEvent(rateType), null);
						}

						Log.d(LOG_TAG, "currencyCodes: " + currencyCodes);
						for (CurrencyCode currencyCode : currencyCodes) {
							tracker.logEvent(formatAsEvent(currencyCode), null);
						}

						Log.d(LOG_TAG, "widgetCount: " + widgetCount);
						switch (widgetCount) {
						case 0:
							// tracker.trackPageView("widgets_0");
							break;
						case 1:
							tracker.logEvent("widgets_1",null);
							break;
						case 2:
							tracker.logEvent("widgets_2",null);
							break;
						default:
							tracker.logEvent("widgets_more",null);
							break;
						}
					}
				});
	}

	/**
	 * transform enum name to url-like form
	 * 
	 * @param in
	 * @return
	 */
	private static String formatAsEvent(Enum<?> in) {
		return in.name().toLowerCase();
	}
}
