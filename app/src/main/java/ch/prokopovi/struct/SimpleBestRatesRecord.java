package ch.prokopovi.struct;

import ch.prokopovi.api.struct.BestRatesRecord;

/**
 * Simple implementation of BestRatesRecord
 * 
 * @author public
 * 
 */
public class SimpleBestRatesRecord implements BestRatesRecord {

	private final int id;
	private final int currencyId;
	private final int exchangeTypeId;
	private final Double value;

	public SimpleBestRatesRecord(int currencyId, int exchangeTypeId,
			Double value) {
		super();
		id = -1;
		this.currencyId = currencyId;
		this.exchangeTypeId = exchangeTypeId;
		this.value = value;
	}

	@Override
	public int getCurrencyId() {
		return currencyId;
	}

	@Override
	public int getExchangeTypeId() {
		return exchangeTypeId;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public Double getValue() {
		return value;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SimpleBestRatesRecord [id=").append(id)
				.append(", currencyId=").append(currencyId)
				.append(", exchangeTypeId=").append(exchangeTypeId)
				.append(", value=").append(value).append("]");
		return builder.toString();
	}

}
