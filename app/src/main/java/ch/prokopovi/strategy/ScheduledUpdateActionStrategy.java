package ch.prokopovi.strategy;

import android.content.Context;
import ch.prokopovi.api.provider.Strategy;
import ch.prokopovi.struct.ProviderRequirements;

class ScheduledUpdateActionStrategy extends AbstractUpdateActionStrategy
		implements Strategy {

	ScheduledUpdateActionStrategy(Context context,
			ProviderRequirements requirements) {
		super(context, requirements);
	}

}
