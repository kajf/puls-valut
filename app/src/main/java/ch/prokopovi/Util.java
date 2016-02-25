package ch.prokopovi;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

public final class Util {

	private static final double ZERO_THRESHOLD = 0.0001;

	/**
	 * parse double value with '.' as decimal separator. locale independent
	 * 
	 * @param str
	 *            value to parse
	 * @return parsed value
	 * 
	 * @throws ParseException
	 *             if error occurs
	 */
	public static double parseDotDouble(String str) throws ParseException {
		// for . as decimal separator
		return parseDouble(str, '.');
	}

	/**
	 * parse double value with ',' as decimal separator. locale independent
	 * 
	 * @param str
	 *            value to parse
	 * @return parsed value
	 * 
	 * @throws ParseException
	 *             if error occurs
	 */
	public static double parseCommaDouble(String str) throws ParseException {
		// for , as decimal separator

		return parseDouble(str, ',');
	}

	/**
	 * parse double by provided locale
	 * 
	 * @param str
	 *            value to parse
	 * @param decSeparator
	 * @return parsed value
	 * 
	 * @throws ParseException
	 *             if error occurs
	 */
	private static double parseDouble(String str, char decSeparator)
			throws ParseException {
		// for , as decimal separator

		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setDecimalSeparator(decSeparator);

		DecimalFormat df = new DecimalFormat();
		df.setGroupingUsed(false);
		df.setDecimalFormatSymbols(symbols);

		Number number = df.parse(str);

		double val = number.doubleValue();

		return val;
	}

	/**
	 * check whether dates are in the same day
	 * 
	 * @param t1
	 * @param t2
	 * 
	 * @return
	 */
	public static boolean isInSameDay(long t1, long t2) {

		Calendar cal1 = Calendar.getInstance();
		cal1.setTime(new Date(t1));

		Calendar cal2 = Calendar.getInstance();
		cal2.setTime(new Date(t2));

		if (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
				&& cal1.get(Calendar.DAY_OF_YEAR) == cal2
						.get(Calendar.DAY_OF_YEAR)) {
			return true;
		}

		return false;
	}

	/**
	 * checks double against zero threshold
	 * 
	 * @param dbl
	 * @param threshold
	 *            optional zero-value threshold
	 * @return
	 */
	public static boolean isZero(double dbl, Double... threshold) {

		Double th = ZERO_THRESHOLD;
		if (threshold.length > 0 && threshold[0] != null) {
			th = threshold[0];
		}

		return dbl > -th && dbl < th;
	}

	public static boolean isBlank(String str) {
		if (str == null)
			return true;

		String stripped = str.replace(" ", "");

		if ("".equals(stripped))
			return true;

		return false;
	}
}
