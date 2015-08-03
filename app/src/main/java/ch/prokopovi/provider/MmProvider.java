package ch.prokopovi.provider;

import android.util.Log;

import org.w3c.dom.Node;

import java.util.*;

import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.err.WebUpdatingException;
import ch.prokopovi.struct.Master.*;
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

		MmRateType(String code, RateType rateType) {
			this.code = code;
			this.rateType = rateType;
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

		List<ProviderRate> res = new ArrayList<>();

		RateType rateType = requirements.getRateType();
		MmRateType mmRateType = MmRateType.get(rateType);

		if (mmRateType == null) return res;

		// TODO move xpath creation here
		Set<CurrencyCode> currencyCodes = requirements.getCurrencyCodes();
		for (CurrencyCode currencyCode : currencyCodes) {
			MmCurrencyCode mmCurrencyCode = MmCurrencyCode.get(currencyCode);

			try {
				Calendar cal = Calendar.getInstance();
				cal.setTime(now);

				cal.add(Calendar.DATE, 1);
				Date tomorrow = cal.getTime();

				// end period date is not included in result
				String location = String.format(URL_FORMAT, 25,
						mmCurrencyCode.getCode(), mmRateType.code, now,
						tomorrow);

				Node root = ProviderUtils.readFrom(location);

				String prefix = "//chart/graphs/graph[@gid='";
				String postfix = "']/value[last()]/text()";

				Double buy = extractDotValue(root, prefix + "1" + postfix);
				Double sell = extractDotValue(root, prefix + "2" + postfix);

				List<ProviderRate> tmpRates = assembleProviderRates(builder,
						currencyCode, buy, sell);

				res.addAll(tmpRates);
			} catch (Exception e) {
				throw new WebUpdatingException("error on mm data loading", e);
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
