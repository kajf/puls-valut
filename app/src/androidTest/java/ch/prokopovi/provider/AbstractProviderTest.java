package ch.prokopovi.provider;

import java.util.Calendar;
import java.util.Date;

import android.test.AndroidTestCase;
import android.util.Log;
import ch.prokopovi.Util;
import ch.prokopovi.struct.Master.ProviderCode;

public class AbstractProviderTest extends AndroidTestCase {

	private static final String LOG_TAG = "AbstractProviderTest";

	public void testGetLastValidDay() {
		Calendar cal = Calendar.getInstance();
		Date currentDay = cal.getTime();

		Log.d(LOG_TAG, "current day: "+currentDay);

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


		Log.d(LOG_TAG, "lastValidDay: "+lastValidDay);
		assertNotNull("lastValidDay is null", lastValidDay);

		long currentTime = currentDay.getTime();
		long lastTime = lastValidDay.getTime();

		boolean inSameDay = Util.isInSameDay(currentTime, lastTime);

		assertTrue(expectedInSameDay == inSameDay);

	}
}
