package ch.prokopovi.provider;

import ch.prokopovi.api.provider.Provider;
import ch.prokopovi.struct.Master.ProviderCode;

public class ProviderFactory {

	/**
	 * retrieve provider by code. providers are static and lazy-initialized
	 * 
	 * @param code
	 *            provider code
	 * 
	 * @return
	 */
	public static Provider getProvider(ProviderCode code) {

		if (code == null)
			throw new IllegalArgumentException(
					"can't create provider by null code");

		try {
			return (Provider) code.getClazz().newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * don't create factories
	 * 
	 */
	private ProviderFactory() {
		super();
	}
}
