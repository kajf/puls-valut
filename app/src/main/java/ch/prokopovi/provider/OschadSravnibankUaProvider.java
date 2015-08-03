package ch.prokopovi.provider;

import ch.prokopovi.struct.*;
import ch.prokopovi.struct.Master.*;

import static ch.prokopovi.provider.AbstractSravnibankUaProvider.SravnibankUaCurrencyCode.*;

public class OschadSravnibankUaProvider extends AbstractSravnibankUaProvider {

	@Override
	protected String getBankCode() {
		return "oschadbank";
	}

	@Override
	protected ProviderCode getProviderCode() {
		return ProviderCode.OSCHAD_SB_UA;
	}

	@Override
	CurrencyCodable[] getCurrencyCodables(Master.RateType rt) {

		return Master.RateType.CASH.equals(rt) ? new SravnibankUaCurrencyCode[]{USD, EUR, RUB}
				: new SravnibankUaCurrencyCode[]{};
	}
}
