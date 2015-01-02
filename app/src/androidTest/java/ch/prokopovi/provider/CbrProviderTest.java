package ch.prokopovi.provider;

import java.util.List;
import java.util.Set;

import android.test.AndroidTestCase;
import android.util.Log;
import ch.prokopovi.api.provider.Provider;
import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.err.WebUpdatingException;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.ProviderCode;
import ch.prokopovi.struct.Master.RateType;
import ch.prokopovi.struct.ProviderRequirements;

public class CbrProviderTest extends AndroidTestCase {

	private static final String LOG_TAG = "CbrProviderTest";

	private Provider provider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.provider = new CbrProvider();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testUpdate() throws WebUpdatingException {

		RateType inRateType = RateType.CASH;

		ProviderRequirements requirements = new ProviderRequirements(ProviderCode.CBR, inRateType);
		Set<CurrencyCode> currencyCodes = requirements.getCurrencyCodes();
		currencyCodes.add(CurrencyCode.USD);
		currencyCodes.add(CurrencyCode.EUR);
		//currencyCodes.add(CurrencyCode.RUR);

		List<ProviderRate> list = this.provider.update(requirements);

		Log.d(LOG_TAG, "list.size(): "+list.size());

		assertNotNull("result is null", list);
		assertTrue("result has wrong size", list.size() >= 2);

		for (ProviderRate providerRate : list) {
			RateType rateType = providerRate.getRateType();
			Log.d(LOG_TAG, "rateType: "+rateType);

			assertTrue("alien rate type", inRateType.equals(rateType));

			CurrencyCode currencyCode = providerRate.getCurrencyCode();
			Log.d(LOG_TAG, "currencyCode: "+currencyCode);

			assertTrue("alien currency", currencyCodes.contains(currencyCode));
		}
	}
}
