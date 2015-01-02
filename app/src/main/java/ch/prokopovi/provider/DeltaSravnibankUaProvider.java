package ch.prokopovi.provider;

import ch.prokopovi.struct.Master.ProviderCode;

public class DeltaSravnibankUaProvider extends AbstractSravnibankUaProvider {

	@Override
	protected String getBankCode() {
		return "delita-bank";
	}

	@Override
	protected ProviderCode getProviderCode() {
		return ProviderCode.DELTA_SB_UA;
	}

}
