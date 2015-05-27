package ch.prokopovi.provider;

import android.util.Log;

import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.util.*;

import javax.xml.parsers.DocumentBuilderFactory;

import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.err.WebUpdatingException;
import ch.prokopovi.struct.Master.*;
import ch.prokopovi.struct.ProviderRateBuilder;
import ch.prokopovi.struct.ProviderRequirements;

public class ByAlfaProvider extends AbstractProvider {

	private static final String DATA_URL = "https://www.alfabank.by/personal/currency/rates.rss";

	private static final String XPATH_BUY_FMT = "//td[@id='%1$s%2$sBy']/text()";
	private static final String XPATH_SELL_FMT = "//td[@id='%1$s%2$sSell']/text()";

	private static final String LOG_TAG = "ByAlfaProvider";

	private enum ByAlfaRateType {
		CASH("cash", RateType.CASH), CARD("noncash", RateType.CARD);

		private final String code;
		private final RateType rateType;

		ByAlfaRateType(String code, RateType rateType) {
			this.code = code;
			this.rateType = rateType;
		}

		public static ByAlfaRateType get(RateType rateType) {
			for (ByAlfaRateType byAlfaRateType : values()) {
				if (byAlfaRateType.rateType.equals(rateType)) {
					return byAlfaRateType;
				}
			}

			return null;
		}
	}

	private enum ByAlfaCurrencyCode implements CurrencyCodable {
		USD("Доллар США", CurrencyCode.USD), //
		EUR("Евро", CurrencyCode.EUR), //
		RUB("Российский рубль", CurrencyCode.RUR), //
		;

		private final String code;
		private final CurrencyCode currencyCode;

		private ByAlfaCurrencyCode(String code, CurrencyCode currencyCode) {
			this.code = code;
			this.currencyCode = currencyCode;
		}

		public static ByAlfaCurrencyCode get(CurrencyCode сс) {
			for (ByAlfaCurrencyCode brCurrCode : values()) {
				if (brCurrCode.currencyCode.equals(сс)) {
					return brCurrCode;
				}
			}

			return null;
		}

		@Override
		public CurrencyCode getCurrencyCode() {
			return this.currencyCode;
		}
	}

	@Override
	protected List<ProviderRate> requestRates(
			ProviderRequirements requirements, Date now,
			ProviderRateBuilder builder) throws WebUpdatingException {

		RateType rateType = requirements.getRateType();

		ByAlfaRateType baRateType = ByAlfaRateType.get(rateType);

		if (baRateType == null) {
			Log.e(LOG_TAG, "no rate type for " + rateType + " in provider");
			throw new WebUpdatingException();
		}

		Set<CurrencyCode> currencyCodes = requirements.getCurrencyCodes();

		List<ProviderRate> res = new ArrayList<>();
		try {
			String cdataDescription = ProviderUtils.readFrom(DATA_URL, "//rss/channel/item/description");

			if (cdataDescription == null)
				return res;

			Element root = DocumentBuilderFactory
					.newInstance()
					.newDocumentBuilder()
					.parse(new ByteArrayInputStream(cdataDescription.getBytes()))
					.getDocumentElement();

			if (root == null) return res;

			for (CurrencyCode currencyCode : currencyCodes) {

				ByAlfaCurrencyCode baCode = ByAlfaCurrencyCode.get(currencyCode);

				String buyXpath = String.format(Locale.US, XPATH_BUY_FMT,
						baCode.code, baRateType.code);
				Double buy = extractDotValue(root, buyXpath);

				String sellXpath = String.format(Locale.US, XPATH_SELL_FMT,
						baCode.code, baRateType.code);
				Double sell = extractDotValue(root, sellXpath);

				List<ProviderRate> tmpRates = assembleProviderRates(
						builder, currencyCode, buy, sell);

				res.addAll(tmpRates);
			}

		} catch (Exception e) {
			Log.e(LOG_TAG, "error on update");
			throw new WebUpdatingException(e);
		}

		return res;
	}

	@Override
	protected ProviderCode getProviderCode() {
		return ProviderCode.BY_ALFA;
	}

	@Override
	public RateType[] getSupportedRateTypes() {
		return new RateType[] { RateType.CASH, RateType.CARD };
	}

	@Override
	CurrencyCodable[] getCurrencyCodables(RateType rt) {

		return RateType.CASH.equals(rt) ? ByAlfaCurrencyCode.values()
				: new ByAlfaCurrencyCode[] { ByAlfaCurrencyCode.USD,
						ByAlfaCurrencyCode.EUR };
	}
}
