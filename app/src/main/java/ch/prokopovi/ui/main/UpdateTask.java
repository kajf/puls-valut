package ch.prokopovi.ui.main;

import android.util.Log;
import android.widget.Toast;

import org.androidannotations.annotations.*;

import java.util.List;
import java.util.Map.Entry;

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
					.getPlaces(activity, region);

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

		Toast.makeText(this.activity, R.string.err_update, Toast.LENGTH_LONG).show();
		// unsuccessful update
	}

	public boolean isInProgress() {
		return inProgress;
	}
}
