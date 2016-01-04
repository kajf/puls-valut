package ch.prokopovi.provider;

import android.util.Log;

import org.w3c.dom.Node;

import java.util.*;

import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.err.WebUpdatingException;
import ch.prokopovi.struct.Master.*;
import ch.prokopovi.struct.ProviderRateBuilder;
import ch.prokopovi.struct.ProviderRequirements;

public class PriorProvider extends AbstractProvider {

	private enum PriorCurrencyCode implements CurrencyCodable {
		USD(CurrencyCode.USD), EUR(CurrencyCode.EUR), RUB(CurrencyCode.RUB);

		private final CurrencyCode code;

		private PriorCurrencyCode(CurrencyCode code) {
			this.code = code;
		}

		private static PriorCurrencyCode get(CurrencyCode code) {

			for (PriorCurrencyCode pcc : values()) {
				if (pcc.getCurrencyCode().equals(code)) {
					return pcc;
				}
			}

			return null;
		}

		@Override
		public CurrencyCode getCurrencyCode() {
			return this.code;
		}
	}

	private static final String XPATH_PREFIX = "//R_DATE/LIST_E_CHANNEL/E_CHANNEL/LIST_RATE";
	private static final String XPATH_RATE_VAL_FMT = "/RATE[ISO='%1$s']/%2$s/text()";

	private static final String LOG_TAG = "PriorProvider";

	private static final String DATA_URL_FORMAT = "http://www.priorbank.by/CurratesExportXml.axd?"
			+ "channel=%1$s";

	/**
	 * create service request line
	 * 
	 * @param rateType
	 * 
	 * @return service request line
	 */
	private static String buildUrlString(RateType rateType) {

		String type = "3"; // cash [default]
		if (RateType.CARD.equals(rateType)) {
			type = "9";
		}

		return String.format(DATA_URL_FORMAT, type);
	}

	@Override
	protected List<ProviderRate> requestRates(
			ProviderRequirements requirements, Date now,
			ProviderRateBuilder builder) throws WebUpdatingException {

		List<ProviderRate> res = new ArrayList<>();

		try {
			String location = buildUrlString(requirements.getRateType());

			Node root = ProviderUtils.readFrom(location);
			if (root == null) {
				Log.e(LOG_TAG, "can't find appropriate rates");
				throw new WebUpdatingException();
			}

			Set<CurrencyCode> currencyCodes = requirements.getCurrencyCodes();
			for (CurrencyCode currencyCode : currencyCodes) {

				PriorCurrencyCode priorCurrencyCode = PriorCurrencyCode
						.get(currencyCode);

				String buyPath = XPATH_PREFIX + String.format(XPATH_RATE_VAL_FMT,
						priorCurrencyCode.name(), OperationType.BUY.name());
				Double buy = extractDotValue(root, buyPath);

				String sellPath = XPATH_PREFIX + String.format(XPATH_RATE_VAL_FMT,
						priorCurrencyCode.name(), OperationType.SELL.name());

				Double sell = extractDotValue(root, sellPath);

				List<ProviderRate> tmpRates = assembleProviderRates(builder,
						currencyCode, buy, sell);

				res.addAll(tmpRates);
			}

		} catch (Exception e) {
			throw new WebUpdatingException("error on prior data loading", e);
		}

		return res;
	}

	@Override
	protected ProviderCode getProviderCode() {
		return ProviderCode.PRIOR;
	}

	@Override
	public RateType[] getSupportedRateTypes() {
		return new RateType[] { RateType.CASH, RateType.CARD };
	}

	@Override
	CurrencyCodable[] getCurrencyCodables(RateType rt) {
		return PriorCurrencyCode.values();
	}
}
