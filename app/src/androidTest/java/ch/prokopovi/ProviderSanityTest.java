package ch.prokopovi;

import android.test.AndroidTestCase;

import java.util.List;
import java.util.Set;

import ch.prokopovi.api.provider.Provider;
import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.struct.Master.*;
import ch.prokopovi.struct.ProviderRequirements;

public class ProviderSanityTest extends AndroidTestCase {

    public void testNoRequirements() throws Exception {

        ProviderCode[] providerCodes = ProviderCode.values();
        for (ProviderCode code : providerCodes) {
            // given
            Provider p = (Provider) code.getClazz().newInstance();

            // when
            List<ProviderRate> list = p.update(null);

            // then
            assertNotNull("provider: " + p, list);
            assertTrue("provider: " + p, list.isEmpty());
        }
    }

    public void testNoCurrencies() throws Exception {

        ProviderCode[] providerCodes = ProviderCode.values();
        for (ProviderCode code : providerCodes) {
            Provider p = (Provider) code.getClazz().newInstance();

            RateType[] rateTypes = p.getSupportedRateTypes();
            for (RateType rateType : rateTypes) {
                // given
                ProviderRequirements requirements = new ProviderRequirements(code, rateType);

                // when
                List<ProviderRate> list = p.update(requirements);

                // then
                assertNotNull("provider: " + p, list);
            }
        }
    }

    public void testNoRateType() throws Exception {

        ProviderCode[] providerCodes = ProviderCode.values();
        for (ProviderCode code : providerCodes) {
            Provider p = (Provider) code.getClazz().newInstance();

            RateType[] rateTypes = p.getSupportedRateTypes();
            if (rateTypes.length <= 1) continue;

            // given
            CurrencyCode[] currCodes = p.getSupportedCurrencyCodes(rateTypes[0]);

            ProviderRequirements requirements = new ProviderRequirements(code, null);
            Set<CurrencyCode> currencySet = requirements.getCurrencyCodes();
            for (int i = 0; i < currCodes.length; i++) {
                currencySet.add(currCodes[i]);
            }

            // when
            List<ProviderRate> list = p.update(requirements);

            // then
            assertNotNull("provider: " + p, list);
        }
    }

    public void testUpdate() throws Exception {

        ProviderCode[] providerCodes = ProviderCode.values();
        for (ProviderCode code : providerCodes) {
            Provider p = (Provider) code.getClazz().newInstance();

            RateType[] rateTypes = p.getSupportedRateTypes();
            for (RateType rateType : rateTypes) {
                // given
                CurrencyCode[] currCodes = p.getSupportedCurrencyCodes(rateType);

                ProviderRequirements requirements = new ProviderRequirements(code, rateType);
                Set<CurrencyCode> currencySet = requirements.getCurrencyCodes();
                for (int i = 0; i < currCodes.length; i++) {
                    currencySet.add(currCodes[i]);
                }

                int expectedSize = currCodes.length;

                // when
                List<ProviderRate> list = p.update(requirements);

                // then
                check(p, list, expectedSize, rateType, currencySet);
            }
        }
    }

    private void check(Provider p, List<ProviderRate> list, int minExpectedSize, RateType rt, Set<CurrencyCode> ccs) {
        String prefix = "checking provider: " + p + "type: " + rt;

        assertNotNull(prefix + "result is null", list);
        assertTrue(prefix + " result has wrong size: " + list.size() + ", expected: " + minExpectedSize, list.size() >= minExpectedSize);

        for (ProviderRate providerRate : list) {
            RateType rateType = providerRate.getRateType();
            CurrencyCode currencyCode = providerRate.getCurrencyCode();

            assertTrue(prefix + "alien rate type: " + providerRate, rt.equals(rateType));
            assertTrue(prefix + "alien currency: " + providerRate, ccs.contains(currencyCode));

            Double value = providerRate.getValue();
            if (value != null)
                assertFalse(prefix + "zero rate value " + providerRate, Util.isZero(value));
        }
    }

}
