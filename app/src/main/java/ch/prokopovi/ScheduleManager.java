package ch.prokopovi;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;

public final class ScheduleManager {

	public enum ScheduledIntentCode {
		MORNING(9 * 60 + 30), AFTERNOON(12 * 60 + 30), EVENING(16 * 60);

		private int minutesFromMidnight = 0;

		private ScheduledIntentCode(int minutesFromMidnight) {
			this.minutesFromMidnight = minutesFromMidnight;
		}

		public int getMinutesFromMidnight() {
			return minutesFromMidnight;
		}
	}

	private static final String LOG_TAG = "ScheduleManager";

	private static final int TZ_OFFSET = 180;

	/**
	 * calculate time zone correction in minutes
	 * 
	 * @param currentTimeZone
	 * 
	 * @return minutes to add to current time zone
	 */
	private static int calculateTzCorrection(TimeZone currentTimeZone) {
		Date today = new Date();

		int offset = currentTimeZone.getOffset(today.getTime());
		Log.d(LOG_TAG, "time zone offset " + offset + " ms");

		int tzMinutesOffset = offset / (60 * 1000);
		Log.d(LOG_TAG, "time zone offset " + tzMinutesOffset + " minutes"); // -
																			// 120

		int correction = (TZ_OFFSET - tzMinutesOffset); // 300 =
														// (180) -
														// (-120)

		Log.d(LOG_TAG, "time zone correction " + correction + " minutes"); // 300

		return -correction;
	}

	/**
	 * stop all planned alarms
	 * 
	 * @param context
	 */
	public static void off(Context context) {
		Log.d(LOG_TAG, "off");

		Map<ScheduledIntentCode, PendingIntent> senders = prepareSenders(context);

		AlarmHelper.cancel(context, senders.get(ScheduledIntentCode.MORNING));
		AlarmHelper.cancel(context, senders.get(ScheduledIntentCode.AFTERNOON));
		AlarmHelper.cancel(context, senders.get(ScheduledIntentCode.EVENING));
	}

	/**
	 * schedule all planned events
	 * 
	 * @param context
	 */
	public static void on(Context context) {
		Log.d(LOG_TAG, "on");

		Map<ScheduledIntentCode, PendingIntent> senders = prepareSenders(context);

		Calendar calendar = Calendar.getInstance();

		int tzCorrection = calculateTzCorrection(calendar.getTimeZone());

		// set schedules from last to first. For not resetting calendar day
		// every time. Day is added if earlier date has passed.

		scheduleAlarm(context, ScheduledIntentCode.EVENING, senders, calendar,
				tzCorrection);
		scheduleAlarm(context, ScheduledIntentCode.AFTERNOON, senders,
				calendar, tzCorrection);
		scheduleAlarm(context, ScheduledIntentCode.MORNING, senders, calendar,
				tzCorrection);
	}

	/**
	 * create list of pending intents for various events
	 * 
	 * @param context
	 * @return
	 */
	private static Map<ScheduledIntentCode, PendingIntent> prepareSenders(
			Context context) {

		Map<ScheduledIntentCode, PendingIntent> res = new HashMap<ScheduleManager.ScheduledIntentCode, PendingIntent>();

		ScheduledIntentCode[] intentCodes = ScheduledIntentCode.values();
		for (ScheduledIntentCode code : intentCodes) {
			PendingIntent tmp = IntentFactory
					.createScheduledUpdateServiceIntent(context, code);

			res.put(code, tmp);
		}

		return res;
	}

	/**
	 * prepare and start next day alarm
	 * 
	 * @param context
	 * @param code
	 *            code of alarm
	 * @param senders
	 *            map of prepared events with codes
	 * @param calendar
	 *            calendar shared between alarms (for adding days if needed)
	 * @param tzCorrection
	 *            time zone correction in minutes
	 */
	private static void scheduleAlarm(Context context,
			ScheduledIntentCode code,
			Map<ScheduledIntentCode, PendingIntent> senders, Calendar calendar,
			int tzCorrection) {

		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);

		calendar.add(Calendar.MINUTE, tzCorrection);

		calendar.add(Calendar.MINUTE, code.getMinutesFromMidnight());

		long now = new Date().getTime() + 1000; // correction for tightly
												// situated time moments
		if (calendar.getTimeInMillis() <= now) {
			Log.d(LOG_TAG, code + " already passed");

			calendar.add(Calendar.DAY_OF_MONTH, 1);
		}
		AlarmHelper.start(context, calendar, senders.get(code));
	}
}
