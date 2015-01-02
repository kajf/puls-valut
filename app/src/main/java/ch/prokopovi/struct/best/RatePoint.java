package ch.prokopovi.struct.best;

public class RatePoint {
	public int id;
	public int regionId;
	public Integer bankId;
	public String placeDescription;
	public Double x;
	public Double y;
	public String addr;
	public String workHours;
	public String phones;

	public RatePoint(int id, int regionId, Integer bankId,
			String placeDescription, Double x, Double y, String addr,
			String workHours, String phones) {
		super();
		this.id = id;
		this.regionId = regionId;
		this.bankId = bankId;
		this.placeDescription = placeDescription;
		this.x = x;
		this.y = y;
		this.addr = addr;
		this.workHours = workHours;
		this.phones = phones;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RatePoint [id=").append(this.id).append(", regionId=")
				.append(this.regionId).append(", bankId=").append(this.bankId)
				.append(", placeDescription=").append(this.placeDescription)
				.append(", x=").append(this.x).append(", y=").append(this.y)
				.append(", addr=").append(this.addr).append(", workHours=")
				.append(this.workHours).append(", phones=").append(this.phones)
				.append("]");
		return builder.toString();
	}

}
