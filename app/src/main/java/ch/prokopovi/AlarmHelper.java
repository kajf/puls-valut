package ch.prokopovi;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;

public class AlarmHelper {

	private static final String LOG_TAG = "AlarmHelper";

	public static void cancel(Context context, PendingIntent pendingIntent) {
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);

		alarmManager.cancel(pendingIntent);
	}

	public static void start(Context context, Calendar calendar,
			PendingIntent pendingIntent) {
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);

		Log.d(LOG_TAG, "next alarm " + calendar.getTime());

		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
				calendar.getTimeInMillis(),
				TimingConstants.ALARM_UPDATE_PERIOD, pendingIntent);
	}
}
