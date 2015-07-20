package ch.prokopovi.provider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

import android.util.Log;
import ch.prokopovi.Util;
import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.err.WebUpdatingException;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.OperationType;
import ch.prokopovi.struct.Master.ProviderCode;
import ch.prokopovi.struct.Master.RateType;
import ch.prokopovi.struct.ProviderRateBuilder;
import ch.prokopovi.struct.ProviderRequirements;

public class SbrProvider extends AbstractProvider {

	private static final String JSON_SELL_PROP = "sell";
	private static final String JSON_BUY_PROP = "buy";

	private static final String LOG_TAG = "SbrProvider";

	private static final String DATA_URL = "http://www.sbrf.ru/common/js/get_quote_values.php";

	private static final String DATE_FORMAT = "dd.MM.yyyy";
	private static final String JSON_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	private enum SbrCurrencyCode implements CurrencyCodable {
		USD("3", CurrencyCode.USD), EUR("2", CurrencyCode.EUR);

		private final String code;
		private final CurrencyCode currencyCode;

		private SbrCurrencyCode(String code, CurrencyCode currencyCode) {
			this.code = code;
			this.currencyCode = currencyCode;
		}

		@Override
		public CurrencyCode getCurrencyCode() {
			return this.currencyCode;
		}

		public String getCode() {
			return this.code;
		}

		public static SbrCurrencyCode get(CurrencyCode cc) {
			for (SbrCurrencyCode sbrCurrCode : values()) {
				if (sbrCurrCode.getCurrencyCode().equals(cc)) {
					return sbrCurrCode;
				}
			}

			throw new IllegalArgumentException("currency code " + cc
					+ " is not supported");
		}
	}

	/**
	 * build post params
	 * 
	 * @param sbrCurrencyCodes
	 * @param dateFrom
	 * @param dateTo
	 * @return
	 */
	private static String[][] buildPostParams(
			Set<SbrCurrencyCode> sbrCurrencyCodes, Date dateFrom, Date dateTo) {

		final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

		String strDateFrom = sdf.format(dateFrom);
		String strDateTo = sdf.format(dateTo);

		final String[][] hdr = new String[][] {//
		new String[] { "_date_afrom114", strDateFrom },//
				new String[] { "_date_ato114", strDateTo },//
				new String[] { "inf_block", "123" },// region
				new String[] { "cbrf", "0" },// no cbrf rates in result
		};

		List<String[]> list = new ArrayList<String[]>(Arrays.asList(hdr));

		// currencies
		Iterator<SbrCurrencyCode> iter = sbrCurrencyCodes.iterator();
		while (iter.hasNext()) {
			SbrCurrencyCode sbrCurrencyCode = iter.next();

			list.add(new String[] { "qid[]", sbrCurrencyCode.getCode() });
		}

		return list.toArray(new String[list.size()][2]);
	}

	/**
	 * parse json object of date
	 * 
	 * @param obj
	 * @param field
	 *            name of date object field to parse value from
	 * @return
	 * @throws Exception
	 */
	private static double parseDateJson(JSONObject obj, String field)
			throws Exception {
		String str = obj.getString(field);

		double res = Util.parseDotDouble(str);

		return res;
	}

	/**
	 * find object of 'day'
	 * 
	 * @param root
	 *            where to find
	 * @param day
	 *            date to find (only date taken into account)
	 * @return found object or null - if nothing found
	 * 
	 * @throws Exception
	 *             if error occurs
	 */
	private static JSONObject extractDateObject(JSONObject root, Date day)
			throws Exception {

		SimpleDateFormat sdf = new SimpleDateFormat(JSON_DATE_FORMAT);

		Calendar cal1 = Calendar.getInstance();
		cal1.setTime(day);

		Calendar cal2 = Calendar.getInstance();

		Iterator<?> iter = root.keys();
		while (iter.hasNext()) {
			String key = (String) iter.next();

			Date jsonDate = sdf.parse(key);
			cal2.setTime(jsonDate);

			boolean sameDay = cal1.get(Calendar.YEAR) == cal2
					.get(Calendar.YEAR)
					&& cal1.get(Calendar.DAY_OF_YEAR) == cal2
							.get(Calendar.DAY_OF_YEAR);

			if (sameDay) {
				JSONObject res = root.getJSONObject(key);
				Log.d(LOG_TAG, "day: [" + day + "] as key: [" + key
						+ "] found: " + res.toString());
				return res;
			}
		}

		return null;
	}

	@Override
	protected List<ProviderRate> requestRates(
			ProviderRequirements requirements, Date now,
			ProviderRateBuilder builder) throws WebUpdatingException {

		List<ProviderRate> res = new ArrayList<>();

		if (requirements.getCurrencyCodes().size() == 0) return res;

		// currencies
		Set<SbrCurrencyCode> sbrCurrencyCodes = new HashSet<>();
		Iterator<CurrencyCode> iter = requirements.getCurrencyCodes()
				.iterator();
		while (iter.hasNext()) {
			CurrencyCode currencyCode = iter.next();
			try {
				SbrCurrencyCode sbrCurrencyCode = SbrCurrencyCode
						.get(currencyCode);
				sbrCurrencyCodes.add(sbrCurrencyCode);
			} catch (Exception e) {
				continue;
			}
		}

		// dates

		String[][] postParams = buildPostParams(sbrCurrencyCodes, now, now);

		try {
			String data = post(DATA_URL, postParams);
			JSONObject json = new JSONObject(data);

			Iterator<SbrCurrencyCode> it = sbrCurrencyCodes.iterator();
			while (it.hasNext()) {
				SbrCurrencyCode tmpSbrCurr = it.next();

				JSONObject currObj = json.getJSONObject(tmpSbrCurr.getCode());
				JSONObject jsonQuotes = currObj.getJSONObject("quotes");

				JSONObject jsonToday = extractDateObject(jsonQuotes, now);

				try {
					CurrencyCode currencyCode = tmpSbrCurr.getCurrencyCode();

					double todayBuy = parseDateJson(jsonToday, JSON_BUY_PROP);
					res.add(builder.build(OperationType.BUY, currencyCode,
							todayBuy));

					double todaySell = parseDateJson(jsonToday, JSON_SELL_PROP);
					res.add(builder.build(OperationType.SELL, currencyCode,
							todaySell));

				} catch (Exception e) {
					Log.w(LOG_TAG, "error on parsing");
					continue;
				}

			}

		} catch (Exception e) {
			Log.e(LOG_TAG, "error on update");
			throw new WebUpdatingException(e);
		}

		return res;
	}

	@Override
	protected ProviderCode getProviderCode() {
		return ProviderCode.SBR;
	}

	@Override
	CurrencyCodable[] getCurrencyCodables(RateType rt) {

		return RateType.CASH.equals(rt) ? SbrCurrencyCode.values()
				: new SbrCurrencyCode[] {};
	}
}
