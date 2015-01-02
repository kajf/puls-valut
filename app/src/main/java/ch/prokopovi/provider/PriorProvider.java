package ch.prokopovi.provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.htmlcleaner.TagNode;

import android.util.Log;
import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.err.WebUpdatingException;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.OperationType;
import ch.prokopovi.struct.Master.ProviderCode;
import ch.prokopovi.struct.Master.RateType;
import ch.prokopovi.struct.ProviderRateBuilder;
import ch.prokopovi.struct.ProviderRequirements;

public class PriorProvider extends AbstractProvider {

	private enum PriorCurrencyCode implements CurrencyCodable {
		USD(CurrencyCode.USD), EUR(CurrencyCode.EUR), RUB(CurrencyCode.RUR);

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

	private static final String XPATH_RATE_VAL_FMT = "//RATE[ISO='%1$s']/%2$s/text()";

	private static final String LOG_TAG = "PriorProvider";

	private static final String DATA_URL_FORMAT = "http://www.priorbank.by/CurratesExportXml.axd?"
			+ "channel=%1$s"
			+ "&iso=%2$s"
			+ "&from=%3$td.%3$tm.%3$tY"
			+ "&to=%4$td.%4$tm.%4$tY";

	/**
	 * create service request line (period starting 'frame' days before today
	 * and ending in 'date')
	 * 
	 * @param rateType
	 * @param codes
	 *            currency codes to request
	 * @param date
	 *            end of period
	 * @param frame
	 *            number of days to look back from date
	 * 
	 * 
	 * @return service request line
	 */
	private static String buildUrlString(RateType rateType,
			Set<CurrencyCode> codes, Date date) {

		String currStr = "";
		Iterator<CurrencyCode> iterator = codes.iterator();
		while (iterator.hasNext()) {
			CurrencyCode code = iterator.next();

			PriorCurrencyCode priorCurrencyCode = PriorCurrencyCode.get(code);

			currStr += priorCurrencyCode.name();

			if (iterator.hasNext())
				currStr += ",";
		}

		String type = RateType.CASH.name();
		if (rateType != null) {
			type = rateType.getCode();
		}

		return String.format(DATA_URL_FORMAT, type, currStr, date, date);
	}

	@Override
	protected List<ProviderRate> requestRates(
			ProviderRequirements requirements, Date now,
			ProviderRateBuilder builder) throws WebUpdatingException {

		List<ProviderRate> res = new ArrayList<ProviderRate>();

		try {
			TagNode node = null;

			RateType rateType = requirements.getRateType();
			Set<CurrencyCode> currencyCodes = requirements.getCurrencyCodes();
			String location = buildUrlString(rateType, currencyCodes, now);

			TagNode tmpNode = ProviderUtils.load(location);

			Object[] dateNodes = tmpNode.evaluateXPath("//R_DATE");
			if (dateNodes != null && dateNodes.length > 0) {
				Log.d(LOG_TAG, "appropreate rates are found");
				node = tmpNode;
			}

			if (node == null) {
				Log.e(LOG_TAG, "can't found appropreate rates");
				throw new WebUpdatingException();
			}

			Iterator<CurrencyCode> iter = currencyCodes.iterator();
			while (iter.hasNext()) {
				CurrencyCode currencyCode = iter.next();
				PriorCurrencyCode priorCurrencyCode = PriorCurrencyCode
						.get(currencyCode);

				String buyPath = String.format(XPATH_RATE_VAL_FMT,
						priorCurrencyCode.name(), OperationType.BUY.name());
				double buy = extractValue(node, buyPath, false);

				String sellPath = String.format(XPATH_RATE_VAL_FMT,
						priorCurrencyCode.name(), OperationType.SELL.name());
				double sell = extractValue(node, sellPath, false);

				List<ProviderRate> tmpRates = assembleProviderRates(builder,
						currencyCode, buy, sell);

				res.addAll(tmpRates);
			}

		} catch (Exception e) {
			Log.e(LOG_TAG, "error on prior data loading", e);
			throw new WebUpdatingException();
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

		return RateType.CASH.equals(rt) ? PriorCurrencyCode.values()
				: new PriorCurrencyCode[] { PriorCurrencyCode.USD,
						PriorCurrencyCode.EUR };
	}
}
