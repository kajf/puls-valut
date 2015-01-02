package ch.prokopovi.provider;

import ch.prokopovi.struct.Master.ProviderCode;

public class PrivatSravnibankUaProvider extends AbstractSravnibankUaProvider {

	@Override
	protected String getBankCode() {
		return "privatbank";
	}

	@Override
	protected ProviderCode getProviderCode() {
		return ProviderCode.PRIVAT_SB_UA;
	}

}
