package ch.prokopovi.provider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.htmlcleaner.TagNode;

import android.util.Log;
import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.err.WebUpdatingException;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.ProviderCode;
import ch.prokopovi.struct.Master.RateType;
import ch.prokopovi.struct.ProviderRateBuilder;
import ch.prokopovi.struct.ProviderRequirements;

public class MmProvider extends AbstractProvider {

	private static final String LOG_TAG = "MmProvider";

	// 25 usd mm
	private static final String URL_FORMAT = "http://www.mmbank.by/swf/amcharts/data.php?town=%1$s&currency=%2$s&type=%3$s&from=%4$td.%4$tm.%4$tY&to=%5$td.%5$tm.%5$tY";

	private enum MmCurrencyCode implements CurrencyCodable {
		USD("usd", CurrencyCode.USD), EUR("eur", CurrencyCode.EUR), RUB("rur",
				CurrencyCode.RUR);

		private final String code;
		private final CurrencyCode currencyCode;

		private MmCurrencyCode(String code, CurrencyCode currencyCode) {
			this.code = code;
			this.currencyCode = currencyCode;
		}

		public String getCode() {
			return this.code;
		}

		@Override
		public CurrencyCode getCurrencyCode() {
			return this.currencyCode;
		}

		public static MmCurrencyCode get(CurrencyCode currencyCode) {
			for (MmCurrencyCode mmCurrencyCode : values()) {
				if (mmCurrencyCode.getCurrencyCode().equals(currencyCode)) {
					return mmCurrencyCode;
				}
			}

			return null;
		}
	}

	private enum MmRateType {
		CASH("mm", RateType.CASH), CARD("cards", RateType.CARD);

		private final String code;
		private final RateType rateType;

		private MmRateType(String code, RateType rateType) {
			this.code = code;
			this.rateType = rateType;
		}

		public String getCode() {
			return this.code;
		}

		public RateType getRateType() {
			return this.rateType;
		}

		public static MmRateType get(RateType rateType) {
			for (MmRateType mmRateType : values()) {
				if (mmRateType.getRateType().equals(rateType)) {
					return mmRateType;
				}
			}

			return null;
		}
	}

	@Override
	protected List<ProviderRate> requestRates(
			ProviderRequirements requirements, Date now,
			ProviderRateBuilder builder) throws WebUpdatingException {

		RateType rateType = requirements.getRateType();
		MmRateType mmRateType = MmRateType.get(rateType);

		List<ProviderRate> res = new ArrayList<ProviderRate>();

		Set<CurrencyCode> currencyCodes = requirements.getCurrencyCodes();
		for (CurrencyCode currencyCode : currencyCodes) {
			MmCurrencyCode mmCurrencyCode = MmCurrencyCode.get(currencyCode);

			try {
				TagNode node = null;

				Calendar cal = Calendar.getInstance();
				cal.setTime(now);

				cal.add(Calendar.DATE, 1);
				Date tomorrow = cal.getTime();

				// end period date is not included in result
				String location = String.format(URL_FORMAT, 25,
						mmCurrencyCode.getCode(), mmRateType.getCode(), now,
						tomorrow);

				TagNode tmpNode = ProviderUtils.load(location);

				Object[] dateNodes = tmpNode.evaluateXPath("//graph/value");
				if (dateNodes != null && dateNodes.length > 1) {
					Log.d(LOG_TAG, "appropreate rates are found");
					node = tmpNode;
				}

				if (node == null) {
					Log.e(LOG_TAG, "can't found appropreate rates");
					throw new WebUpdatingException();
				}

				double buy = extractValue(node,
						"//chart/graphs/graph[@gid='1']/value[last()]/text()",
						false);
				double sell = extractValue(node,
						"//chart/graphs/graph[@gid='2']/value[last()]/text()",
						false);

				List<ProviderRate> tmpRates = assembleProviderRates(builder,
						currencyCode, buy, sell);

				res.addAll(tmpRates);
			} catch (Exception e) {
				Log.e(LOG_TAG, "error on mm data loading", e);
				throw new WebUpdatingException();
			}
		}

		return res;
	}

	@Override
	protected ProviderCode getProviderCode() {
		return ProviderCode.MM;
	}

	@Override
	public RateType[] getSupportedRateTypes() {
		return new RateType[] { RateType.CASH, RateType.CARD };
	}

	@Override
	CurrencyCodable[] getCurrencyCodables(RateType rt) {

		return RateType.CASH.equals(rt) ? MmCurrencyCode.values()
				: new MmCurrencyCode[] { MmCurrencyCode.USD, MmCurrencyCode.EUR };
	}
}
