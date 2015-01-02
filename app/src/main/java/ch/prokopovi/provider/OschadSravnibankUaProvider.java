package ch.prokopovi.provider;

import ch.prokopovi.struct.Master.ProviderCode;

public class OschadSravnibankUaProvider extends AbstractSravnibankUaProvider {

	@Override
	protected String getBankCode() {
		return "oschadbank";
	}

	@Override
	protected ProviderCode getProviderCode() {
		return ProviderCode.OSCHAD_SB_UA;
	}

}
