package ch.prokopovi.api.struct;

import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.OperationType;

public interface BestRatePlace {

	int getId();

	int getRegionId();

	String getPlaceDescription();

	Double getX();

	Double getY();

	String getAddr();

	String getWorkHours();

	String getPhone();

	Double getRateValue();

	Long getRateTimeUpdated();

	CurrencyCode getCurrency();

	OperationType getOperationType();

	boolean hasValue();
}
