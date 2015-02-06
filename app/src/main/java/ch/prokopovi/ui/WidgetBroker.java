package ch.prokopovi.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import android.util.SparseArray;
import android.widget.RemoteViews;
import ch.prokopovi.PrefsUtil;
import ch.prokopovi.R;
import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.db.ProviderRatesDbAdapter;
import ch.prokopovi.struct.Master;
import ch.prokopovi.struct.Master.ProviderCode;
import ch.prokopovi.struct.Master.RateType;
import ch.prokopovi.struct.Master.WidgetSize;
import ch.prokopovi.struct.WidgetPreferences;
import ch.prokopovi.ui.AbstractWidgetProvider.WidgetUiMap;

public class WidgetBroker {

	private static final String LOG_TAG = "WidgetBroker";

	private static final Class<?>[] PROVIDER_CLASSES = new Class<?>[] {
			MiniWidgetProvider.class, WidgetProvider.class,
			MultiWidgetProvider.class, WideWidgetProvider.class };

	public enum WidgetUpdateType {
		FULL, UPDATING;
	}

	private WidgetBroker(Context c) {
	}

	/**
	 * update widgets with particular bunch
	 * 
	 * @param context
	 * @param provider
	 * @param rateType
     * @param updateType
	 */
	public static void update(final Context context, ProviderCode provider,
			RateType rateType, final WidgetUpdateType updateType) {

		Log.d(LOG_TAG, "update type=" + updateType + " provider=" + provider
				+ " rateType=" + rateType);

		if (provider == null || rateType == null) {
			Log.w(LOG_TAG, "empty update input");
			return;
		}

		// fetch from db
		final List<ProviderRate> list = ProviderRatesDbAdapter.read(context,
				provider, rateType);

		if (list.isEmpty())
			return;

		Integer mapCode = Master.calcMapCode(provider, rateType);

		loopBuild(context, mapCode, new Buildable() {
			@Override
			public RemoteViews build(WidgetUiMap uiMap,
					WidgetPreferences preferences) {

				RemoteViews views = null;
				switch (updateType) {
				case FULL:
					views = RemoteViewFactory.buildFullView(context, uiMap,
							preferences, list);
					break;
				case UPDATING:
					String footerText = context.getResources().getString(
							R.string.msg_updating);

					views = RemoteViewFactory.buildFooterView(uiMap,
							context.getPackageName(), footerText);
					break;
				}

				return views;
			}
		});
	}

	/**
	 * show message views
	 * 
	 * @param inMapCode
	 * @param txt
	 */
	public static void message(final Context context, Integer inMapCode,
			final String txt) {

		Log.d(LOG_TAG, "message = " + txt);

		loopBuild(context, inMapCode, new Buildable() {
			@Override
			public RemoteViews build(WidgetUiMap uiMap,
					WidgetPreferences preferences) {

				RemoteViews views = RemoteViewFactory.buildMessageView(context,
						uiMap.getMessageUiMap(), txt, preferences);

				return views;
			}
		});
	}

	/**
	 * get widget ids filtered by map code (according widget prefs)
	 * 
	 * @param inMapCode
	 * @return
	 */
	private static List<Integer> filterWidgetIds(Context context,
			Integer inMapCode) {

		if (inMapCode == null) {
			return Collections.emptyList();
		}

		SparseArray<WidgetPreferences> widgetPrefsMap = getWidgetPrefsMap(context);

		List<Integer> res = new ArrayList<Integer>();
		for (int i = 0; i < widgetPrefsMap.size(); i++) {
			WidgetPreferences widgetPreferences = widgetPrefsMap.valueAt(i);

			RateType widgetRateType = widgetPreferences.getRateType();
			ProviderCode widgetProvider = widgetPreferences.getProviderCode();

			Integer widgetMapCode = Master.calcMapCode(widgetProvider,
					widgetRateType);

			if (inMapCode != null && !inMapCode.equals(widgetMapCode)) {
				continue;
			}

			int widgetId = widgetPreferences.getWidgetId();
			res.add(widgetId);
		}

		return res;
	}

	/**
	 * builder of remote views
	 * 
	 * @author Pavel_Letsiaha
	 * 
	 */
	private interface Buildable {

		/**
		 * build view
		 * 
		 * @param uiMap
		 *            map of view to fill
		 * @param preferences
		 *            widget preferences to rely on
		 * @return
		 */
		RemoteViews build(WidgetUiMap uiMap, WidgetPreferences preferences);
	}

	/**
	 * find out widget size by id
	 * 
	 * @param widgetManager
	 * @param widgetId
	 * @return
	 */
	public static WidgetSize getWidgetSize(AppWidgetManager widgetManager,
			Integer widgetId) {

		AppWidgetProviderInfo widgetInfo = widgetManager
				.getAppWidgetInfo(widgetId);
		String providerClassName = widgetInfo.provider.getClassName();

		WidgetSize widgetSize = WidgetSize.get(providerClassName);

		return widgetSize;
	}

	private static void loopBuild(Context context, Integer inMapCode,
			Buildable callback) {

		if (inMapCode == null) {
			Log.w(LOG_TAG, "empty loopBuild input");
			return;
		}

		List<Integer> widgetIds = filterWidgetIds(context, inMapCode);

		SparseArray<WidgetPreferences> widgetPrefsMap = getWidgetPrefsMap(context);
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);

		for (Integer widgetId : widgetIds) {

			WidgetPreferences preferences = widgetPrefsMap.get(widgetId);

			WidgetUiMap uiMap = null;
			WidgetSize widgetSize = getWidgetSize(widgetManager, widgetId);
			switch (widgetSize) {
			case MINI:
				uiMap = MiniWidgetProvider.UI_MAP;
				break;
			case MEDIUM:
				uiMap = WidgetProvider.UI_MAP;
				break;
			case WIDE:
				uiMap = WideWidgetProvider.UI_MAP;
				break;
			case LARGE:
				uiMap = MultiWidgetProvider.UI_MAP;
				break;
			}

			RemoteViews views = callback.build(uiMap, preferences);

			if (views != null)
				widgetManager.updateAppWidget(widgetId, views);
		}
	}

	/**
	 * get valid widget preferences by id
	 * 
	 * @param context
	 * 
	 * @return map of {widgetId, widgetPrefs}
	 */
	public static SparseArray<WidgetPreferences> getWidgetPrefsMap(
			Context context) {

		SparseArray<WidgetPreferences> res = new SparseArray<WidgetPreferences>();

		AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);

		List<Integer> appWidgetIds = new ArrayList<Integer>();
		for (Class<?> compClass : PROVIDER_CLASSES) {
			ComponentName componentName = new ComponentName(context, compClass);

			int[] tmpWidgetIds = widgetManager.getAppWidgetIds(componentName);
			for (int tmpWidgetId : tmpWidgetIds) {
				appWidgetIds.add(tmpWidgetId);
			}
		}
		Log.d(LOG_TAG, "all widget ids are " + appWidgetIds);

		for (int appWidgetId : appWidgetIds) {
			WidgetPreferences prefs;
			try {
				prefs = PrefsUtil.load(context, appWidgetId);

				if (prefs == null || prefs.getProviderCode() == null) {
					continue;
				}

				res.put(appWidgetId, prefs);
			} catch (Exception e) {
				Log.w(LOG_TAG, "error on load prefs for widget id:"
						+ appWidgetId);
				continue;
			}
		}

		return res;
	}
}
