package ch.prokopovi.provider;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

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

public class CbrProvider extends AbstractProvider {

	private static final String LOG_TAG = "CbrProvider";

	private static final String DATA_URL_FORMAT = "http://www.cbr.ru/scripts/XML_dynamic.asp?date_req1=%1$td/%1$tm/%1$tY&date_req2=%2$td/%2$tm/%2$tY&VAL_NM_RQ=%3$s";
	private static final String DATE_XPATH_FORMAT = "//Record[@Date='%1$td.%1$tm.%1$tY']/Value/text()";

	private enum CbrCurrencyCode implements CurrencyCodable {
		USD("R01235", CurrencyCode.USD), //
		EUR("R01239", CurrencyCode.EUR), //

		BYR("R01090", CurrencyCode.BYR), //
		PLN("R01565", CurrencyCode.PLN), //

		UAH("R01720", CurrencyCode.UAH), //
		GBP("R01035", CurrencyCode.GBP), //
		JPY("R01820", CurrencyCode.JPY), //
		;

		private final String code;
		private final CurrencyCode currencyCode;

		private CbrCurrencyCode(String code, CurrencyCode currencyCode) {
			this.code = code;
			this.currencyCode = currencyCode;
		}

		public static CbrCurrencyCode get(CurrencyCode curr) {
			for (CbrCurrencyCode val : values()) {
				if (val.getCurrencyCode().equals(curr)) {
					return val;
				}
			}

			throw new IllegalArgumentException("currency code " + curr
					+ " is not supported");
		}

		public String getCode() {
			return this.code;
		}

		@Override
		public CurrencyCode getCurrencyCode() {
			return this.currencyCode;
		}
	}

	/**
	 * create service request line (period starting 'frame' days before today
	 * and ending in 'date')
	 * 
	 * @param code
	 *            currency code to request
	 * @param begin
	 *            begin of period
	 * @param end
	 *            end of period
	 * 
	 * 
	 * @return service request line
	 */
	private static String buildUrlString(CurrencyCode code, Date begin, Date end) {

		CbrCurrencyCode providerCurrencyCode = CbrCurrencyCode.get(code);
		String currStr = providerCurrencyCode.getCode();

		return String.format(DATA_URL_FORMAT, begin, end, currStr);
	}

	/**
	 * make xpath like //Record[@Date='28.07.2012']/Value/text() with
	 * customizable date
	 * 
	 * @param date
	 * @return
	 */
	private static String buildDateXpath(Date date) {
		return String.format(DATE_XPATH_FORMAT, date);
	}

	/**
	 * extract rate value from root node by date
	 * 
	 * @param date
	 * @param tagNode
	 * @return
	 * @throws XPatherException
	 * @throws ParseException
	 */
	private static Double extractValue(Date date, TagNode tagNode)
			throws XPatherException, ParseException {
		Double res = null;

		String xpath = buildDateXpath(date);
		Object[] nodes = tagNode.evaluateXPath(xpath);
		if (nodes != null && nodes.length > 0) {
			res = Util.parseCommaDouble(nodes[0].toString());
		}

		return res;
	}

	@Override
	protected List<ProviderRate> requestRates(
			ProviderRequirements requirements, Date now,
			ProviderRateBuilder builder) throws WebUpdatingException {

		Set<CurrencyCode> currencyCodes = requirements.getCurrencyCodes();

		Calendar cal = Calendar.getInstance();
		cal.setTime(now);

		cal.add(Calendar.DATE, 1);
		Date tomorrow = cal.getTime();

		List<ProviderRate> res = new ArrayList<ProviderRate>();
		for (CurrencyCode currencyCode : currencyCodes) {

			try {
				String location = buildUrlString(currencyCode, now, tomorrow);

				TagNode tagNode = ProviderUtils.load(location);

				Double tomorrowValue = extractValue(tomorrow, tagNode);
				res.add(builder.build(OperationType.BUY, currencyCode,
						tomorrowValue));

				Double nowValue = extractValue(now, tagNode);
				if (nowValue != null) {
					res.add(builder.build(OperationType.SELL, currencyCode,
							nowValue));
				} // now value should not be null

			} catch (Exception e) {
				Log.w(LOG_TAG, "error on loading", e);
				continue;
			}
		}

		return res;
	}

	@Override
	protected ProviderCode getProviderCode() {
		return ProviderCode.CBR;
	}

	@Override
	CurrencyCodable[] getCurrencyCodables(RateType rt) {

		return RateType.CASH.equals(rt) ? CbrCurrencyCode.values()
				: new CbrCurrencyCode[] {};
	}
}
