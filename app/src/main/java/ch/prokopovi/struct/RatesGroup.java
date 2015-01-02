package ch.prokopovi.struct;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import ch.prokopovi.struct.Master.CurrencyCode;

/**
 * parent for classes with rate grouping functionality with validation and
 * timing
 * 
 * @author public
 * 
 */
class RatesGroup {

	private static final int INITIAL_TIME_UPDATED = -1;

	/**
	 * check rate validity
	 * 
	 * @param rate
	 *            what to check
	 * 
	 * @return is rate valid
	 */
	private static boolean isValid(Rate rate) {
		boolean res = false;

		CurrencyCode currencyCode = rate.getCurrency();
		RateValue first = rate.getFirst();
		RateValue second = rate.getSecond();

		res = (currencyCode != null) && (first != null || second != null);

		return res;
	}

	private long timeUpdated = INITIAL_TIME_UPDATED;

	private final Map<CurrencyCode, Rate> rates = new LinkedHashMap<CurrencyCode, Rate>();

	/**
	 * unmodifiable map of rates in group. For modification use
	 * {@link #putRate(Rate)}
	 * 
	 * @return
	 */
	public Map<CurrencyCode, Rate> getRates() {
		return Collections.unmodifiableMap(rates);
	}

	/**
	 * calculate time passed since last data update
	 * 
	 * @return time parsed in milliseconds
	 */
	public long getTimePassedSinceLastUpdate() {

		long timeUpdated = getTimeUpdated();
		long currTime = new Date().getTime();
		long diff = currTime - timeUpdated;

		return diff > 0 ? diff : 0;
	}

	public long getTimeUpdated() {
		return timeUpdated;
	}

	/**
	 * check if bunch is newly created or has some data
	 * 
	 * @return true - bunch is new, false - otherwise
	 */
	public boolean isInnocent() {
		return timeUpdated == INITIAL_TIME_UPDATED && rates.isEmpty();
	}

	/**
	 * add/update rate in bunch
	 * 
	 * @param rate
	 */
	public void putRate(Rate rate) {
		boolean valid = isValid(rate);
		if (valid) {
			CurrencyCode currency = rate.getCurrency();
			rates.put(currency, rate);

			timeUpdated = new Date().getTime();
		}
	}
}
