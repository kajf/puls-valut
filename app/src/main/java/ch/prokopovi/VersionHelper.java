package ch.prokopovi;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;

public class VersionHelper {

	/**
	 * force action bar items to refresh (if needed)
	 * 
	 * @param a
	 *            activity
	 */
	@SuppressLint("NewApi")
	public static void invalidateOptionsMenu(Activity a) {
		a.invalidateOptionsMenu();
	}

	public static boolean isXlarge(Configuration c) {
        return (c.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }
}
