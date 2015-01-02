package ch.prokopovi.ui.main;

import org.androidannotations.annotations.EActivity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.util.Log;
import ch.prokopovi.PrefsUtil;
import ch.prokopovi.R;

@EActivity
public class PrefsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	private static final String LOG_TAG = "PrefsActivity";

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle aSavedState) {

		PrefsUtil.initSkin(this);

		super.onCreate(aSavedState);

		// full screen
		PrefsUtil.initFullscreen(this);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			getPreferenceManager().setSharedPreferencesName(
					PrefsUtil.PREFS_NAME);
			addPreferencesFromResource(R.xml.prefs);

			getPreferenceManager().getSharedPreferences()
					.registerOnSharedPreferenceChangeListener(this);
		} else {

			// Display the fragment as the main content.
			getFragmentManager().beginTransaction()
					.replace(android.R.id.content, new PrefsFragment())
					.commit();
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			final String key) {

		Log.d(LOG_TAG, "pref: " + key + " changed");

		final Context ctx = PrefsActivity.this.getBaseContext();

		if (ctx.getString(R.string.pref_app_skin).equals(key)) {
			Log.d(LOG_TAG, "skin changed...");

			restartActivity(ctx);
		} else if (ctx.getString(R.string.pref_ads_on).equals(key)) {
			boolean isAdsOn = sharedPreferences.getBoolean(key, true);

			if (!isAdsOn) {

				final Editor editor = sharedPreferences.edit();

				// use activity as context not an app
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.pref_ads_msg)
						.setPositiveButton(R.string.pref_ads_msg_leave,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										editor.putBoolean(key, true).commit();
										restartActivity(ctx);
									}
								})
						.setNegativeButton(R.string.pref_ads_msg_off,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										editor.putBoolean(key, false).commit();
										restartActivity(ctx);
									}
								}).show();

			}

		} else if (ctx.getString(R.string.pref_fullscreen_on).equals(key)) {
			Log.d(LOG_TAG, "fullscreen changed...");

			restartActivity(ctx);
		}
	}

	private void restartActivity(Context ctx) {
		Log.d(LOG_TAG, "restarting ...");

		ctx.startActivity(TabsActivity.getLauncherIntent(ctx));
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			getPreferenceManager().getSharedPreferences()
					.registerOnSharedPreferenceChangeListener(this);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		super.onPause();

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			getPreferenceManager().getSharedPreferences()
					.unregisterOnSharedPreferenceChangeListener(this);
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class PrefsFragment extends PreferenceFragment {
		private OnSharedPreferenceChangeListener listener;

		@Override
		public void onCreate(Bundle aSavedState) {
			super.onCreate(aSavedState);
			getPreferenceManager().setSharedPreferencesName(
					PrefsUtil.PREFS_NAME);

			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.prefs);

		}

		@Override
		public void onAttach(Activity activity) {

			Log.d(LOG_TAG, "onAttach");

			super.onAttach(activity);

			// This makes sure that the container activity has implemented
			// the callback interface. If not, it throws an exception
			try {
				this.listener = (OnSharedPreferenceChangeListener) activity;

			} catch (ClassCastException e) {
				throw new ClassCastException(activity.toString()
						+ " must implement Updater");
			}
		}

		@Override
		public void onResume() {
			super.onResume();

			getPreferenceManager().getSharedPreferences()
					.registerOnSharedPreferenceChangeListener(this.listener);

		}

		@Override
		public void onPause() {
			super.onPause();

			getPreferenceManager().getSharedPreferences()
					.unregisterOnSharedPreferenceChangeListener(this.listener);

		}
	}

}
