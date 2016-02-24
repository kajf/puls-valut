package ch.prokopovi.strategy;

import java.util.Date;
import java.util.List;

import android.content.Context;
import android.util.Log;
import ch.prokopovi.TimingConstants;
import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.err.OfflineException;
import ch.prokopovi.err.WebUpdatingException;
import ch.prokopovi.struct.ProviderRequirements;

class ForceUpdateActionStrategy extends AbstractUpdateActionStrategy {

	private static final String LOG_TAG = "ForceUpdateStrategy";

	ForceUpdateActionStrategy(Context context, ProviderRequirements requirements) {
		super(context, requirements);
	}

	@Override
	public void execute() throws WebUpdatingException, OfflineException {

		Log.d(LOG_TAG, "execute()");

		boolean tooFrequent = true;
		List<ProviderRate> list = find(); // fetch from db

		// was last update too short time ago ?
		if (list.isEmpty()) {
			tooFrequent = false;
		} else {
			long timeUpdated = getLastTimeUpdated(list);
			long timePassed = new Date().getTime() - timeUpdated;

			tooFrequent = timePassed < TimingConstants.MIN_UPDATE_PERIOD;
		}

		Log.d(LOG_TAG, "tooFrequent: " + tooFrequent);

		if (!tooFrequent) {
			super.execute();
		}
	}
}
