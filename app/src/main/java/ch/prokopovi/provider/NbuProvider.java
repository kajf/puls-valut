package ch.prokopovi.provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.htmlcleaner.TagNode;

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

public class NbuProvider extends AbstractProvider {

	private static final String LOG_TAG = "NbuProvider";

	private static final String DATA_URL_FORMAT = "http://www.sravnibank.com.ua/kursy-valut/nbu_xml/?id=chprokopovipl-023711&date=%1$tY-%1$tm-%1$td";

	private static final String RATE_XPATH_FORMAT = "//Currency[Code/text()='%1$s']/Rate/text()";
	private static final String CHANGE_XPATH_FORMAT = "//Currency[Code/text()='%1$s']/ChangeAbs/text()";

	private enum NbuCurrencyCode implements CurrencyCodable {
		USD(CurrencyCode.USD), //
		EUR(CurrencyCode.EUR), //
		RUB(CurrencyCode.RUR), //

		PLN(CurrencyCode.PLN), //
		GBP(CurrencyCode.GBP), //
		JPY(CurrencyCode.JPY), //
		CHF(CurrencyCode.CHF), //
		;

		private final CurrencyCode currencyCode;

		private NbuCurrencyCode(CurrencyCode currencyCode) {
			this.currencyCode = currencyCode;
		}

		public static NbuCurrencyCode get(CurrencyCode currencyCode) {
			for (NbuCurrencyCode nbuCurrencyCode : values()) {
				if (nbuCurrencyCode.getCurrencyCode().equals(currencyCode)) {
					return nbuCurrencyCode;
				}
			}

			return null;
		}

		@Override
		public CurrencyCode getCurrencyCode() {
			return this.currencyCode;
		}
	}

	/**
	 * create service request line
	 * 
	 * @return service request line
	 */
	private static String buildUrlString(Date date) {
		return String.format(DATA_URL_FORMAT, date);
	}

	/**
	 * make xpath like //Currency[Code/text()='USD']/Rate/text() with
	 * customizable currency
	 * 
	 * @param currencyCode
	 * @return
	 */
	private static String buildCurrencyXpath(NbuCurrencyCode currencyCode) {
		return String.format(RATE_XPATH_FORMAT, currencyCode.name());
	}

	/**
	 * make xpath like //Currency[Code/text()='USD']/ChangeAbs/text() with
	 * customizable currency
	 * 
	 * @param currencyCode
	 * @return
	 */
	private static String buildChangeXpath(NbuCurrencyCode currencyCode) {
		return String.format(CHANGE_XPATH_FORMAT, currencyCode.name());
	}

	@Override
	protected List<ProviderRate> requestRates(
			ProviderRequirements requirements, Date now,
			ProviderRateBuilder builder) throws WebUpdatingException {

		String location = buildUrlString(now);
		TagNode tagNode = ProviderUtils.load(location);

		List<ProviderRate> res = new ArrayList<ProviderRate>();

		Set<CurrencyCode> currencyCodes = requirements.getCurrencyCodes();
		for (CurrencyCode currencyCode : currencyCodes) {

			try {
				NbuCurrencyCode nbuCurrencyCode = NbuCurrencyCode
						.get(currencyCode);

				String rateXpath = buildCurrencyXpath(nbuCurrencyCode);
				Object[] rateNodes = tagNode.evaluateXPath(rateXpath);

				String changeXpath = buildChangeXpath(nbuCurrencyCode);
				Object[] changeNodes = tagNode.evaluateXPath(changeXpath);

				if (rateNodes != null && rateNodes.length > 0) {
					double val = Util.parseDotDouble(rateNodes[0].toString());
					res.add(builder.build(OperationType.BUY, currencyCode, val));

					if (changeNodes != null && changeNodes.length > 0) {
						double change = Util.parseDotDouble(changeNodes[0]
								.toString());
						res.add(builder.build(OperationType.SELL, currencyCode,
								val + change));
					}
				}

				// Log.d(LOG_TAG, "rates are : " + Arrays.toString(res));
			} catch (Exception e) {
				Log.e(LOG_TAG, "error parsing", e);
				throw new WebUpdatingException();
			}
		}

		return res;
	}

	@Override
	protected ProviderCode getProviderCode() {
		return ProviderCode.NBU;
	}

	@Override
	CurrencyCodable[] getCurrencyCodables(RateType rt) {

		return RateType.CASH.equals(rt) ? NbuCurrencyCode.values()
				: new NbuCurrencyCode[] {};
	}
}
