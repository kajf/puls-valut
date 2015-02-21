package ch.prokopovi.db;

import java.util.HashMap;

import ch.prokopovi.exported.DbColumn;

public class BestRatesTable {

	public enum ColumnBestRates implements DbColumn {
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

        @Override
        public String getName() {
            return name();
        }
    }

	// Database table
	public static final String TABLE = "T_BEST_RATES";
}
