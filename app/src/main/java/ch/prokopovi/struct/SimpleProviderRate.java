package ch.prokopovi.struct;

import java.util.Date;

import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.OperationType;
import ch.prokopovi.struct.Master.ProviderCode;
import ch.prokopovi.struct.Master.RateType;

public class SimpleProviderRate implements ProviderRate {

	private final long id;
	private final ProviderCode provider;
	private final RateType rateType;
	private final OperationType operationType;
	private final long timeUpdated;
	private final long timeEffective;
	private CurrencyCode currencyCode;
	private Double value;

	public SimpleProviderRate(long id, ProviderCode provider,
			RateType rateType, OperationType operationType,
			CurrencyCode currencyCode, Double value, long timeUpdated,
			long timeEffective) {

		this.id = id;
		this.provider = provider;
		this.rateType = rateType;
		this.operationType = operationType;
		this.timeUpdated = timeUpdated;
		this.timeEffective = timeEffective;
		this.currencyCode = currencyCode;
		this.value = value;
	}

	public SimpleProviderRate(ProviderCode provider, RateType rateType,
			OperationType operationType, CurrencyCode currencyCode,
			Double value, long timeUpdated, long timeEffective) {

		this(-1L, provider, rateType, operationType, currencyCode, value,
				timeUpdated, timeEffective);

	}

	@Override
	public long getId() {
		return this.id;
	}

	@Override
	public ProviderCode getProvider() {
		return this.provider;
	}

	@Override
	public RateType getRateType() {
		return this.rateType;
	}

	@Override
	public OperationType getExchangeType() {
		return this.operationType;
	}

	@Override
	public long getTimeUpdated() {
		return this.timeUpdated;
	}

	@Override
	public long getTimeEffective() {
		return this.timeEffective;
	}

	@Override
	public CurrencyCode getCurrencyCode() {
		return this.currencyCode;
	}

	@Override
	public Double getValue() {
		return this.value;
	}

	public void setCurrencyCode(CurrencyCode currencyCode) {
		this.currencyCode = currencyCode;
	}

	public void setValue(Double value) {
		this.value = value;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SimpleProviderRate [provider=");
		builder.append(this.provider);
		builder.append(", rateType=");
		builder.append(this.rateType);
		builder.append(", operationType=");
		builder.append(this.operationType);
		builder.append(", currencyCode=");
		builder.append(this.currencyCode);
		builder.append(", value=");
		builder.append(this.value);
		builder.append(", timeUpdated=");
		builder.append(new Date(this.timeUpdated));
		builder.append(", timeEffective=");
		builder.append(new Date(this.timeEffective));
		builder.append("]");
		return builder.toString();
	}

}
