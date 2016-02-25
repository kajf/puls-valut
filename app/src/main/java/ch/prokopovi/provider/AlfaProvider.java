package ch.prokopovi.provider;

import org.w3c.dom.*;

import java.util.*;

import ch.prokopovi.api.struct.*;
import ch.prokopovi.err.*;
import ch.prokopovi.struct.Master.*;
import ch.prokopovi.struct.*;

public class AlfaProvider extends AbstractProvider {

	private static final String LOG_TAG = "AlfaProvider";

	private static final String URL = "https://www.alfabank.ru/_/_currency.xml";

	private static final String XPATH_FMT = "//rates[@type='%1$s']//item[@currency-id='%2$s']";
	private static final String XPATH_SELL_FMT = XPATH_FMT + "/@value-selling";
	private static final String XPATH_BUY_FMT = XPATH_FMT + "/@value-buying";

	private enum AlfaCurrencyCode implements CurrencyCodable {
		USD("840", CurrencyCode.USD), EUR("978", CurrencyCode.EUR);

		private final String code;
		private final CurrencyCode currencyCode;

		private AlfaCurrencyCode(String code, CurrencyCode currencyCode) {
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

		public static AlfaCurrencyCode get(CurrencyCode currencyCode) {
			for (AlfaCurrencyCode tmpCurrencyCode : values()) {
				if (tmpCurrencyCode.getCurrencyCode().equals(currencyCode)) {
					return tmpCurrencyCode;
				}
			}

			return null;
		}
	}

	private enum AlfaRateType {
		CASH("cash", RateType.CASH), CARD("non-cash", RateType.CARD);

		private final String code;
		private final RateType rateType;

		AlfaRateType(String code, RateType rateType) {
			this.code = code;
			this.rateType = rateType;
		}

		public RateType getRateType() {
			return this.rateType;
		}

		public static AlfaRateType get(RateType rateType) {
			for (AlfaRateType tmpRateType : values()) {
				if (tmpRateType.getRateType().equals(rateType)) {
					return tmpRateType;
				}
			}

			return null;
		}
	}

	@Override
	protected ProviderCode getProviderCode() {
		return ProviderCode.ALFA;
	}

	@Override
	protected List<ProviderRate> requestRates(
			ProviderRequirements requirements, Date now,
			ProviderRateBuilder builder) throws WebUpdatingException {

		List<ProviderRate> res = new ArrayList<>();
		try {
			RateType rateType = requirements.getRateType();
			AlfaRateType alfaRateType = AlfaRateType.get(rateType);

			if (alfaRateType == null) return res;

			Node root = ProviderUtils.readFrom(URL);

			Set<CurrencyCode> currencyCodes = requirements.getCurrencyCodes();
			for (CurrencyCode currencyCode : currencyCodes) {
				AlfaCurrencyCode alfaCurrencyCode = AlfaCurrencyCode
						.get(currencyCode);

				if (alfaCurrencyCode == null) continue;

				String buyXpath = String.format(XPATH_BUY_FMT,
						alfaRateType.code, alfaCurrencyCode.getCode());
				Double buy = extractCommaValue(root, buyXpath);

				String sellXpath = String.format(XPATH_SELL_FMT,
						alfaRateType.code, alfaCurrencyCode.getCode());
				Double sell = extractCommaValue(root, sellXpath);

				List<ProviderRate> tmpRates = assembleProviderRates(builder,
						currencyCode, buy, sell);

				res.addAll(tmpRates);

			}
		} catch (Exception e) {
			throw new WebUpdatingException("error on data loading", e);
		}
		return res;
	}

	@Override
	public RateType[] getSupportedRateTypes() {
		return new RateType[] { RateType.CASH, RateType.CARD };
	}

	@Override
	CurrencyCodable[] getCurrencyCodables(RateType rt) {
		return AlfaCurrencyCode.values();
	}
}
