package ch.prokopovi.provider;

import org.w3c.dom.Node;

import java.util.*;

import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.err.WebUpdatingException;
import ch.prokopovi.struct.Master.*;
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

		NbuCurrencyCode(CurrencyCode currencyCode) {
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

		List<ProviderRate> res = new ArrayList<>();
		String location = buildUrlString(now);

		try {
			Node root = ProviderUtils.readFrom(location);

			Set<CurrencyCode> currencyCodes = requirements.getCurrencyCodes();
			for (CurrencyCode currencyCode : currencyCodes) {


				NbuCurrencyCode nbuCurrencyCode = NbuCurrencyCode
						.get(currencyCode);

				String rateXpath = buildCurrencyXpath(nbuCurrencyCode);
				Double rate = extractDotValue(root, rateXpath);

				if (rate != null) {
					res.add(builder.build(OperationType.BUY, currencyCode, rate));

					String changeXpath = buildChangeXpath(nbuCurrencyCode);
					Double change = extractDotValue(root, changeXpath);

					if (change != null) {

						res.add(builder.build(OperationType.SELL, currencyCode,
								rate + change));
					}
				}
			}

		} catch (Exception e) {
			throw new WebUpdatingException("error parsing", e);
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
