package ch.prokopovi.api.provider;

import java.util.List;

import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.err.WebUpdatingException;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.RateType;
import ch.prokopovi.struct.ProviderRequirements;

/**
 * data provider
 * 
 * @author Pavel_Letsiaha
 * 
 */
public interface Provider {

	/**
	 * list supported rate types
	 * 
	 * @return
	 */
	RateType[] getSupportedRateTypes();

	/**
	 * list supported currencies by rate type
	 * 
	 * @param rt
	 *            rate type
	 * @return
	 */
	CurrencyCode[] getSupportedCurrencyCodes(RateType rt);

	/**
	 * load rates records
	 * 
	 * @param requirements
	 *            data requirements
	 * @return
	 * @throws WebUpdatingException
	 */
	List<ProviderRate> update(ProviderRequirements requirements)
			throws WebUpdatingException;
}
