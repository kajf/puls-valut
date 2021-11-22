package ch.prokopovi.provider;

import android.util.Log;

import org.w3c.dom.Node;

import java.util.*;

import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.struct.Master.*;
import ch.prokopovi.struct.ProviderRateBuilder;
import ch.prokopovi.struct.ProviderRequirements;

public class NbrbProvider extends AbstractProvider {
	private static final String LOG_TAG = "NbrbProvider";

	private static final String DATA_URL_FORMAT = "https://www.nbrb.by/Services/XmlExRates.aspx?onDate=%1$tm/%1$td/%1$tY";
	private static final String CURR_XPATH_FORMAT = "//Currency[CharCode='%s']/Rate/text()";

	private enum NbrbCurrencyCode implements CurrencyCodable {
		USD(CurrencyCode.USD), //
		EUR(CurrencyCode.EUR), //
		RUR("RUB", CurrencyCode.RUR), //

		PLN(CurrencyCode.PLN), //
		UAH(CurrencyCode.UAH), //
		GBP(CurrencyCode.GBP), //
		JPY(CurrencyCode.JPY), //
		;

		private final String code;
		private final CurrencyCode currencyCode;

		NbrbCurrencyCode(String code, CurrencyCode currencyCode) {
			this.code = code;
			this.currencyCode = currencyCode;
		}

		NbrbCurrencyCode(CurrencyCode currencyCode) {
			this.code = currencyCode.name();
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

	private static Double extractValue(CurrencyCode currencyCode, Node node)
			throws Exception {

		String code = NbrbCurrencyCode.get(currencyCode).getCode();

		// make xpath like //Currency[CharCode='USD']/Rate/text()
		String xpath = String.format(CURR_XPATH_FORMAT, code);

		return extractDotValue(node, xpath);
	}

	@Override
	protected List<ProviderRate> requestRates(
			ProviderRequirements requirements, Date now,
			ProviderRateBuilder builder) {

		Calendar cal = Calendar.getInstance();
		cal.setTime(now);

		cal.add(Calendar.DATE, 1);
		Date tomorrow = cal.getTime();

		List<ProviderRate> res = new ArrayList<>();

		Set<CurrencyCode> currencyCodes = requirements.getCurrencyCodes();
		String locationToday = String.format(DATA_URL_FORMAT, now);
		String locationTomorrow = String.format(DATA_URL_FORMAT, tomorrow);

		try {

			List<ProviderRate> todayRates = requestRates(locationToday, currencyCodes, OperationType.SELL, builder);
			res.addAll(todayRates);

			List<ProviderRate> tomorrowRates = requestRates(locationTomorrow, currencyCodes, OperationType.BUY, builder);
			res.addAll(tomorrowRates);


		} catch (Exception e) {
			Log.e(LOG_TAG, "error loading/parsing: "
					+ locationToday + ", "
					+ locationTomorrow + ", "
					+ currencyCodes, e);
			return res;
		}

		return res;
	}

	private List<ProviderRate> requestRates(
			String location,
			Set<CurrencyCode> currencyCodes,
			OperationType opType,
			ProviderRateBuilder builder) throws Exception {

		List<ProviderRate> res = new ArrayList<>();

		Node root = ProviderUtils.readFrom(location);

		for (CurrencyCode currencyCode : currencyCodes) {

			Double rateValue = extractValue(currencyCode, root);

			res.add(builder.build(opType, currencyCode,
					rateValue));

			if (rateValue != null) {
				res.add(builder.build(opType, currencyCode,
						rateValue));
			}// now value should not be null
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
