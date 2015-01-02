package ch.prokopovi;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.WindowManager;
import ch.prokopovi.struct.Master;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.ProviderCode;
import ch.prokopovi.struct.Master.RateType;
import ch.prokopovi.struct.ProviderRequirements;
import ch.prokopovi.struct.WidgetPreferences;

/**
 * helper class to process config operations
 * 
 * @author Pavel_Letsiaha
 * 
 */
public class PrefsUtil {

	private static final String LOG_TAG = "PrefsUtil";

	public static final String PREFS_NAME = "ch.prokopovi.WidgetProvider";

	public static void initSkin(ContextThemeWrapper ctx) {

		String prefAppSkinKey = ctx.getString(R.string.pref_app_skin);

		String skin = ctx.getSharedPreferences(PrefsUtil.PREFS_NAME,
				Context.MODE_PRIVATE).getString(prefAppSkinKey, null);

		if (ctx.getString(R.string.pref_app_skin_value_dark).equals(skin)) {
			ctx.setTheme(R.style.Theme_Puls_Dark);
		} else {
			ctx.setTheme(R.style.Theme_Puls_Light);
		}
	}

	public static void initFullscreen(Activity activity) {
		SharedPreferences prefs = activity.getSharedPreferences(
				PrefsUtil.PREFS_NAME, Context.MODE_PRIVATE);

		String prefFullscreenOnKey = activity
				.getString(R.string.pref_fullscreen_on);

		boolean fullscreen = prefs.getBoolean(prefFullscreenOnKey, true);

		if (fullscreen) {

			activity.getWindow().setFlags(
					WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		//
	}

	/**
	 * clean all prefs for widget
	 * 
	 * @param context
	 * @param widgetId
	 *            widget to clean props for
	 */
	public static void cleanUp(Context context, int widgetId) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME,
				Context.MODE_PRIVATE);
		if (prefs != null) {
			Editor edit = prefs.edit();

			if (edit != null) {
				Map<String, ?> map = prefs.getAll();

				Iterator<String> iterator = map.keySet().iterator();
				while (iterator.hasNext()) {
					String key = iterator.next();

					if (key.contains(String.valueOf(widgetId))) {
						edit.remove(key);
					}
				}

				edit.commit();
			}
		}
	}

	/**
	 * parse configuration and find values of providers and their currencies
	 * expected to be fetched
	 * 
	 * @param context
	 * 
	 * @return requirements map
	 * @throws JSONException
	 */
	public static SparseArray<ProviderRequirements> collectRequirements(
			Context context) {

		SparseArray<ProviderRequirements> res = new SparseArray<ProviderRequirements>();

		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME,
				Context.MODE_PRIVATE);
		Map<String, ?> map = prefs.getAll();
		Iterator<String> iterator = map.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();

			try {
				int appWidgetId = Integer.parseInt(key);

				WidgetPreferences preferences = load(context, appWidgetId);

				Log.d(LOG_TAG, "collecting: " + preferences);

				ProviderCode providerCode = preferences.getProviderCode();

				RateType rateType = preferences.getRateType();

				Integer mapCode = Master.calcMapCode(providerCode, rateType);

				if (res.get(mapCode) == null) {
					ProviderRequirements requirements = new ProviderRequirements(
							providerCode, rateType);
					res.put(mapCode, requirements);
				}

				Collection<CurrencyCode> currencies = preferences
						.getCurrencyCodes();

				ProviderRequirements requirements = res.get(mapCode);
				requirements.getCurrencyCodes().addAll(currencies);

			} catch (NumberFormatException e1) {
				Log.w(LOG_TAG, "prop key: " + key + " is not for widget");
				continue;
			} catch (JSONException e) {
				Log.w(LOG_TAG, "bad json in: " + key);
				continue;
			}
		}

		return res;
	}

	/**
	 * get config by widget id
	 * 
	 * @param context
	 * @param appWidgetId
	 * @return
	 * @throws JSONException
	 */
	public static WidgetPreferences load(Context context, int appWidgetId)
			throws JSONException {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME,
				Context.MODE_PRIVATE);

		String str = prefs.getString(String.valueOf(appWidgetId), null);
		// Log.d(LOG_TAG, "prefs str " + str);

		WidgetPreferences preferences = null;
		if (str != null) {
			preferences = new WidgetPreferences(str);

			ProviderCode providerCode = preferences.getProviderCode();

			RateType rateType = preferences.getRateType();

			// obsolete prefs (set dafaults and save)
			if (providerCode == null || rateType == null) {
				Log.w(LOG_TAG, "upgrading obsolete config");

				providerCode = ProviderCode.getDefault();
				rateType = RateType.getDefault();

				preferences.setProviderCode(providerCode);
				preferences.setRateType(rateType);

				save(context, preferences.getWidgetId(), preferences);
			}
			// ---
		}

		return preferences;
	}

	public static void save(Context context, int appWidgetId,
			WidgetPreferences preferences) throws JSONException {

		preferences.setWidgetId(appWidgetId);

		Log.d(LOG_TAG, "saving widget id: " + appWidgetId + " prefs: "
				+ preferences);

		String str = preferences.to();

		SharedPreferences.Editor prefs = context.getSharedPreferences(
				PREFS_NAME, Context.MODE_PRIVATE).edit();
		prefs.putString(String.valueOf(appWidgetId), str);
		prefs.commit();
	}
}
