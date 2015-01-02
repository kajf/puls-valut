package ch.prokopovi.struct.best;

import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.OperationType;

public class RateItem {

	public int id;
	public int placeId;
	public CurrencyCode currency;
	public OperationType operationType;
	public Double value;
	public Long timeUpdated;

	public RateItem(int id, int placeId, CurrencyCode currency,
			OperationType operationType, Double value, Long timeUpdated) {
		super();
		this.id = id;
		this.placeId = placeId;
		this.currency = currency;
		this.operationType = operationType;
		this.value = value;
		this.timeUpdated = timeUpdated;
	}

}
