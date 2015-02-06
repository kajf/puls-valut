package ch.prokopovi;

import java.util.Arrays;
import java.util.Set;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import ch.prokopovi.ScheduleManager.ScheduledIntentCode;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.ProviderCode;
import ch.prokopovi.struct.Master.RateType;
import ch.prokopovi.struct.WidgetPreferences;

public final class IntentFactory {
	public enum ExtrasKey {
		PROVIDER, RATE_TYPE, CURRENCIES;
	}

	private static final String LOG_TAG = "IntentFactory";

	public static final String SERVICE_PREFIX = UpdateService.class
			.getCanonicalName();
	public static final String ACTION_SCHEDULED_UPDATE = SERVICE_PREFIX
			+ ".ACTION_SCHEDULED_UPDATE"; // alarm
	public static final String ACTION_FORCE_UPDATE = SERVICE_PREFIX
			+ ".ACTION_FORCE_UPDATE"; // user request
	public static final String ACTION_ROUTINE_UPDATE = SERVICE_PREFIX
			+ ".ACTION_ROUTINE_UPDATE"; // OS (new widget or update)
	public static final String ACTION_EXPIRED_UPDATE = SERVICE_PREFIX
			+ ".ACTION_EXPIRED_UPDATE"; // connection restored

	/**
	 * build update-if-expired intent
	 * 
	 * @param context
	 * @return
	 */
	public static PendingIntent createExpiredUpdateServiceIntent(Context context) {

		Log.d(LOG_TAG, "creating expired intent");

		// Intent intent = new Intent(context, UpdateService.class);
		// intent.setAction(ACTION_EXPIRED_UPDATE);
		Intent intent = UpdateService_.intent(context).get()
				.setAction(ACTION_EXPIRED_UPDATE);
		PendingIntent pendingIntent = PendingIntent.getService(context, 0,
				intent, PendingIntent.FLAG_CANCEL_CURRENT);

		return pendingIntent;
	}

	/**
	 * build intent to launch service
	 * 
	 * @param context
	 * @param provider
	 * @param rateType
	 * @param id
	 * 
	 * @return intent
	 */
	public static PendingIntent createForceUpdateServiceIntent(Context context,
			ProviderCode provider, RateType rateType, int id) {
		Log.d(LOG_TAG, "creating force intent (" + id + ") for " + provider);

		// Intent intent = new Intent(context, UpdateService.class);
		// intent.setAction(ACTION_FORCE_UPDATE);
		// intent.putExtra(ExtrasKey.PROVIDER.name(), provider);
		// intent.putExtra(ExtrasKey.RATE_TYPE.name(), rateType);

		Intent intent = UpdateService_.intent(context).get()
				.setAction(ACTION_FORCE_UPDATE)
				.putExtra(ExtrasKey.PROVIDER.name(), provider)
				.putExtra(ExtrasKey.RATE_TYPE.name(), rateType);

		PendingIntent pendingIntent = PendingIntent.getService(context, id,
				intent, PendingIntent.FLAG_CANCEL_CURRENT);

		return pendingIntent;
	}

	/**
	 * scheduled update service intent
	 * 
	 * @param context
	 * @return
	 */
	public static PendingIntent createScheduledUpdateServiceIntent(
			Context context, ScheduledIntentCode сode) {

		Log.d(LOG_TAG, "creating scheduled intent");

		// Intent intent = new Intent(context, UpdateService.class);
		// intent.setAction(ACTION_SCHEDULED_UPDATE);

		Intent intent = UpdateService_.intent(context).get()
				.setAction(ACTION_SCHEDULED_UPDATE);

		PendingIntent pendingIntent = PendingIntent.getService(context,
				сode.ordinal(), intent, PendingIntent.FLAG_CANCEL_CURRENT);

		return pendingIntent;
	}

	/**
	 * build conditional update intent
	 * 
	 * @param context
	 * @param provider
	 * @param currencies
	 * @param id
	 * @param rateType
	 * 
	 * @return
	 */
	public static PendingIntent createWeakUpdateServiceIntent(Context context,
			ProviderCode provider, CurrencyCode[] currencies, int id,
			RateType rateType) {

		Log.d(LOG_TAG, "creating weak service intent (" + id + ") for "
				+ provider + " currencies " + Arrays.toString(currencies));

		if (provider == null || currencies == null) {
			throw new NullPointerException(
					"input nulls are not permitted on service intent creation");
		}

		// Intent intent = new Intent(context, UpdateService.class);
		// intent.setAction(ACTION_ROUTINE_UPDATE);
		// intent.putExtra(ExtrasKey.PROVIDER.name(), provider);
		// intent.putExtra(ExtrasKey.RATE_TYPE.name(), rateType);
		// intent.putExtra(ExtrasKey.CURRENCIES.name(), currencies);

		Intent intent = UpdateService_.intent(context).get()
				.setAction(ACTION_ROUTINE_UPDATE)
				.putExtra(ExtrasKey.PROVIDER.name(), provider)
				.putExtra(ExtrasKey.RATE_TYPE.name(), rateType)
				.putExtra(ExtrasKey.CURRENCIES.name(), currencies);

		PendingIntent pendingIntent = PendingIntent.getService(context, id,
				intent, PendingIntent.FLAG_CANCEL_CURRENT);

		return pendingIntent;
	}

	/**
	 * build conditional update intent
	 * 
	 * @param context
	 * @param prefs
	 * @return
	 */
	public static PendingIntent createWeakUpdateServiceIntent(Context context,
			WidgetPreferences prefs) {

		Log.d(LOG_TAG, "creating weak service intent by prefs " + prefs);

		ProviderCode provider = prefs.getProviderCode();
		RateType rateType = prefs.getRateType();
		Set<CurrencyCode> currencyCodes = prefs.getCurrencyCodes();

		CurrencyCode[] currencies = currencyCodes
				.toArray(new CurrencyCode[currencyCodes.size()]);

		PendingIntent pendingIntent = createWeakUpdateServiceIntent(context,
				provider, currencies, prefs.getWidgetId(), rateType);

		return pendingIntent;
	}
}
