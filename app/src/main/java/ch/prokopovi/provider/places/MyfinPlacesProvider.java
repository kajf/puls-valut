package ch.prokopovi.provider.places;

import android.util.Log;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;
import java.util.Map.Entry;

import ch.prokopovi.Util;
import ch.prokopovi.api.struct.BestRatesRecord;
import ch.prokopovi.exported.PureConst.MyfinRegion;
import ch.prokopovi.provider.ProviderUtils;
import ch.prokopovi.struct.Master.*;
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
			Node root = ProviderUtils.readFrom(strUrl);

			long loadPoint = new Date().getTime();
			Log.d(LOG_TAG, "load time spent: " + (loadPoint - start));

			if (root == null)
				return Collections.emptyList();

			final List<Entry<Long, BestRatesRecord>> res = new ArrayList<>();

			NodeList placeNodes = root.getFirstChild().getChildNodes();
			for (int i = 0; i < placeNodes.getLength(); i++) {
				Node placeNode = placeNodes.item(i);

				// Log.d(LOG_TAG, "string " + tmp.getText());

				List<Entry<Long, BestRatesRecord>> placeRecords = parsePlace(placeNode);

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

	static List<Entry<Long, BestRatesRecord>> parsePlace(Node placeNode) {

		try {
			List<Entry<Long, BestRatesRecord>> placeRecords = new ArrayList<>();

			Node idNode = findChildByName(placeNode, "filialid");
			if (idNode == null)
				return placeRecords;

			String strId = idNode.getTextContent().toString();
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
			return Collections.emptyList();
		}
	}

	private static List<BestRatesRecord> parseRates(Node placeNode) {

        Set<String> vauesToSkip = new HashSet<>(Arrays.asList("-", "1"));

		List<BestRatesRecord> rates = new ArrayList<>();

		for (MyfinCurrency mfc : MyfinCurrency.values()) {
			for (OperationType ot : OperationType.values()) {

				String tag = mfc.getCode() + "_"
						+ ot.name().toLowerCase(Locale.US);

				Node placeTag = findChildByName(placeNode, tag);
				if (placeTag == null)
					continue;

				String strValue = placeTag.getTextContent().toString();

                if (vauesToSkip.contains(strValue))
                    continue;

				try {

                    double val = Util.parseDotDouble(strValue);

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

	private static Node findChildByName(Node parent, String childName) {
		NodeList childNodes = parent.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);

			if (child.getNodeName().equals(childName)) {
				return child;
			}
		}

		return null;
	}
}
