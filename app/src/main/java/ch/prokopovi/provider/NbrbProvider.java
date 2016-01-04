package ch.prokopovi.provider;

import android.util.Log;

import org.w3c.dom.Node;

import java.util.*;

import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.err.WebUpdatingException;
import ch.prokopovi.struct.Master.*;
import ch.prokopovi.struct.ProviderRateBuilder;
import ch.prokopovi.struct.ProviderRequirements;

public class NbrbProvider extends AbstractProvider {
	private static final String LOG_TAG = "NbrbProvider";

	private static final String DATA_URL_FORMAT = "http://www.nbrb.by/Services/XmlExRatesDyn.aspx?curId=%1$s&fromDate=%2$tm/%2$td/%2$tY&toDate=%3$tm/%3$td/%3$tY";
	private static final String DATE_XPATH_FORMAT = "//Record[@Date='%1$tm/%1$td/%1$tY']/Rate/text()";

	private enum NbrbCurrencyCode implements CurrencyCodable {
		USD("145", CurrencyCode.USD), //
		EUR("19", CurrencyCode.EUR), //
		RUR("190", CurrencyCode.RUB), //

		PLN("219", CurrencyCode.PLN), //
		UAH("224", CurrencyCode.UAH), //
		GBP("143", CurrencyCode.GBP), //
		JPY("277", CurrencyCode.JPY), //
		;

		private final String code;
		private final CurrencyCode currencyCode;

		private NbrbCurrencyCode(String code, CurrencyCode currencyCode) {
			this.code = code;
			this.currencyCode = currencyCode;
		}

		public static NbrbCurrencyCode get(CurrencyCode currencyCode) {
			for (NbrbCurrencyCode nbrbCurrencyCode : values()) {
				if (nbrbCurrencyCode.getCurrencyCode().equals(currencyCode)) {
					return nbrbCurrencyCode;
				}
			}

			return null;
		}

		@Override
		public CurrencyCode getCurrencyCode() {
			return this.currencyCode;
		}

		public String getCode() {
			return this.code;
		}
	}

	/**
	 * create service request line (period starting two days before today and
	 * ending tomorrow)
	 * 
	 * @return service request line
	 */
	private static String buildUrlString(CurrencyCode code, Date begin, Date end) {
		return String.format(DATA_URL_FORMAT, NbrbCurrencyCode.get(code)
				.getCode(), begin, end);
	}

	/**
	 * make xpath like //Record[@Date='07/28/2012']/Rate/text() with
	 * customizable date
	 * 
	 * @param date
	 * @return
	 */
	private static String buildDateXpath(Date date) {
		return String.format(DATE_XPATH_FORMAT, date);
	}

	private static Double extractValue(Date date, Node node)
			throws Exception {

		String xpath = buildDateXpath(date);

		return extractDotValue(node, xpath);
	}

	@Override
	protected List<ProviderRate> requestRates(
			ProviderRequirements requirements, Date now,
			ProviderRateBuilder builder) throws WebUpdatingException {

		Calendar cal = Calendar.getInstance();
		cal.setTime(now);

		cal.add(Calendar.DATE, 1);
		Date tomorrow = cal.getTime();

		List<ProviderRate> res = new ArrayList<>();

		Set<CurrencyCode> currencyCodes = requirements.getCurrencyCodes();
		for (CurrencyCode currencyCode : currencyCodes) {
			String location = buildUrlString(currencyCode, now, tomorrow);

			try {

				Node root = ProviderUtils.readFrom(location);

				Double tomorrowValue = extractValue(tomorrow, root);
				Double nowValue = extractValue(now, root);

				res.add(builder.build(OperationType.BUY, currencyCode,
						tomorrowValue));

				if (nowValue != null) {
					res.add(builder.build(OperationType.SELL, currencyCode,
							nowValue));
				}// now value should not be null
			} catch (Exception e) {
				Log.e(LOG_TAG, "error loading/parsing: " + currencyCode, e);
				continue;
			}
		}

		return res;
	}

	@Override
	protected ProviderCode getProviderCode() {
		return ProviderCode.NBRB;
	}

	@Override
	CurrencyCodable[] getCurrencyCodables(RateType rt) {

		return RateType.CASH.equals(rt) ? NbrbCurrencyCode.values()
				: new NbrbCurrencyCode[] {};
	}
}
