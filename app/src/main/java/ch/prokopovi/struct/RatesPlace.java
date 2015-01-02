package ch.prokopovi.struct;

import ch.prokopovi.api.struct.RatesPlacesRecord;
import ch.prokopovi.struct.Master.Region;

public class RatesPlace extends RatesGroup implements RatesPlacesRecord {

	private Long id;
	private String description;
	private Region region;

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public Region getRegion() {
		return this.region;
	}

	public void setRegion(Region region) {
		this.region = region;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RatesPlace [description=");
		builder.append(this.description);
		builder.append(", region=");
		builder.append(this.region);
		builder.append(", getRates().size()=");
		builder.append(getRates() != null ? getRates().size() : null);
		builder.append("]");
		return builder.toString();
	}

}
