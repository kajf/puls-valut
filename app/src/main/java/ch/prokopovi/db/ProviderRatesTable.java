package ch.prokopovi.db;


public class ProviderRatesTable {

	public enum ProviderRatesColumn {
		PROVIDER_RATE_ID, PROVIDER_ID, RATE_TYPE_ID, EXCHANGE_TYPE_ID, CURRENCY_ID, //
		VALUE, TIME_UPDATED, TIME_EFFECTIVE; //
	}

	// Database table
	public static final String TABLE = "T_PROVIDER_RATES";
}
