package ch.prokopovi;

import org.junit.Test;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UtilTest {

	private static final String LOG_TAG = "UtilTest";

	@Test
	public void parseDotDouble() throws ParseException {
		double res = Util.parseDotDouble("3020.70");

		assertEquals("dot double parsing is wrong", 3020.70, res, 0.00001);
	}

	@Test
	public void parseCommaDouble() throws ParseException {
		double res = Util.parseCommaDouble("3110,8270");

		assertEquals("comma double parsing is wrong", 3110.8270, res, 0.00001);
	}

	@Test
	public void isSameDay()  {

		long now = new Date().getTime();
		boolean sameDay = Util.isInSameDay(now, now);
		assertTrue("not the same day", sameDay);

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, 1);

		long tomorrow = calendar.getTime().getTime();

		boolean wrongSameDay = Util.isInSameDay(now, tomorrow);
		assertFalse("the same day", wrongSameDay);

	}

	@Test
	public void isBlankNull() {
		// given
		String str = null;

		// when
		boolean isBlank = Util.isBlank(str);

		// then
		assertTrue(isBlank);
	}

	@Test
	public void isBlankEmpty() {
		// given
		String str = "";

		// when
		boolean isBlank = Util.isBlank(str);

		// then
		assertTrue(isBlank);
	}

	@Test
	public void isBlankNo() {
		// given
		String str = "test";

		// when
		boolean isBlank = Util.isBlank(str);

		// then
		assertFalse(isBlank);
	}

	@Test
	public void isBlankSpaces() {
		// given
		String str = "  ";

		// when
		boolean isBlank = Util.isBlank(str);

		// then
		assertTrue(isBlank);
	}
}
