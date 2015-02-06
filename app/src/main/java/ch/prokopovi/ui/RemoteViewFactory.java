package ch.prokopovi.ui;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import ch.prokopovi.IntentFactory;
import ch.prokopovi.R;
import ch.prokopovi.Util;
import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.OperationType;
import ch.prokopovi.struct.Master.ProviderCode;
import ch.prokopovi.struct.Master.RateType;
import ch.prokopovi.struct.WidgetPreferences;
import ch.prokopovi.ui.AbstractWidgetProvider.ClickUiMap;
import ch.prokopovi.ui.AbstractWidgetProvider.MessageUiMap;
import ch.prokopovi.ui.AbstractWidgetProvider.RateUiMap;
import ch.prokopovi.ui.AbstractWidgetProvider.RateValueUiMap;
import ch.prokopovi.ui.AbstractWidgetProvider.WidgetUiMap;
import ch.prokopovi.ui.main.TabsActivity;

public final class RemoteViewFactory {

	private static final String LOG_TAG = "RemoteViewFactory";

	private static final String VALUE_FMT = "#.####";
	private static final String DYNAMIC_FMT = "#.####";

	/**
	 * create full view for medium size widget
	 * 
	 * @param context
	 * @param uiMap
	 *            UI widget map to refer
	 * @param preferences
	 * @param data
	 *            records for same provider and rate type
	 * @return
	 */
	public static RemoteViews buildFullView(Context context, WidgetUiMap uiMap,
			WidgetPreferences preferences, List<ProviderRate> data) {
		RemoteViews views = new RemoteViews(context.getPackageName(),
				uiMap.getLayoutId());

		// defaults
		// click listener
		addServiceClickListener(context, views, preferences, uiMap);

		ProviderCode prefsProvider = preferences.getProviderCode();

		// provider icon
		views.setImageViewResource(uiMap.getPrividerThumbId(),
				prefsProvider.getThumbRes());

		// rate type
		RateType prt = preferences.getRateType();
		RateType rt = RateType.CASH.equals(prt) ? null : prt;
		fillRateType(context, views, uiMap.getRateTypeId(), rt);

		if (data == null || data.size() == 0) {
			Log.w(LOG_TAG, "buildFullView does not accept empty data:" + data);
			return views;
		}
		// --- --- ---

		// rate
		List<RateUiMap> rateUiMaps = uiMap.getRates();
		Set<CurrencyCode> currencyCodes = preferences.getCurrencyCodes();
		Iterator<CurrencyCode> prefCurrIter = currencyCodes.iterator();

		int filledRatesCount = 0;
		long recordUpdated = 0L;

		int len = rateUiMaps.size();
		for (int i = 0; i < len; i++) {
			RateUiMap rateUiMap = rateUiMaps.get(i);

			resetRate(views, rateUiMap);

			boolean hasNext = prefCurrIter.hasNext();
			if (hasNext) {
				CurrencyCode prefCurr = prefCurrIter.next();

				// filter by currency
				List<ProviderRate> singleCurrency = new ArrayList<ProviderRate>();
				for (ProviderRate rec : data) {
					if (rec.getCurrencyCode().equals(prefCurr)) {
						singleCurrency.add(rec);
						recordUpdated = rec.getTimeUpdated();
					}
				}
				// ---

				if (!singleCurrency.isEmpty()) {
					filledRatesCount += fillRate(context, views, rateUiMap,
							singleCurrency);
				}
			}
		}
		// ---

		if (filledRatesCount == 0) {
			Log.w(LOG_TAG, "no views were filled for:" + preferences);

			String footerText = context.getResources().getString(
					R.string.err_short_nodata);
			views.setTextViewText(uiMap.getTimeUpdatedId(), footerText);
			return views;
		}

		// footer text
		switch (uiMap.getWidgetSize()) {
		case MINI:
			// first currency in requirements
			views.setTextViewText(uiMap.getTimeUpdatedId(), currencyCodes
					.iterator().next().name());

			break;
		case MEDIUM:
		case WIDE:
		case LARGE:
			Log.d(LOG_TAG, "recordUpdated: " + new Date(recordUpdated));
			String footerText = getFooterDate(recordUpdated);
			views.setTextViewText(uiMap.getTimeUpdatedId(), footerText);
			break;
		}

		return views;
	}

	/**
	 * reset UI rate value to <empty value>
	 * 
	 * @param views
	 * @param rateUiMap
	 */
	private static void resetRate(RemoteViews views, RateUiMap rateUiMap) {

		Integer currencyTextId = rateUiMap.getCurrencyTextId();
		if (currencyTextId != null)
			views.setViewVisibility(currencyTextId, View.GONE);

		RateValueUiMap buyUi = rateUiMap.getBuy();
		RateValueUiMap sellUi = rateUiMap.getSell();

		views.setTextViewText(buyUi.getCurrentId(), "");
		views.setTextViewText(sellUi.getCurrentId(), "");

		Integer buyDirectId = buyUi.getDirectionId();
		if (buyDirectId != null) {
			views.setImageViewResource(buyDirectId, R.drawable.arrow_up);
			views.setViewVisibility(buyDirectId, View.GONE);
		}

		Integer sellDirectId = sellUi.getDirectionId();
		if (sellDirectId != null) {
			views.setImageViewResource(sellDirectId, R.drawable.arrow_up);
			views.setViewVisibility(sellDirectId, View.GONE);
		}

		Integer buyDynId = buyUi.getDynamicId();
		if (buyDynId != null)
			views.setTextViewText(buyDynId, "");

		Integer sellDynId = sellUi.getDynamicId();
		if (sellDynId != null)
			views.setTextViewText(sellDynId, "");

	}

	/**
	 * create footer view for medium and large size widget
	 * 
	 * @param uiMap
	 *            UI widget map to refer
	 * @param packageName
	 * @param footerText
	 * @return
	 */
	public static RemoteViews buildFooterView(WidgetUiMap uiMap,
			String packageName, String footerText) {
		RemoteViews views = new RemoteViews(packageName, uiMap.getLayoutId());

		switch (uiMap.getWidgetSize()) {
		case MINI:
			break;
		case MEDIUM:
		case WIDE:
		case LARGE:
			views.setTextViewText(uiMap.getTimeUpdatedId(), footerText);
			break;
		}

		return views;
	}

	/**
	 * construct widget error views
	 * 
	 * @param context
	 * @param msgUiMap
	 * @param txt
	 *            error text
	 * @return
	 */
	public static RemoteViews buildMessageView(Context context,
			MessageUiMap msgUiMap, String txt, WidgetPreferences preferences) {

		RemoteViews views = new RemoteViews(context.getPackageName(),
				msgUiMap.getLayoutId());

		views.setTextViewText(msgUiMap.getTextId(), txt);

		addServiceClickListener(context, views, preferences, msgUiMap);

		return views;
	}

	/**
	 * construct widget normal views
	 * 
	 * @param ctx
	 *            context
	 * @param views
	 *            view to fill
	 * @param rateUiMap
	 * @param rate
	 * 
	 * @return filled rate views count
	 */
	private static int fillRate(Context ctx, RemoteViews views,
			RateUiMap rateUiMap, List<ProviderRate> rate) {

		if (rate == null || rate.size() == 0) {
			Log.w(LOG_TAG, "fillRate does not accept empty rate:" + rate);
			return 0;
		}

		Integer currencyTextId = rateUiMap.getCurrencyTextId();

		CurrencyCode currencyCode = rate.get(0).getCurrencyCode();

		if (currencyTextId != null)
			views.setViewVisibility(currencyTextId, View.VISIBLE);

		if (currencyTextId != null) {
			String txt = ctx.getResources().getString(
					currencyCode.getTitleRes());
			views.setTextViewText(currencyTextId, txt);
		}

		// sort by exchange type
		List<ProviderRate> buyPair = new ArrayList<ProviderRate>(2);
		List<ProviderRate> sellPair = new ArrayList<ProviderRate>(2);
		for (ProviderRate providerRate : rate) {
			OperationType operationType = providerRate.getExchangeType();
			switch (operationType) {
			case BUY:
				buyPair.add(providerRate);
				break;
			case SELL:
				sellPair.add(providerRate);
				break;
			}
		}
		// ---

		int res = 0;
		res += fillRateValue(views, rateUiMap.getBuy(), buyPair);
		res += fillRateValue(views, rateUiMap.getSell(), sellPair);

		return res > 0 ? 1 : 0;
	}

	/**
	 * fill numeric part of provided view with data
	 * 
	 * @param views
	 *            view to fill
	 * @param rateValueUiMap
	 *            view map
	 * @param pair
	 *            data
	 * @return filled value views count
	 */
	private static int fillRateValue(RemoteViews views,
			RateValueUiMap rateValueUiMap, List<ProviderRate> pair) {

		if (pair == null || pair.size() < 1) {
			Log.w(LOG_TAG, "fillRateValue does not accept empty pair: " + pair);
			return 0;
		}

		boolean isSinglePair = pair.size() == 1;

		// define order
		ProviderRate current = !isSinglePair ? pair.get(1) : pair.get(0);
		ProviderRate prev = !isSinglePair ? pair.get(0) : null;

		Log.d(LOG_TAG, "current: " + current);

		if (current == null || current.getValue() == null) {
			Log.i(LOG_TAG,
					"fillRateValue does not accept pair without current value");
			return 0;
		}
		// ---

		Double currRateVal = current.getValue();

		// rate dynamics in percent
		Double dynamics = 0.0;
		String strDynamic = null;
		if (prev != null && prev.getValue() != null) {
			Log.d(LOG_TAG, "prev " + prev);
			dynamics = calculateDynamics(prev.getValue(), currRateVal);

			DecimalFormat dynFormat = new DecimalFormat(DYNAMIC_FMT);
			strDynamic = dynFormat.format(dynamics);
			strDynamic = strDynamic.replace("-", "");
		}
		// ---

		// direction
		Boolean direction = null;
		boolean isZero = Util.isZero(dynamics);
		if (!isZero) {
			direction = dynamics > 0;
		}
		// ---

		// fill UI ------------------------------------------------

		// main (current) rate value
		DecimalFormat valFormat = new DecimalFormat(VALUE_FMT);
		String strCurrent = valFormat.format(currRateVal);
		views.setTextViewText(rateValueUiMap.getCurrentId(), strCurrent);
		// ---

		boolean hasDynamic = (direction != null && dynamics != null);

		Integer directionId = rateValueUiMap.getDirectionId();
		boolean hasDirectionView = (directionId != null);
		if (hasDynamic && hasDirectionView) {
			int srcId = direction ? R.drawable.arrow_up : R.drawable.arrow_down;
			views.setImageViewResource(directionId, srcId);
		}

		if (hasDirectionView) {
			int directionVisibility = hasDynamic ? View.VISIBLE
					: View.INVISIBLE;
			views.setViewVisibility(directionId, directionVisibility);
		}

		Integer dynamicId = rateValueUiMap.getDynamicId();
		boolean hasDynamicView = (dynamicId != null);
		if (hasDynamicView) {
			strDynamic = hasDynamic ? strDynamic : "";
			views.setTextViewText(dynamicId, strDynamic);
		}

		return 1;
	}

	/**
	 * fill rate type indicator
	 * 
	 * @param context
	 * @param views
	 * @param rateTypeTvId
	 * @param rateType
	 * 
	 */
	private static void fillRateType(Context context, RemoteViews views,
			int rateTypeTvId, RateType rateType) {

		if (rateType == null) {
			views.setViewVisibility(rateTypeTvId, View.INVISIBLE);
			views.setTextViewText(rateTypeTvId, "");
		} else {
			views.setViewVisibility(rateTypeTvId, View.VISIBLE);

			String txt = context.getResources().getString(
					rateType.getShortTitleRes());

			views.setTextViewText(rateTypeTvId, txt);
		}
	}

	private static String getFooterDate(long timeUpdated) {
		SimpleDateFormat df = new SimpleDateFormat("H:mm");

		boolean today = DateUtils.isToday(timeUpdated);
		if (!today) {
			df = new SimpleDateFormat("dMMM H:mm");
		}

		String res = df.format(new Date(timeUpdated));
		Log.d(LOG_TAG, "footerDate " + res);

		return res;
	}

	/**
	 * fill views with listener, calling service to update
	 * 
	 * @param context
	 * @param views
	 * @param preferences
	 * @param uiMap
	 */
	private static void addServiceClickListener(Context context,
			RemoteViews views, WidgetPreferences preferences, ClickUiMap uiMap) {

		// launch app
		Integer launchAppClickId = uiMap.getLaunchAppClickId();
		if (launchAppClickId != null) {

			Intent i = TabsActivity.getLauncherIntent(context);

			PendingIntent pi = PendingIntent.getActivity(context,
					preferences.getWidgetId(), i,
					PendingIntent.FLAG_CANCEL_CURRENT);

			views.setOnClickPendingIntent(launchAppClickId, pi);
		}

		// Intent to launch UpdateService
		Integer updClickId = uiMap.getUpdClickId();
		if (updClickId != null) {
			ProviderCode providerCode = preferences.getProviderCode();

			PendingIntent pendingIntent = IntentFactory
					.createForceUpdateServiceIntent(context, providerCode,
							preferences.getRateType(),
							preferences.getWidgetId());

			views.setOnClickPendingIntent(updClickId, pendingIntent);
		}

		// launch cfg
		Integer cfgClickId = uiMap.getCfgClickId();
		if (cfgClickId != null) {

			// intent to launch widget configure activity
			Class<?> cfgActivity = uiMap.getWidgetSize().getConfigurator();

			Intent intent = new Intent(context, cfgActivity);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
					preferences.getWidgetId());
			PendingIntent pendingIntent2 = PendingIntent.getActivity(context,
					preferences.getWidgetId(), intent,
					PendingIntent.FLAG_CANCEL_CURRENT);

			views.setOnClickPendingIntent(cfgClickId, pendingIntent2);

		}
	}

	/**
	 * calculate value of dynamics (increasing/decreasing percentage)
	 * 
	 * @param prev
	 * @param curr
	 * 
	 * @return dynamics value
	 */
	private static Double calculateDynamics(Double prev, Double curr) {
		// Double dyn = (curr - prev) * 100 / prev;
		Double dyn = (curr - prev);

		return dyn;
	}
}
