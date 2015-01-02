package ch.prokopovi.ui;

import ch.prokopovi.struct.WidgetPreferences;

public class MultiWidgetConfigure extends AbstractWidgetConfigure {

	@Override
	protected void callNext(WidgetPreferences prefs) {

		selectFewCurrencies(prefs, 3);

	}
}
