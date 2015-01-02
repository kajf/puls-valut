package ch.prokopovi.api.struct;

/**
 * Record representing BestRates table data
 * 
 * @author public
 * 
 */
public interface BestRatesRecord {

	int getCurrencyId();

	int getExchangeTypeId();

	int getId();

	Double getValue();
}
