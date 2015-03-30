package ch.prokopovi.provider.places;

import android.util.Log;

import org.htmlcleaner.TagNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import ch.prokopovi.Util;
import ch.prokopovi.api.struct.BestRatesRecord;
import ch.prokopovi.exported.PureConst.MyfinRegion;
import ch.prokopovi.provider.ProviderUtils;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.OperationType;
import ch.prokopovi.struct.Master.Region;
import ch.prokopovi.struct.SimpleBestRatesRecord;

public class MyfinPlacesProvider extends AbstractPlacesProvider {

	private static final String LOG_TAG = "myfin.by provider";

	private static final String URL_FORMAT = "http://myfin.by/scripts/xml_new/work/banks_city_%1$s.xml";

	public enum MyfinCurrency {
		USD("usd", CurrencyCode.USD), //
		EUR("eur", CurrencyCode.EUR), //
		RUR("rur", CurrencyCode.RUR), //
		PLN("pln", CurrencyCode.PLN), //
		UAH("uah", CurrencyCode.UAH), //
		EUR_USD("eurusd", CurrencyCode.EUR_USD);

		private final String code;
		private final CurrencyCode currency;

		private MyfinCurrency(String code, CurrencyCode currency) {
			this.code = code;
			this.currency = currency;
		}

		public String getCode() {
			return this.code;
		}

		public CurrencyCode getCurrency() {
			return this.currency;
		}

	}

	@Override
	public boolean isSupported(Region region) {
		return MyfinRegion.get(region.getId()) != null;
	}

	@Override
	public boolean isSupported(CurrencyCode currency) {

		for (MyfinCurrency mfc : MyfinCurrency.values()) {
			if (mfc.getCurrency().equals(currency)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public List<Entry<Long, BestRatesRecord>> getPlaces(Region region) {
		Log.d(LOG_TAG, " --- loading places --- ");

		long start = new Date().getTime();

		MyfinRegion myfinRegion = MyfinRegion.get(region.getId());

		String strUrl = String.format(URL_FORMAT, myfinRegion.getId());

		try {
			TagNode node = ProviderUtils.load(strUrl);

			long loadPoint = new Date().getTime();
			Log.d(LOG_TAG, "load time spent: " + (loadPoint - start));

			Object[] nodes = node.evaluateXPath("//root/*");

			if (nodes != null) {
				final List<Entry<Long, BestRatesRecord>> res = new ArrayList<Entry<Long, BestRatesRecord>>();

				for (Object obj : nodes) {
					TagNode tmp = (TagNode) obj;

					// Log.d(LOG_TAG, "string " + tmp.getText());

					List<Entry<Long, BestRatesRecord>> placeRecords = parsePlace(tmp);

					res.addAll(placeRecords);
				}

				long parsePoint = new Date().getTime();
				Log.d(LOG_TAG, "parse time spent: " + (parsePoint - loadPoint));

				return res;
			}

		} catch (Exception e) {
			Log.d(LOG_TAG, "error during loading rates ", e);
		}

		return Collections.emptyList();
	}

	private static List<BestRatesRecord> parseRates(TagNode placeNode) {

		List<BestRatesRecord> rates = new ArrayList<BestRatesRecord>();

		for (MyfinCurrency mfc : MyfinCurrency.values()) {
			for (OperationType ot : OperationType.values()) {

				String tag = mfc.getCode() + "_"
						+ ot.name().toLowerCase(Locale.US);

				TagNode placeTag = placeNode.findElementByName(tag, false);

				try {
					double val = Util.parseDotDouble(placeTag.getText()
							.toString());

                    if (val < 0.0001) {
                        Log.w(LOG_TAG, "to small rate " + val);
                        continue;
                    }

                    SimpleBestRatesRecord record = new SimpleBestRatesRecord(
							mfc.getCurrency().getId(), ot.getId(), val);

					rates.add(record);
				} catch (Exception e) {
                    Log.w(LOG_TAG, "invalid rate "
                            + e.getMessage());
				}
			}
		}

		return rates;
	}

	static List<Entry<Long, BestRatesRecord>> parsePlace(TagNode placeNode) {

		try {
			List<Entry<Long, BestRatesRecord>> placeRecords = new ArrayList<Entry<Long, BestRatesRecord>>();

			TagNode idNode = placeNode.getElementsByName("filialid", false)[0];
			String strId = idNode.getText().toString();

			// String strId = placeNode.getName().substring(4);
			// Log.d(LOG_TAG, "strId " + strId);

			Long placeId = Long.valueOf(strId);

			List<BestRatesRecord> placeRates = parseRates(placeNode);
			for (BestRatesRecord record : placeRates) {

				Entry<Long, BestRatesRecord> entry = createImmutableEntry(
						placeId, record);

				placeRecords.add(entry);
			}

			return placeRecords;

		} catch (Exception e) {
			Log.d(LOG_TAG, "error on parse place. Skipped.", e);
			return null;
		}
	}
}
