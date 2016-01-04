package ch.prokopovi.provider;

import android.util.Log;

import org.w3c.dom.Node;

import java.util.*;

import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.err.WebUpdatingException;
import ch.prokopovi.struct.Master.*;
import ch.prokopovi.struct.ProviderRateBuilder;
import ch.prokopovi.struct.ProviderRequirements;

public class MtbProvider extends AbstractProvider {

	private static final String LOG_TAG = "MtbProvider";

	private static final String URL_FORMAT = "http://www.mtbank.by/currxml.php?d=%1$td.%1$tm.%1$tY";

	private enum MtbCurrencyCode implements CurrencyCodable {
		USD(CurrencyCode.USD), EUR(CurrencyCode.EUR), RUB(CurrencyCode.RUB);

		private final CurrencyCode currencyCode;

		private MtbCurrencyCode(CurrencyCode currencyCode) {
			this.currencyCode = currencyCode;
		}

		@Override
		public CurrencyCode getCurrencyCode() {
			return this.currencyCode;
		}
	}

	/**
	 * load rates data form web
	 * 
	 * @param date
	 *            rates data for
	 * 
	 * @return
	 * @throws Exception
	 */
	private List<ProviderRate> getRatesData(ProviderRateBuilder builder,
			Date date) throws Exception {

		List<ProviderRate> res = new ArrayList<ProviderRate>();

		Log.d(LOG_TAG, "getRatesData()");

		String location = String.format(URL_FORMAT, date);

		Node root = ProviderUtils.readFrom(location);

		MtbCurrencyCode[] mtbCurrencyCodes = MtbCurrencyCode.values();
		for (MtbCurrencyCode mtbCurrencyCode : mtbCurrencyCodes) {
			String prefix = String.format(
					"//currency[code/text()='%1$s'][codeTo/text()='BYR']",
					mtbCurrencyCode.name());

			Double purchase = extractDotValue(root, prefix + "/purchase", 0.0);
			Double sell = extractDotValue(root, prefix + "/sale", 0.0);

			// !!! purchase and sell are shifted in xml so purchase = sell, sale
			// = buy

			ProviderRate buyRate = builder.build(OperationType.SELL,
					mtbCurrencyCode.getCurrencyCode(), sell);
			Log.d(LOG_TAG, "buyRate " + buyRate);
			res.add(buyRate);

			ProviderRate sellRate = builder.build(OperationType.BUY,
					mtbCurrencyCode.getCurrencyCode(), purchase);
			Log.d(LOG_TAG, "sellRate " + sellRate);
			res.add(sellRate);
		}

		return res;
	}

	@Override
	protected List<ProviderRate> requestRates(
			ProviderRequirements requirements, Date now,
			ProviderRateBuilder builder) throws WebUpdatingException {

		List<ProviderRate> res = new ArrayList<ProviderRate>();

		try {
			res = getRatesData(builder, now);
			Log.d(LOG_TAG, "rates found for date " + now);
		} catch (Exception e) {
			Log.w(LOG_TAG, "rates not found for date " + now);
		}

		return res;
	}

	@Override
	protected ProviderCode getProviderCode() {
		return ProviderCode.MTB;
	}

	@Override
	CurrencyCodable[] getCurrencyCodables(RateType rt) {

		return RateType.CASH.equals(rt) ? MtbCurrencyCode.values()
				: new MtbCurrencyCode[] {};
	}
}
