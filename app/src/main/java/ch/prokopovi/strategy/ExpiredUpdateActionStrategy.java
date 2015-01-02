package ch.prokopovi.strategy;

import java.util.List;

import android.content.Context;
import android.util.Log;
import ch.prokopovi.api.provider.Strategy;
import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.err.OfflineException;
import ch.prokopovi.err.WebUpdatingException;
import ch.prokopovi.struct.ProviderRequirements;

/**
 * executes strategy in case if last update was too long time ago
 * 
 * @author public
 * 
 */
class ExpiredUpdateActionStrategy extends AbstractUpdateActionStrategy
		implements Strategy {

	private static final String LOG_TAG = "ExpiredUpdateActionStrategy";

	ExpiredUpdateActionStrategy(Context context,
			ProviderRequirements requirements) {
		super(context, requirements);
	}

	@Override
	public void execute() throws WebUpdatingException, OfflineException {

		Log.d(LOG_TAG, "execute()");

		List<ProviderRate> list = find(); // fetch from db

		if (isExpired(list)) {
			super.execute();
		}
	}
}
