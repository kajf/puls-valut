package ch.prokopovi.api.struct;

import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.OperationType;
import ch.prokopovi.struct.Master.ProviderCode;
import ch.prokopovi.struct.Master.RateType;

/**
 * DB record for provider rate
 * 
 * @author Pavel_Letsiaha
 * 
 */
public interface ProviderRate {

	long getId();

	ProviderCode getProvider();

	RateType getRateType();

	OperationType getExchangeType();

	CurrencyCode getCurrencyCode();

	long getTimeUpdated();

	long getTimeEffective();

	Double getValue();

}