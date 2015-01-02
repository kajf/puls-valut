package ch.prokopovi.provider;

import ch.prokopovi.struct.Master.ProviderCode;

public class UnicreditSravnibankUaProvider extends AbstractSravnibankUaProvider {

	@Override
	protected String getBankCode() {
		return "unicredit-bank-ukrsocbank1";
	}

	@Override
	protected ProviderCode getProviderCode() {
		return ProviderCode.UNICREDIT_SB_UA;
	}

}
