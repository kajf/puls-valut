package ch.prokopovi.struct;

import ch.prokopovi.api.struct.BestRatePlace;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.OperationType;

public class SimpleBestRatePlace implements BestRatePlace {

	private final int id;
	private final int regionId;
	private final Double rateValue;
	private final Long rateTimeUpdated;
	private final String placeDescription;
	private final Double x;
	private final Double y;
	private final String addr;
	private final String workHours;
	private final String phone;
	private final CurrencyCode currency;
	private final OperationType operationType;

	public SimpleBestRatePlace(int id, int regionId, Double rateValue,
			Long rateTimeUpdated, String placeDescription, Double x, Double y,
			String addr, String workHours, String phone, CurrencyCode currency,
			OperationType operationType) {
		super();
		this.id = id;
		this.regionId = regionId;
		this.rateValue = rateValue;
		this.rateTimeUpdated = rateTimeUpdated;
		this.placeDescription = placeDescription;
		this.x = x;
		this.y = y;
		this.addr = addr;
		this.workHours = workHours;
		this.phone = phone;
		this.currency = currency;
		this.operationType = operationType;
	}

	@Override
	public int getId() {
		return this.id;
	}

	@Override
	public int getRegionId() {
		return this.regionId;
	}

	@Override
	public String getPlaceDescription() {
		return this.placeDescription;
	}

	@Override
	public Double getRateValue() {
		return this.rateValue;
	}

	@Override
	public Long getRateTimeUpdated() {
		return this.rateTimeUpdated;
	}

	@Override
	public Double getX() {
		return this.x;
	}

	@Override
	public Double getY() {
		return this.y;
	}

	@Override
	public String getAddr() {
		return this.addr;
	}

	@Override
	public String getWorkHours() {
		return this.workHours;
	}

	@Override
	public String getPhone() {
		return this.phone;
	}

	@Override
	public CurrencyCode getCurrency() {
		return this.currency;
	}

	@Override
	public OperationType getOperationType() {
		return this.operationType;
	}

	@Override
	public boolean hasValue() {
		return this.currency != null && this.operationType != null
				&& this.rateTimeUpdated != null && this.rateValue != null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SimpleBestRatePlace [rateValue=");
		builder.append(this.rateValue);
		builder.append(", curr=");
		builder.append(this.currency);
		builder.append(", oper=");
		builder.append(this.operationType);
		builder.append(", timeUpdated=");
		builder.append(this.rateTimeUpdated);
		builder.append(", placeDescr=");
		builder.append(this.placeDescription);
		builder.append(", x=");
		builder.append(this.x);
		builder.append(", y=");
		builder.append(this.y);
		builder.append(", addr=");
		builder.append(this.addr);
		builder.append("]");
		return builder.toString();
	}

}
