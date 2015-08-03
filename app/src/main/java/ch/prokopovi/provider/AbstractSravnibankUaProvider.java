package ch.prokopovi.provider;

import android.util.Log;

import org.w3c.dom.Node;

import java.util.*;

import javax.xml.xpath.XPathExpression;

import ch.prokopovi.Util;
import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.err.WebUpdatingException;
import ch.prokopovi.struct.Master.*;
import ch.prokopovi.struct.ProviderRateBuilder;
import ch.prokopovi.struct.ProviderRequirements;

public abstract class AbstractSravnibankUaProvider extends AbstractProvider {

	private static final String LOG_TAG = "AbSravnibankUaProvider";

	private static final String DATA_URL_FORMAT = "http://www.sravnibank.com.ua/kursy-valut/cash_xml/?id=chprokopovipl-023711&currency=%1$s&bank=%2$s&date=%3$tY-%3$tm-%3$td";

	private static final String BUY_RATE_XPATH = "//Bank/Buy/text()";
	private static final String SALE_RATE_XPATH = "//Bank/Sale/text()";

	private enum SravnibankUaCurrencyCode implements CurrencyCodable {
		USD(CurrencyCode.USD), EUR(CurrencyCode.EUR), RUB(CurrencyCode.RUR), GBP(
				CurrencyCode.GBP), CHF(CurrencyCode.CHF);

		private final CurrencyCode currencyCode;

		SravnibankUaCurrencyCode(CurrencyCode currencyCode) {
			this.currencyCode = currencyCode;
		}

		public static SravnibankUaCurrencyCode get(CurrencyCode currencyCode) {
			for (SravnibankUaCurrencyCode tmpCurrencyCode : values()) {
				if (tmpCurrencyCode.getCurrencyCode().equals(currencyCode)) {
					return tmpCurrencyCode;
				}
			}

			return null;
		}

		@Override
		public CurrencyCode getCurrencyCode() {
			return this.currencyCode;
		}
	}

	protected abstract String getBankCode();

	@Override
	protected abstract ProviderCode getProviderCode();

	/**
	 * create service request line
	 * 
	 * @param currency
	 * @param bankCode
	 * @param date
	 * @return service request line
	 */
	private static String buildUrlString(SravnibankUaCurrencyCode currency,
			String bankCode, Date date) {
		return String.format(DATA_URL_FORMAT, currency.name(), bankCode, date);
	}

	@Override
	protected List<ProviderRate> requestRates(
			ProviderRequirements requirements, Date now,
			ProviderRateBuilder builder) throws WebUpdatingException {

		List<ProviderRate> res = new ArrayList<>();

		Set<CurrencyCode> currencyCodes = requirements.getCurrencyCodes();
		try {

			XPathExpression buyXpath = ProviderUtils.newXpath().compile(BUY_RATE_XPATH);
			XPathExpression saleXpath = ProviderUtils.newXpath().compile(SALE_RATE_XPATH);


			for (CurrencyCode currencyCode : currencyCodes) {
				SravnibankUaCurrencyCode sbCurr = SravnibankUaCurrencyCode
						.get(currencyCode);
				String location = buildUrlString(sbCurr, getBankCode(), now);

				Node root = ProviderUtils.readFrom(location);

				String strBuy = buyXpath.evaluate(root);
				if (!Util.isBlank(strBuy)) {
					double val = Util.parseDotDouble(strBuy);
					res.add(builder.build(OperationType.BUY, currencyCode, val));
				}

				String strSell = saleXpath.evaluate(root);
				if (!Util.isBlank(strSell)) {
					double val = Util.parseDotDouble(strSell);
					res.add(builder
							.build(OperationType.SELL, currencyCode, val));
				}
			}

		} catch (Exception e) {
			throw new WebUpdatingException(getProviderCode() + " error parsing", e);
		}

		return res;
	}

	@Override
	CurrencyCodable[] getCurrencyCodables(RateType rt) {

		return RateType.CASH.equals(rt) ? SravnibankUaCurrencyCode.values()
				: new SravnibankUaCurrencyCode[] {};
	}
}
