package ch.prokopovi.provider;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

import ch.prokopovi.Util;
import ch.prokopovi.struct.Master.ProviderCode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class AbstractProviderTest {

	@Test
	public void getLastValidDay() {
		Calendar cal = Calendar.getInstance();
		Date currentDay = cal.getTime();

		int currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		int[] daysOff = ProviderCode.NBU.getDaysOff();

		boolean expectedInSameDay = true;
		for (int i : daysOff) {
			if (currentDayOfWeek == i) {
				expectedInSameDay = false;
				break;
			}
		}

		Date lastValidDay = ProviderCode.NBU.getLastValidDay();


		assertNotNull("lastValidDay is null", lastValidDay);

		long currentTime = currentDay.getTime();
		long lastTime = lastValidDay.getTime();

		boolean inSameDay = Util.isInSameDay(currentTime, lastTime);

		assertEquals(expectedInSameDay, inSameDay);
	}
}
