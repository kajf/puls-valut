package ch.prokopovi.api.struct;

import ch.prokopovi.struct.Master.Region;

public interface RatesPlacesRecord {
	String getDescription();

	Long getId();

	Region getRegion();
}
