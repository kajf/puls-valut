package ch.prokopovi.strategy;

import android.content.Context;
import ch.prokopovi.api.provider.Strategy;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.ProviderRequirements;

public final class UpdateActionStrategyFactory {

	public static Strategy createExpiredUpdateActionStrategy(Context context,
			ProviderRequirements requirements) {
		return new ExpiredUpdateActionStrategy(context, requirements);
	}

	public static Strategy createForceUpdateActionStrategy(Context context,
			ProviderRequirements requirements) {

		return new ForceUpdateActionStrategy(context, requirements);
	}

	public static Strategy createRoutineUpdateActionStrategy(Context context,
			ProviderRequirements requirements,
			CurrencyCode[] requestedCurrencies) {
		return new RoutineUpdateActionStrategy(context, requirements,
				requestedCurrencies);
	}

	public static Strategy createScheduledUpdateActionStrategy(Context context,
			ProviderRequirements requirements) {
		return new ScheduledUpdateActionStrategy(context, requirements);
	}
}
