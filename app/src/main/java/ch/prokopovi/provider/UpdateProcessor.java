package ch.prokopovi.provider;

import java.util.Collections;
import java.util.List;

import android.util.Log;
import ch.prokopovi.api.provider.Provider;
import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.err.WebUpdatingException;
import ch.prokopovi.struct.Master.ProviderCode;
import ch.prokopovi.struct.ProviderRequirements;

public class UpdateProcessor {

	private static final String LOG_TAG = "UpdateProcessor";

	/**
	 * load values by requirements
	 * 
	 * @param requirements
	 *            data requirements
	 * @return
	 * @throws WebUpdatingException
	 */
	public static List<ProviderRate> process(ProviderRequirements requirements)
			throws WebUpdatingException {

		if (requirements == null || requirements.getProviderCode() == null) {
			Log.e(LOG_TAG, "requirements provider should not be null");
			return Collections.emptyList();
		}

		ProviderCode providerCode = requirements.getProviderCode();
		Provider provider = ProviderFactory.getProvider(providerCode);
		if (provider == null) {
			Log.e(LOG_TAG, "can't create provider: " + providerCode);
			return Collections.emptyList();
		}

		// save to db
		List<ProviderRate> list = provider.update(requirements);
		Log.d(LOG_TAG, ((list != null) ? list.size() : null)
				+ " records loaded from web");

		return list;
	}
}
