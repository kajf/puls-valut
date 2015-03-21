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

		/*
		 * On Android 2.3.x and lower, the system calls onPrepareOptionsMenu()
		 * each time the user opens the options menu (presses the Menu button).
		 * 
		 * On Android 3.0 and higher, the options menu is considered to always
		 * be open when menu items are presented in the action bar. When an
		 * event occurs and you want to perform a menu update, you must call
		 * invalidateOptionsMenu() to request that the system call
		 * onPrepareOptionsMenu().
		 */

		if (Build.VERSION.SDK_INT >= 11)
			a.invalidateOptionsMenu();
	}

	public static boolean isXlarge(Configuration c) {
        return (c.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }
}
