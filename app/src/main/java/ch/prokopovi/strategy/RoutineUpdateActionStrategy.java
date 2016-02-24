package ch.prokopovi.strategy;

import java.util.List;

import android.content.Context;
import android.util.Log;
import ch.prokopovi.api.provider.Strategy;
import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.err.OfflineException;
import ch.prokopovi.err.WebUpdatingException;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.ProviderRequirements;

class RoutineUpdateActionStrategy extends AbstractUpdateActionStrategy
		implements Strategy {

	private static final String LOG_TAG = "RoutineUpdateStrategy";

	private final CurrencyCode[] requestedCurrencies;

	RoutineUpdateActionStrategy(Context context,
			ProviderRequirements requirements,
			CurrencyCode[] requestedCurrencies) {
		super(context, requirements);

		this.requestedCurrencies = requestedCurrencies;
	}

	@Override
	public void execute() throws WebUpdatingException, OfflineException {

		Log.d(LOG_TAG, "execute()");

		List<ProviderRate> list = find(); // fetch from db

		boolean hasData = hasData(requestedCurrencies, list);
		boolean expired = isExpired(list);

		if (!hasData || expired) {
			super.execute();
		}
	}
}
