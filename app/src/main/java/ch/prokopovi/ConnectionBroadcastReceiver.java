package ch.prokopovi;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * receiver calling update service on connection restoration
 * 
 * @author Pavel_Letsiaha
 * 
 */
public class ConnectionBroadcastReceiver extends BroadcastReceiver {

	private static final String LOG_TAG = "ConnectionBR";

	private static Boolean previousOnline = null;

	/**
	 * check whether web connection alive or not
	 * 
	 * @return true - online, false - otherwise
	 */
	public static boolean isOnline(Context context) {
		boolean res = false;

		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnected() && !netInfo.isRoaming()) {
			res = true;
		}

		Log.d(LOG_TAG, "isOnline [" + res + "]");
		return res;
	}

	/**
	 * change updating status depending on connection status (no connection - no
	 * attempts to update)
	 * 
	 * @param context
	 */
	public static void updateState(Context context) {

		boolean currentOnline = isOnline(context);

		boolean smthChanged = previousOnline == null
				|| previousOnline != currentOnline;
		Log.d(LOG_TAG, "something changed: " + smthChanged);
		if (!smthChanged)
			return; // nothing's changed

		previousOnline = currentOnline;

		if (currentOnline) {
			Log.d(LOG_TAG, "internet connection restored");

			ScheduleManager.on(context);

			try {
				PendingIntent expiredIntent = IntentFactory
						.createExpiredUpdateServiceIntent(context);

				expiredIntent.send();
			} catch (CanceledException e) {
				Log.e(LOG_TAG, "error on expired update intent", e);
			}

		} else {
			Log.d(LOG_TAG, "internet connection lost");

			ScheduleManager.off(context);
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(LOG_TAG, "receive " + intent);

		if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

			updateState(context);
		}
	}
}
