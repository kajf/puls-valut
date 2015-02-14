package ch.prokopovi.ui.main;

import java.util.List;
import java.util.Map.Entry;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import ch.prokopovi.R;
import ch.prokopovi.api.provider.PlacesProvider;
import ch.prokopovi.api.struct.BestRatesRecord;
import ch.prokopovi.db.BestRatesDbAdapter;
import ch.prokopovi.provider.places.PlacesProviderFactory;
import ch.prokopovi.struct.Master.Region;

@EBean
public class UpdateTask {

	private static final String LOG_TAG = "UpdateTask";

	// Notice that we manipulate the activity ref only from the UI thread
	@RootContext
	TabsActivity activity;

	private static boolean inProgress = false;

	// All background tasks with the same serial will be executed sequentially
	@Background(serial = "bestRatesUpdate")
	void go(BestRatesDbAdapter dbAdapter, Region region) {

		inProgress = true;

		try {

			PlacesProvider provider = PlacesProviderFactory.find(region);
			if (provider == null) {
				Log.w(LOG_TAG, "no provider found for " + region);
				end();
				return;
			}

			List<Entry<Long, BestRatesRecord>> records = provider
					.getPlaces(region);

			if (records.size() > 0) {

				dbAdapter.createOrUpdate(region, records);

				success();
			} else {
				error();
			}

		} finally {
			inProgress = false;
		}
	}

	@UiThread
	void end() {
		this.activity.fireDataUpdate();
	}

	@UiThread
	void success() {
		end();
	}

	@UiThread
	void error() {
		end();

		new AlertDialog.Builder(this.activity)
				// .setIcon(android.R.drawable.ic_dialog_alert)
				.setMessage(R.string.err_update)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {

								/* User clicked OK so do some stuff */
							}
						}).create().show(); // unsuccessful update
	}

	public boolean isInProgress() {
		return inProgress;
	}
}
