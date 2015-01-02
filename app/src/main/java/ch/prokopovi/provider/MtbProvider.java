package ch.prokopovi.provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

public class MtbProvider extends AbstractProvider {

	private static final String LOG_TAG = "MtbProvider";

	private static final String URL_FORMAT = "http://www.mtbank.by/currxml.php?d=%1$td.%1$tm.%1$tY";

	private enum MtbCurrencyCode implements CurrencyCodable {
		USD(CurrencyCode.USD), EUR(CurrencyCode.EUR), RUB(CurrencyCode.RUR);

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

		TagNode node = ProviderUtils.load(location);

		MtbCurrencyCode[] mtbCurrencyCodes = MtbCurrencyCode.values();
		for (MtbCurrencyCode mtbCurrencyCode : mtbCurrencyCodes) {
			String prefix = String.format(
					"//currency[codeTo/text()='%1$s'][code/text()='BYR']",
					mtbCurrencyCode.name());

			double purchase = 0.0;
			double sell = 0.0;

			Object[] purchases = node.evaluateXPath(prefix + "/purchase");
			if (purchases != null && purchases.length > 0) {
				TagNode tmp = (TagNode) purchases[0];
				purchase = Util.parseCommaDouble(tmp.getText().toString());
				Log.d(LOG_TAG, "purchase " + purchase);
			}

			Object[] sales = node.evaluateXPath(prefix + "/sale");
			if (sales != null && sales.length > 0) {
				TagNode tmp = (TagNode) sales[0];
				sell = Util.parseCommaDouble(tmp.getText().toString());
				Log.d(LOG_TAG, "sell " + sell);
			}

			// !!! purchase and sell are shifted in xml so purchase = sell, sale
			// = buy

			ProviderRate buyRate = builder.build(OperationType.BUY,
					mtbCurrencyCode.getCurrencyCode(), sell);
			Log.d(LOG_TAG, "buyRate " + buyRate);
			res.add(buyRate);

			ProviderRate sellRate = builder.build(OperationType.SELL,
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
