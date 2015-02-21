package ch.prokopovi.exported;

import java.util.HashMap;

public class RatesPlacesTable {

	public enum ColumnRatesPlaces implements DbColumn {
		RATES_PLACE_ID, REGION_ID, DESCRIPTION, X, Y, ADDR, WORK_HOURS, PHONE, BANK_ID;

		private static HashMap<String, ColumnRatesPlaces> map;

		static {
			map = new HashMap<String, ColumnRatesPlaces>();
			for (ColumnRatesPlaces val : values()) {
				map.put(val.name(), val);
			}
		}

		public static ColumnRatesPlaces get(String name) {
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
	public static final String TABLE = "T_RATES_PLACES";
}
