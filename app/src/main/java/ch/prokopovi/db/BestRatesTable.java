package ch.prokopovi.db;

import java.util.HashMap;

public class BestRatesTable {

	public enum ColumnBestRates {
		BEST_RATES_ID, CURRENCY_ID, EXCHANGE_TYPE_ID, RATES_PLACE_ID, TIME_UPDATED, VALUE;

		private static HashMap<String, ColumnBestRates> map;

		static {
			map = new HashMap<String, ColumnBestRates>();
			for (ColumnBestRates val : values()) {
				map.put(val.name(), val);
			}
		}

		public static ColumnBestRates get(String name) {
			return map.get(name);
		}

		public static String[] getAllNames() {

			int len = values().length;
			String[] names = new String[len];
			for (int i = 0; i < len; i++) {
				names[i] = values()[i].name();
			}

			return names;
		}
	}

	// Database table
	public static final String TABLE = "T_BEST_RATES";
}
