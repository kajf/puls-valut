package ch.prokopovi;

import java.util.Arrays;
import java.util.Set;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

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
	public static final String ACTION_FORCE_UPDATE = SERVICE_PREFIX
			+ ".ACTION_FORCE_UPDATE"; // user request
	public static final String ACTION_ROUTINE_UPDATE = SERVICE_PREFIX
			+ ".ACTION_ROUTINE_UPDATE"; // OS (new widget or update)

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

		Intent intent = new Intent(context, UpdateBroadcastReceiver.class);
		intent.setAction(ACTION_FORCE_UPDATE);
		intent.putExtra(ExtrasKey.PROVIDER.name(), provider);
		intent.putExtra(ExtrasKey.RATE_TYPE.name(), rateType);

		return PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_CANCEL_CURRENT);
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
	public static Intent createWeakUpdateServiceIntent(Context context,
			ProviderCode provider, CurrencyCode[] currencies, int id,
			RateType rateType) {


		Log.d(LOG_TAG, "creating weak service intent (" + id + ") for "
				+ provider + " currencies " + Arrays.toString(currencies));

		if (provider == null || currencies == null) {
			throw new NullPointerException(
					"input nulls are not permitted on service intent creation");
		}

        Intent intent = new Intent(context, UpdateService.class);
        intent.setAction(ACTION_ROUTINE_UPDATE);
        intent.putExtra(ExtrasKey.PROVIDER.name(), provider);
        intent.putExtra(ExtrasKey.RATE_TYPE.name(), rateType);
        intent.putExtra(ExtrasKey.CURRENCIES.name(), currencies);


		return intent;
	}

	/**
	 * build conditional update intent
	 * 
	 * @param context
	 * @param prefs
	 * @return
	 */
	public static Intent createWeakUpdateServiceIntent(Context context,
			WidgetPreferences prefs) {

		Log.d(LOG_TAG, "creating weak service intent by prefs " + prefs);

		ProviderCode provider = prefs.getProviderCode();
		RateType rateType = prefs.getRateType();
		Set<CurrencyCode> currencyCodes = prefs.getCurrencyCodes();

		CurrencyCode[] currencies = currencyCodes
				.toArray(new CurrencyCode[currencyCodes.size()]);

		Intent pendingIntent = createWeakUpdateServiceIntent(context,
				provider, currencies, prefs.getWidgetId(), rateType);

		return pendingIntent;
	}
}
