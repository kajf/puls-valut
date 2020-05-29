package ch.prokopovi;

import java.util.Arrays;
import java.util.List;

import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import android.util.Log;
import android.util.SparseArray;
import ch.prokopovi.IntentFactory.ExtrasKey;
import ch.prokopovi.api.provider.Strategy;
import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.db.ProviderRatesDbAdapter;
import ch.prokopovi.err.OfflineException;
import ch.prokopovi.err.WebUpdatingException;
import ch.prokopovi.strategy.UpdateActionStrategyFactory;
import ch.prokopovi.struct.Master;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.ProviderCode;
import ch.prokopovi.struct.Master.RateType;
import ch.prokopovi.struct.ProviderRequirements;
import ch.prokopovi.ui.WidgetBroker;
import ch.prokopovi.ui.WidgetBroker.WidgetUpdateType;

/**
 * service to process widget update-on-click event
 * 
 * @author Pavel_Letsiaha
 * 
 */
public class UpdateService extends JobIntentService {

	private static final String LOG_TAG = "UpdateService";

    private void wrapStrategy(final Strategy strategy,
			final ProviderRequirements requirements) {

		final ProviderCode reqProvider = requirements.getProviderCode();
		final RateType reqRateType = requirements.getRateType();

		final Integer requirementsMapCode = Master.calcMapCode(requirements);

		ContextWrapper ctx = UpdateService.this;
		final List<ProviderRate> list = ProviderRatesDbAdapter.read(ctx,
				reqProvider, reqRateType);

		final boolean emptyData = list == null || list.isEmpty();

		if (!emptyData) {

			WidgetBroker.update(ctx, reqProvider, reqRateType,
					WidgetUpdateType.UPDATING);
		}// do not send update if no data was provided previously

		try {
			strategy.execute();
			WidgetBroker.update(ctx, reqProvider, reqRateType,
					WidgetUpdateType.FULL);

		} catch (WebUpdatingException e) {
			Log.d(LOG_TAG, "web upd err ", e);

			if (emptyData) {
				String txt = ctx.getResources().getString(R.string.err_nodata);
				WidgetBroker.message(ctx, requirementsMapCode, txt);

			} else { // show older data
				WidgetBroker.update(ctx, reqProvider, reqRateType,
						WidgetUpdateType.FULL);
			}

		} catch (OfflineException e) {
			Log.d(LOG_TAG, "offline err ", e);

			if (emptyData) {
				String txt = ctx.getResources().getString(R.string.err_offline);
				WidgetBroker.message(ctx, requirementsMapCode, txt);

			} else { // show older data
				WidgetBroker.update(ctx, reqProvider, reqRateType,
						WidgetUpdateType.FULL);
			}
		}
	}

    @Override
	public void onDestroy() {
		super.onDestroy();

		Log.i(LOG_TAG, "service is destroyed");
	}

	@Override
	protected void onHandleWork(@NonNull Intent intent) {

		Log.d(LOG_TAG, "started " + intent);

		ContextWrapper context = UpdateService.this;

		// send stats if needed
		StatsHelper.dailyCollect(context);

		Bundle extras = intent.getExtras();

		// in some cases null is applicable
		Integer mapCode = null;
		if (extras != null) {
			ProviderCode providerCode = (ProviderCode) extras
					.get(ExtrasKey.PROVIDER.name());

			RateType rateType = (RateType) extras.get(ExtrasKey.RATE_TYPE
					.name());

			Log.d(LOG_TAG, "providerCode: " + providerCode + " rateType: "
					+ rateType);

			mapCode = Master.calcMapCode(providerCode, rateType);
		}

		String action = intent.getAction();

		SparseArray<ProviderRequirements> dataRequirements = PrefsUtil
				.collectRequirements(context);

		if (IntentFactory.ACTION_FORCE_UPDATE.equals(action)) {

			if (mapCode != null) {
				ProviderRequirements requirements = dataRequirements
						.get(mapCode);

				if (requirements == null) // not found
					return;

				Strategy strategy = UpdateActionStrategyFactory
						.createForceUpdateActionStrategy(context,
								requirements);

				wrapStrategy(strategy, requirements);

			}

		} else if (IntentFactory.ACTION_ROUTINE_UPDATE.equals(action)) {

			Parcelable[] parcelableArray = extras
					.getParcelableArray(ExtrasKey.CURRENCIES.name());

			if (mapCode != null && parcelableArray != null) {

				List<Parcelable> list = Arrays.asList(parcelableArray);
				CurrencyCode[] currencies = list
						.toArray(new CurrencyCode[list.size()]);
				Log.d(LOG_TAG, "currencies: " + Arrays.toString(currencies));

				ProviderRequirements requirements = dataRequirements
						.get(mapCode);

				Log.d(LOG_TAG, "requirements: " + requirements);

				if (requirements == null) // not found
					return;

				Strategy strategy = UpdateActionStrategyFactory
						.createRoutineUpdateActionStrategy(context,
								requirements, currencies);

				wrapStrategy(strategy, requirements);
			} // if no parameters - skip call
		}
	}
}