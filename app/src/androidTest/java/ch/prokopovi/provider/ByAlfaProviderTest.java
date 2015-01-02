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

public class ByAlfaProviderTest extends AndroidTestCase {

    private static final String LOG_TAG = "ByAlfaProviderTest";

    private Provider provider;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.provider = new ByAlfaProvider();
    }

    public void testUpdate() throws WebUpdatingException {

        RateType inRateType = RateType.CASH;

        ProviderRequirements requirements = new ProviderRequirements(ProviderCode.BY_ALFA, inRateType);
        Set<CurrencyCode> currencyCodes = requirements.getCurrencyCodes();
        currencyCodes.add(CurrencyCode.USD);
        currencyCodes.add(CurrencyCode.EUR);
        currencyCodes.add(CurrencyCode.RUR);

        List<ProviderRate> list = this.provider.update(requirements);

        assertNotNull("result is null", list);
        assertTrue("result has wrong size: "+ list.size()+", expected: 6", list.size() >= 6);

        for (ProviderRate providerRate : list) {
            RateType rateType = providerRate.getRateType();
            CurrencyCode currencyCode = providerRate.getCurrencyCode();

            Log.d(LOG_TAG, "rateType: "+rateType + ", currencyCode: "+currencyCode);

            assertTrue("alien rate type", inRateType.equals(rateType));
            assertTrue("alien currency", currencyCodes.contains(currencyCode));
        }
    }
}
