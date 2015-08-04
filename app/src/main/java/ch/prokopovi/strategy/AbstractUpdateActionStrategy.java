package ch.prokopovi.strategy;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import ch.prokopovi.ConnectionBroadcastReceiver;
import ch.prokopovi.TimingConstants;
import ch.prokopovi.api.provider.Provider;
import ch.prokopovi.api.provider.Strategy;
import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.db.DbHelper;
import ch.prokopovi.db.ProviderRatesDbAdapter;
import ch.prokopovi.err.OfflineException;
import ch.prokopovi.err.WebUpdatingException;
import ch.prokopovi.provider.ProviderFactory;
import ch.prokopovi.provider.UpdateProcessor;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.OperationType;
import ch.prokopovi.struct.Master.ProviderCode;
import ch.prokopovi.struct.Master.RateType;
import ch.prokopovi.struct.ProviderRequirements;

public abstract class AbstractUpdateActionStrategy implements Strategy {
	private static final String LOG_TAG = "AbsUpdateActionStrategy";

	protected Context context;

	protected final ProviderRequirements requirements;

	protected AbstractUpdateActionStrategy(Context context,
			ProviderRequirements requirements) {

		if (context == null || requirements == null) {
			throw new NullPointerException(
					"constructor arguments can't be null");
		}

		this.context = context;
		this.requirements = requirements;
	}

	@Override
	public void execute() throws WebUpdatingException, OfflineException {
		updateProviderData();
	}

	/**
	 * query DB for provider records according to requirements (provider + rate
	 * type + currencies)
	 * 
	 * @return provider records
	 */
	protected List<ProviderRate> find() {
		ProviderCode providerCode = this.requirements.getProviderCode();
		RateType rateType = this.requirements.getRateType();

		// fetch from db
		List<ProviderRate> list = ProviderRatesDbAdapter.read(this.context,
				providerCode, rateType);

		Set<CurrencyCode> reqCurrs = this.requirements.getCurrencyCodes();
		Iterator<ProviderRate> iterCurr = list.iterator();
		while (iterCurr.hasNext()) {
			ProviderRate providerRate = iterCurr.next();

			// cut off currencies that are not in requirements
			boolean isRequired = reqCurrs.contains(providerRate
					.getCurrencyCode());
			if (!isRequired) {
				iterCurr.remove();
				continue;
			}
			// --- --- ---

		}

		return list;
	}

	/**
	 * does DB data is fresh enough
	 * 
	 * @param list
	 * 
	 * @return true - if data in DB is expired, false - data is fresh
	 */
	protected static boolean isExpired(List<ProviderRate> list) {
		boolean expired = false;

		// is DB data fresh enough
		if (list.isEmpty()) {
			expired = true;
		} else {

			// days off
			ProviderRate firstRate = list.get(0);
			ProviderCode providerCode = firstRate.getProvider();
			Date lastValidDay = providerCode.getLastValidDay();
			// --- --- ---

			long timeUpdated = getLastTimeUpdated(list);

			Log.d(LOG_TAG, "updated at: " + new Date(timeUpdated)
					+ ", last valid: " + lastValidDay);

			long timePassed = lastValidDay.getTime() - timeUpdated;

			expired = (timePassed > TimingConstants.DATA_EXPIRATION_PERIOD);
		}

		Log.d(LOG_TAG, "expired: " + expired);

		return expired;
	}

	/**
	 * find last update time from list
	 * 
	 * @param list
	 * @return minimal last update time or 0 - if smth.
	 */
	protected static long getLastTimeUpdated(List<ProviderRate> list) {

		long timeUpdated = 0L;
		for (ProviderRate providerRate : list) {
			long tmp = providerRate.getTimeUpdated();
			timeUpdated = (timeUpdated == 0L) ? tmp : Math
					.max(timeUpdated, tmp);
		}

		return timeUpdated;
	}

	/**
	 * check whether requested data is already loaded
	 * 
	 * @param requestedCurrencies
	 *            currencies to check for existence
	 * @param list
	 * 
	 * @return true - if data exists in DB, false - otherwise
	 */
	protected boolean hasData(CurrencyCode[] requestedCurrencies,
			List<ProviderRate> list) {

		boolean hasData = true;

		if (list.isEmpty()) {
			hasData = false;
			Log.d(LOG_TAG, "no previous data");
		} else {

			ProviderCode providerCode = this.requirements.getProviderCode();
			RateType rateType = this.requirements.getRateType();

			Provider provider = ProviderFactory.getProvider(providerCode);
			CurrencyCode[] supportedCurrencyCodes = provider
					.getSupportedCurrencyCodes(rateType);
			Set<CurrencyCode> supportedCurrenciesSet = new HashSet<CurrencyCode>(
					Arrays.asList(supportedCurrencyCodes));

			for (CurrencyCode currencyCode : requestedCurrencies) {

				// currency is not supported by provider
				if (!supportedCurrenciesSet.contains(currencyCode)) {
					Log.w(LOG_TAG, currencyCode + " is not supported by "
							+ providerCode + " for " + rateType);
					continue;
				}

				boolean buyFound = false;
				boolean sellFound = false;
				for (ProviderRate providerRate : list) {

					CurrencyCode currFromDb = providerRate.getCurrencyCode();
					if (currFromDb.equals(currencyCode)) {

						OperationType etFromDb = providerRate.getExchangeType();

						if (OperationType.BUY.equals(etFromDb)) {
							buyFound = true;
						} else if (OperationType.SELL.equals(etFromDb)) {
							sellFound = true;
						}
					}
				}

				Log.d(LOG_TAG, "buyFound: " + buyFound + ", sellFound: "
						+ sellFound);

				boolean found = buyFound && sellFound;
				if (!found) {
					Log.d(LOG_TAG, "currency " + currencyCode
							+ " not found in previous data");
					hasData = false;
					break;
				}
			}
		}

		Log.d(LOG_TAG, "hasData: " + hasData);

		return hasData;
	}

	/**
	 * update provider data and save it to dDBccording requirements *
	 * 
	 * @throws WebUpdatingException
	 *             if some errors occurs on web load
	 * @throws OfflineException
	 *             if no connection at all
	 */
	private void updateProviderData() throws WebUpdatingException,
			OfflineException {
		Log.d(LOG_TAG, "attempt to update data by " + this.requirements);

		boolean online = ConnectionBroadcastReceiver.isOnline(this.context);
		if (!online) {
			throw new OfflineException();
		}

		List<ProviderRate> list = UpdateProcessor.process(this.requirements);

		// save to db
		if (list != null && !list.isEmpty()) {
			synchronized (AbstractUpdateActionStrategy.class) {
				DbHelper dbHelper = DbHelper.getInstance(this.context);
				SQLiteDatabase database = dbHelper.getDb();
				ProviderRatesDbAdapter dbAdapter = new ProviderRatesDbAdapter(
						database);

				for (ProviderRate record : list) {
					dbAdapter.save(record);
				}
			}
		}
		// ---
	}
}
