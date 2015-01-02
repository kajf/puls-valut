package ch.prokopovi.provider.places;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import ch.prokopovi.api.struct.BestRatesRecord;
import ch.prokopovi.exported.PureConst;
import ch.prokopovi.exported.PureConst.FinanceUaRegion;
import ch.prokopovi.provider.ProviderUtils;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.OperationType;
import ch.prokopovi.struct.Master.Region;
import ch.prokopovi.struct.SimpleBestRatesRecord;

class FinanceUaPlacesProvider extends AbstractPlacesProvider {

	private static final String LOG_TAG = "finance.ua provider";

	private static final String URL = "http://resources.finance.ua/ru/public/currency-cash.json";

	public enum FinanceUaCurrency {
		USD(CurrencyCode.USD), //
		EUR(CurrencyCode.EUR), //
		RUB(CurrencyCode.RUR), //
		CHF(CurrencyCode.CHF), //
		GBP(CurrencyCode.GBP), //
		PLN(CurrencyCode.PLN), //
		;

		private final CurrencyCode currency;

		private FinanceUaCurrency(CurrencyCode currency) {
			this.currency = currency;
		}

	}

	@Override
	public boolean isSupported(Region region) {
		return FinanceUaRegion.get(region.getId()) != null;
	}

	@Override
	public boolean isSupported(CurrencyCode currency) {
		for (FinanceUaCurrency c : FinanceUaCurrency.values()) {
			if (c.currency.equals(currency)) {
				return true;
			}
		}

		return false;
	}

	private static List<BestRatesRecord> parseRates(JSONObject jsonCurrencies) {
		List<BestRatesRecord> rates = new ArrayList<BestRatesRecord>();

		for (FinanceUaCurrency c : FinanceUaCurrency.values()) {

			try {
				JSONObject jsonCurrency = jsonCurrencies
						.getJSONObject(c.name());

				double bid = jsonCurrency.getDouble("bid");
				SimpleBestRatesRecord buyRec = new SimpleBestRatesRecord(
						c.currency.getId(), OperationType.BUY.getId(), bid);
				rates.add(buyRec);

				double ask = jsonCurrency.getDouble("ask");
				SimpleBestRatesRecord sellRec = new SimpleBestRatesRecord(
						c.currency.getId(), OperationType.SELL.getId(), ask);
				rates.add(sellRec);

			} catch (JSONException e) {
				Log.d(LOG_TAG, "error parsing currency: " + e.getMessage());
			}
		}

		return rates;
	}

	private static List<Entry<Long, BestRatesRecord>> parsePlace(
			JSONObject jsonOrg) {

		List<Entry<Long, BestRatesRecord>> placeRecords = new ArrayList<Entry<Long, BestRatesRecord>>();

		try {

			// {
			// "id" : "7oiylpmiow8iy1smcr9",
			// "oldId" : 2066,
			// "orgType" : 1,
			// "title" : "CityCommerceBank  \u041a\u0433\u041e \u211669",
			// "regionId" : "ua,7oiylpmiow8iy1smace",
			// "cityId" : "7oiylpmiow8iy1smae3",
			// "phone" : "0522240649",
			// "address" :
			// "\u0443\u043b. \u0414\u0435\u043a\u0430\u0431\u0440\u0438\u0441\u0442\u043e\u0432, 4",
			// "link" :
			// "http:\/\/organizations.finance.ua\/ru\/info\/currency\/~\/7oiylpmiow8iy1smcr9\/cash",
			// "currencies" : {
			// "EUR" : {
			// "ask" : "10.9100",
			// "bid" : "10.7800"
			// },
			// "RUB" : {
			// "ask" : "0.2459",
			// "bid" : "0.2400"
			// },
			// "USD" : {
			// "ask" : "8.1380",
			// "bid" : "8.1100"
			// }
			// }
			// },

			String uid = jsonOrg.getString("id");
			long placeId = PureConst.financeUaPlaceIdTransform(uid);

			JSONObject jsonCurrencies = jsonOrg.getJSONObject("currencies");
			List<BestRatesRecord> placeRates = parseRates(jsonCurrencies);

			for (BestRatesRecord record : placeRates) {

				Entry<Long, BestRatesRecord> entry = createImmutableEntry(
						placeId, record);

				placeRecords.add(entry);
			}

		} catch (Exception e) {
			Log.d(LOG_TAG, "error parsing place", e);
		}

		return placeRecords;
	}

	@Override
	public List<Entry<Long, BestRatesRecord>> getPlaces(Region region) {

		Log.d(LOG_TAG, " --- loading places --- ");

		long start = new Date().getTime();

		try {
			String strJson = ProviderUtils.get(URL);

			long loadPoint = new Date().getTime();
			Log.d(LOG_TAG, "load time spent: " + (loadPoint - start));

			FinanceUaRegion financeUaRegion = FinanceUaRegion.get(region
					.getId());

			final List<Entry<Long, BestRatesRecord>> res = new ArrayList<Entry<Long, BestRatesRecord>>();

			JSONObject jsonRoot = new JSONObject(strJson);
			JSONArray jsonOrgs = jsonRoot.getJSONArray("organizations");
			for (int i = 0; i < jsonOrgs.length(); i++) {
				JSONObject jsonOrg = jsonOrgs.getJSONObject(i);

				String cityUid = jsonOrg.getString("cityId");
				if (!financeUaRegion.getUid().equals(cityUid)) {
					continue;
				} // another region

				List<Entry<Long, BestRatesRecord>> placeRecords = parsePlace(jsonOrg);

				res.addAll(placeRecords);
			}

			long parsePoint = new Date().getTime();
			Log.d(LOG_TAG, "parse time spent: " + (parsePoint - loadPoint));

			return res;

		} catch (Exception e) {
			Log.d(LOG_TAG, "error during loading rates ", e);
		}

		return Collections.emptyList();

	}

}
