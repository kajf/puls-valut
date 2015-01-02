package ch.prokopovi.struct;

import java.text.ParseException;

import ch.prokopovi.Util;

/**
 * dto defininig rate and its dymamic
 * 
 * @author Pavel_Letsiaha
 * 
 */
public class RateValue {

	/**
	 * define if value is positive or negative or sign is undefined
	 * 
	 * @param input
	 * 
	 * @return
	 */
	private static Boolean parseDirection(String input) {
		Boolean res = null;

		boolean isZero = true;
		try {
			// for . as decimal separator
			double val = Util.parseDotDouble(input);

			isZero = Util.isZero(val);
		} catch (ParseException e) {
		}

		if (input != null && !isZero) {
			res = true; //
			if (input.startsWith("-")) {
				res = false;
			}
		}

		return res;
	}

	private String current;
	private Boolean direction; // increasing - true, decreasing - false,

	// undefined - null
	private String dynamic;

	public RateValue(String current, String dynamic) {
		super();
		this.current = current;
		setDynamic(dynamic);
	}

	public String getCurrent() {
		return this.current;
	}

	public Boolean getDirection() {
		return this.direction;
	}

	public String getDynamic() {
		return this.dynamic;
	}

	public void setCurrent(String current) {
		this.current = current;
	}

	public void setDynamic(String dynamic) {
		if (dynamic != null) {

			dynamic = dynamic.trim().replace("%", "");
			this.direction = parseDirection(dynamic);
			dynamic = dynamic.replace("+", "").replace("-", "");
		}
		this.dynamic = dynamic;
	}

	@Override
	public String toString() {
		return String.format(
				"RateValue [current=%s, direction=%s, dynamic=%s]",
				this.current, this.direction, this.dynamic);
	}
}
