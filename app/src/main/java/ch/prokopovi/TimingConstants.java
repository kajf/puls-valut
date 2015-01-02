package ch.prokopovi;

import android.text.format.DateUtils;

public final class TimingConstants {

	public static final long DATA_EXPIRATION_PERIOD = DateUtils.HOUR_IN_MILLIS * 4;
	public static final long MIN_UPDATE_PERIOD = DateUtils.MINUTE_IN_MILLIS;
	public static final long ALARM_UPDATE_PERIOD = DateUtils.DAY_IN_MILLIS;
}
