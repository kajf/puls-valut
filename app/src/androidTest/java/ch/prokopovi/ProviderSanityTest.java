package ch.prokopovi;

import java.util.List;
import java.util.Set;

import android.test.AndroidTestCase;
import android.util.Log;
import ch.prokopovi.api.provider.Provider;
import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.ProviderCode;
import ch.prokopovi.struct.Master.RateType;
import ch.prokopovi.struct.ProviderRequirements;

public class ProviderSanityTest extends AndroidTestCase {

    private static final String LOG_TAG = "ProviderSanityTest";


    public void testUpdate() throws Exception {


        ProviderCode[] providerCodes = ProviderCode.values();
        for (ProviderCode code : providerCodes) {
            Provider p = (Provider) code.getClazz().newInstance();

            RateType[] rateTypes = p.getSupportedRateTypes();
            for (RateType rateType : rateTypes) {
                CurrencyCode[] currCodes = p.getSupportedCurrencyCodes(rateType);

                ProviderRequirements requirements = new ProviderRequirements(code, rateType);
                Set<CurrencyCode> currencySet = requirements.getCurrencyCodes();
                for (int i = 0; i < currCodes.length; i++) {
                    currencySet.add(currCodes[i]);
                }

                int expectedSize = currCodes.length;

                List<ProviderRate> list = p.update(requirements);

                Log.d(LOG_TAG, "checking provider: " + p + "type: " + rateType);
                check(p, list, expectedSize, rateType, currencySet);
            }
        }
    }

    private void check(Provider p, List<ProviderRate> list, int minExpectedSize, RateType rt, Set<CurrencyCode> ccs) {
        assertNotNull("result is null", list);
        assertTrue(p.getClass().getSimpleName()+" result has wrong size: "+list.size()+", expected: "+minExpectedSize, list.size() >= minExpectedSize);

        for (ProviderRate providerRate : list) {
            RateType rateType = providerRate.getRateType();
            CurrencyCode currencyCode = providerRate.getCurrencyCode();

            assertTrue("alien rate type: "+providerRate, rt.equals(rateType));
            assertTrue("alien currency: "+providerRate, ccs.contains(currencyCode));

            Double value = providerRate.getValue();
            if (value != null)
                assertFalse("zero rate value " + providerRate, Util.isZero(value));
        }
    }

}
