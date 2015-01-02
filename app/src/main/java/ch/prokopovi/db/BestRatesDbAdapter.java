package ch.prokopovi.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import android.util.SparseArray;
import ch.prokopovi.api.struct.BestRatesRecord;
import ch.prokopovi.db.BestRatesTable.ColumnBestRates;
import ch.prokopovi.exported.RatesPlacesTable;
import ch.prokopovi.exported.RatesPlacesTable.ColumnRatesPlaces;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.OperationType;
import ch.prokopovi.struct.Master.Region;
import ch.prokopovi.struct.best.RateItem;
import ch.prokopovi.struct.best.RatePoint;

public class BestRatesDbAdapter {

	private static final String LOG_TAG = "BestRatesDbAdapter";

	private static final String PLACES_SHORTCUT = "b";

	private static final String CURRENCY_EXCHANGE_TYPE_QUERY = " SELECT "
			+ ColumnBestRates.BEST_RATES_ID
			+ " _id, "
			+ ColumnBestRates.BEST_RATES_ID
			+ "," //
			+ ColumnBestRates.VALUE
			+ "," //
			+ ColumnBestRates.CURRENCY_ID
			+ "," //
			+ PLACES_SHORTCUT
			+ "."
			+ ColumnRatesPlaces.REGION_ID
			+ "," //
			+ PLACES_SHORTCUT
			+ "."
			+ ColumnRatesPlaces.RATES_PLACE_ID
			+ ", " //
			+ ColumnRatesPlaces.DESCRIPTION
			+ "," //
			+ ColumnRatesPlaces.X
			+ "," //
			+ ColumnRatesPlaces.Y
			+ "," //
			+ ColumnRatesPlaces.ADDR
			+ "," //
			+ ColumnRatesPlaces.WORK_HOURS
			+ "," //
			+ ColumnRatesPlaces.PHONE
			+ "," //

			+ "strftime('%d-%m %H:%M', "
			+ ColumnBestRates.TIME_UPDATED
			+ "/1000, 'unixepoch', 'localtime') AS "
			+ ColumnBestRates.TIME_UPDATED
			+ "," //

			+ ColumnBestRates.EXCHANGE_TYPE_ID //

			+ " FROM " + BestRatesTable.TABLE + " a " + " LEFT JOIN "
			+ RatesPlacesTable.TABLE + " " + PLACES_SHORTCUT + " ON a."
			+ ColumnBestRates.RATES_PLACE_ID + "=" + PLACES_SHORTCUT + "."
			+ ColumnRatesPlaces.RATES_PLACE_ID

			+ " WHERE " + PLACES_SHORTCUT + "." + ColumnRatesPlaces.REGION_ID
			+ "=?" //
			+ " AND " + ColumnBestRates.EXCHANGE_TYPE_ID + "=?" //
			+ " AND " + ColumnBestRates.CURRENCY_ID + "=?" //
	;

	private static final String ORDER_LIMIT_APPENDIX_FMT = " ORDER BY "
			+ ColumnBestRates.VALUE + "	%s, " + ColumnBestRates.BEST_RATES_ID
			+ " LIMIT %d";

	private static final String LAST_UPDATE_TIME_QUERY = " SELECT MAX(a."
			+ ColumnBestRates.TIME_UPDATED
			+ ") " //
			+ " FROM  " + BestRatesTable.TABLE + " a JOIN "
			+ RatesPlacesTable.TABLE + " b ON a."
			+ ColumnBestRates.RATES_PLACE_ID + "=b."
			+ ColumnRatesPlaces.RATES_PLACE_ID //
			+ " WHERE b." + ColumnRatesPlaces.REGION_ID + " = ?";

	private static final String REGION_BEST_RATES_QUERY = "SELECT "
			+ ColumnBestRates.CURRENCY_ID
			+ ", "
			+ ColumnBestRates.EXCHANGE_TYPE_ID
			+ ", " //
			+ " CASE WHEN " + ColumnBestRates.EXCHANGE_TYPE_ID
			+ " = 1 THEN MAX(a."
			+ ColumnBestRates.VALUE
			+ ") ELSE MIN(a."
			+ ColumnBestRates.VALUE
			+ ") END "
			+ ColumnBestRates.VALUE //
			+ " FROM " + RatesPlacesTable.TABLE + " b inner JOIN "
			+ BestRatesTable.TABLE + " a ON a."
			+ ColumnBestRates.RATES_PLACE_ID
			+ " = b."
			+ ColumnRatesPlaces.RATES_PLACE_ID //
			+ " WHERE " + ColumnRatesPlaces.REGION_ID + " = ?"
			+ //
			" GROUP BY " + ColumnBestRates.CURRENCY_ID + ", "
			+ ColumnBestRates.EXCHANGE_TYPE_ID;

	private static final String INSERT_RATE = "INSERT INTO "
			+ BestRatesTable.TABLE + " (" + ColumnBestRates.CURRENCY_ID + ","
			+ ColumnBestRates.EXCHANGE_TYPE_ID + ","
			+ ColumnBestRates.RATES_PLACE_ID + ","
			+ ColumnBestRates.TIME_UPDATED + "," + ColumnBestRates.VALUE
			+ ") VALUES (?,?,?,?,?);";

	private static final String DELETE_REGION_RATES = //
	" delete from "
			+ BestRatesTable.TABLE
			+ " where "
			+ ColumnBestRates.RATES_PLACE_ID //
			+ " in (select p." + ColumnRatesPlaces.RATES_PLACE_ID + " from "
			+ RatesPlacesTable.TABLE + " p where p."
			+ ColumnRatesPlaces.REGION_ID + " = ?);";

	private final SQLiteDatabase database;

	private final RatesPlacesDbAdapter placesDbAdapter;

	public BestRatesDbAdapter(SQLiteDatabase database) {
		this.database = database;
		this.placesDbAdapter = new RatesPlacesDbAdapter(database);
	}

	/**
	 * creating/updating best rates records (updating in case same-day-record
	 * found )
	 * 
	 * @param records
	 */
	public void createOrUpdate(Region region,
			List<Entry<Long, BestRatesRecord>> records) {

		if (records == null || records.size() == 0) {
			Log.w(LOG_TAG, "empty list will not update");
			return;
		}

		long start = new Date().getTime();

		final SQLiteStatement statement = this.database
				.compileStatement(INSERT_RATE);

		this.database.beginTransaction();
		try {

			this.database.execSQL(DELETE_REGION_RATES,
					new Object[] { region.getId() });

			long deletePoint = new Date().getTime();
			Log.d(LOG_TAG, "db delete old region time spent: "
					+ (deletePoint - start));

			Set<Long> placeIds = this.placesDbAdapter.existingPlaceIds();
			long placeIdsPoint = new Date().getTime();
			Log.d(LOG_TAG, "db get place ids time spent: "
					+ (placeIdsPoint - deletePoint));

			// time in mills, equal for every rate in same group
			long bunchTimeUpdated = new Date().getTime();

			for (Entry<Long, BestRatesRecord> record : records) {

				Long placeId = record.getKey();

				boolean placeExists = placeIds.contains(placeId);
				if (!placeExists) {
					Log.w(LOG_TAG, "rate place with id [" + placeId
							+ "] does not exist. Skipped.");
					continue;
				}

				BestRatesRecord rate = record.getValue();
				int currencyId = rate.getCurrencyId();
				int exchangeTypeId = rate.getExchangeTypeId();
				Double value = rate.getValue();

				statement.clearBindings();
				statement.bindLong(1, currencyId);
				statement.bindLong(2, exchangeTypeId);
				statement.bindLong(3, placeId);
				statement.bindLong(4, bunchTimeUpdated);
				statement.bindDouble(5, value);

				statement.executeInsert();
			}

			this.database.setTransactionSuccessful();

			long spent = new Date().getTime() - placeIdsPoint;
			Log.d(LOG_TAG, "db insert time spent: " + spent);

		} finally {
			this.database.endTransaction();
		}
	}

	/**
	 * build sql 'like conditions' for search. pair of likes is used to diminish
	 * no-letter-case for non-ASCI characters
	 * 
	 * @param tablePrefix
	 *            sql table shortcut
	 * @param columns
	 *            columns to use in 'like condition'
	 * @return
	 */
	private String buildSearchSuffix(String tablePrefix,
			ColumnRatesPlaces... columns) {
		String tp = tablePrefix + ".";

		StringBuilder sb = new StringBuilder(" AND (");

		int len = columns.length;
		for (int i = 0; i < len; i++) {
			ColumnRatesPlaces column = columns[i];

			sb.append(tp).append(column).append(" LIKE ? OR ")//
					.append(column).append(" LIKE ? ");

			if (i != len - 1) {
				sb.append(" OR ");
			} // do not add for last statement
		}

		sb.append(" )");

		return sb.toString();
	}

	/**
	 * append pair of searchQuery params to the end of params array
	 * 
	 * @param params
	 *            collection to append pair of params to
	 * @param searchQuery
	 *            initial query string to make pair of params
	 * @param times
	 *            number of times to append pair of params
	 */
	private void tailSearchParams(Collection<String> params,
			String searchQuery, int times) {

		String likeStr = "%" + searchQuery + "%";
		String likeUpStr = "%"
				+ searchQuery.substring(0, 1).toUpperCase(Locale.US)
				+ searchQuery.substring(1) + "%";

		for (int i = 0; i < times; i++) {
			params.add(likeStr);
			params.add(likeUpStr);
		}
	}

	public Cursor fetch(Region region, OperationType operationType,
			CurrencyCode currencyCode, String searchQuery, int limit) {

		if (region == null || operationType == null || currencyCode == null)
			throw new NullPointerException(
					"no nulls allowed in query filter params.");

		final String strRegionId = String.valueOf(region.getId());
		final String strOperationTypeId = String.valueOf(operationType.getId());
		final String strCurrencyCodeId = String.valueOf(currencyCode.getId());

		Collection<String> paramsList = new ArrayList<String>();
		paramsList.add(strRegionId);
		paramsList.add(strOperationTypeId);
		paramsList.add(strCurrencyCodeId);

		String searchSuffix = "";
		if (searchQuery != null) {

			ColumnRatesPlaces[] likeColumns = new ColumnRatesPlaces[] {
					ColumnRatesPlaces.ADDR, ColumnRatesPlaces.DESCRIPTION };

			searchSuffix = buildSearchSuffix(PLACES_SHORTCUT, likeColumns);

			tailSearchParams(paramsList, searchQuery, likeColumns.length);

		}

		String[] params = paramsList.toArray(new String[paramsList.size()]);

		Log.d(LOG_TAG, "fetch. params: " + Arrays.toString(params));

		String appendix = String
				.format(ORDER_LIMIT_APPENDIX_FMT, OperationType.BUY
						.equals(operationType) ? "desc" : "asc", limit);

		String sql = CURRENCY_EXCHANGE_TYPE_QUERY + searchSuffix + appendix;
		// Log.d(LOG_TAG, "sql: " + sql);

		Cursor cursor = this.database.rawQuery(sql, params);

		Log.d(LOG_TAG, cursor.getCount() + " records fetched");

		return cursor;
	}

	public List<RateItem> fetchRates(ColumnBestRates filterCol, int filterVal) {

		String[] params = new String[] { String.valueOf(filterVal) };

		Log.d(LOG_TAG, "fetchRates. params: " + Arrays.toString(params));

		Cursor cursor = this.database.query(
				BestRatesTable.TABLE,
				ColumnBestRates.getAllNames(), //
				filterCol + " = ?", params, null, null,
				ColumnBestRates.BEST_RATES_ID.name());

		List<RateItem> items = new ArrayList<RateItem>();
		while (cursor.moveToNext()) {

			RateItem point = convertItem(cursor);

			items.add(point);
		}

		cursor.close();

		Log.d(LOG_TAG, items.size() + " records fetched");

		return items;
	}

	public Map<CurrencyCode, Map<OperationType, Double>> fetchBestRates(
			Region region) {

		if (region == null)
			throw new NullPointerException(
					"no nulls allowed in query filter params.");

		String[] params = new String[] { String.valueOf(region.getId()) };

		Log.d(LOG_TAG, "fetchBestRates. params: " + Arrays.toString(params));

		Cursor cursor = this.database.rawQuery(REGION_BEST_RATES_QUERY, params);

		Map<CurrencyCode, Map<OperationType, Double>> best = new LinkedHashMap<CurrencyCode, Map<OperationType, Double>>();
		while (cursor.moveToNext()) {

			int currencyId = cursor.getInt(cursor
					.getColumnIndex(ColumnBestRates.CURRENCY_ID.name()));
			int operationId = cursor.getInt(cursor
					.getColumnIndex(ColumnBestRates.EXCHANGE_TYPE_ID.name()));
			Double rateValue = cursor.getDouble(cursor
					.getColumnIndex(ColumnBestRates.VALUE.name()));

			CurrencyCode currency = CurrencyCode.get(currencyId);
			OperationType operationType = OperationType.get(operationId);

			Map<OperationType, Double> currMap = best.get(currency);
			if (currMap == null) {
				currMap = new LinkedHashMap<OperationType, Double>();
				best.put(currency, currMap);
			}

			currMap.put(operationType, rateValue);
		}

		cursor.close();

		Log.d(LOG_TAG, best.size() + " records fetched");

		return best;
	}

	public SparseArray<RatePoint> fetchPoints(Region region) {
		if (region == null)
			throw new NullPointerException(
					"no nulls allowed in query filter params.");

		String[] params = new String[] { String.valueOf(region.getId()) };

		Log.d(LOG_TAG, "fetchPoints. params: " + Arrays.toString(params));

		Cursor cursor = this.database.query(RatesPlacesTable.TABLE,
				ColumnRatesPlaces.getAllNames(), //
				ColumnRatesPlaces.REGION_ID + " = ?", params, null, null, null);

		SparseArray<RatePoint> points = new SparseArray<RatePoint>();
		while (cursor.moveToNext()) {

			RatePoint point = convertPoint(cursor);

			points.put(point.id, point);
		}

		cursor.close();

		Log.d(LOG_TAG, points.size() + " records fetched");

		return points;
	}

	private static RateItem convertItem(Cursor cursor) {

		int itemId = cursor.getInt(cursor
				.getColumnIndex(ColumnBestRates.BEST_RATES_ID.name()));

		int placeId = cursor.getInt(cursor
				.getColumnIndex(ColumnBestRates.RATES_PLACE_ID.name()));

		// value
		int valueIndex = cursor.getColumnIndex(ColumnBestRates.VALUE.name());
		boolean valueIsNull = cursor.isNull(valueIndex);
		Double value = !valueIsNull ? cursor.getDouble(valueIndex) : null;

		// time
		int timeIndex = cursor.getColumnIndex(ColumnBestRates.TIME_UPDATED
				.name());
		boolean timeIsNull = cursor.isNull(timeIndex);
		Long timeUpdated = !timeIsNull ? cursor.getLong(timeIndex) : null;

		// currency
		int currIndex = cursor.getColumnIndex(ColumnBestRates.CURRENCY_ID
				.name());
		boolean currIsNull = cursor.isNull(currIndex);
		Integer currId = !currIsNull ? cursor.getInt(currIndex) : null;

		// exch type
		int exTypeIndex = cursor
				.getColumnIndex(ColumnBestRates.EXCHANGE_TYPE_ID.name());
		boolean exTypeIsNull = cursor.isNull(exTypeIndex);
		Integer etId = !exTypeIsNull ? cursor.getInt(exTypeIndex) : null;

		return new RateItem(itemId, placeId, CurrencyCode.get(currId),
				OperationType.get(etId), value, timeUpdated);
	}

	private static RatePoint convertPoint(Cursor cursor) {

		String placeDescription = cursor.getString(cursor
				.getColumnIndex(ColumnRatesPlaces.DESCRIPTION.name()));
		Double x = cursor.getDouble(cursor.getColumnIndex(ColumnRatesPlaces.X
				.name()));
		Double y = cursor.getDouble(cursor.getColumnIndex(ColumnRatesPlaces.Y
				.name()));

		String addr = cursor.getString(cursor
				.getColumnIndex(ColumnRatesPlaces.ADDR.name()));
		String workHours = cursor.getString(cursor
				.getColumnIndex(ColumnRatesPlaces.WORK_HOURS.name()));
		String phones = cursor.getString(cursor
				.getColumnIndex(ColumnRatesPlaces.PHONE.name()));

		int placeId = cursor.getInt(cursor
				.getColumnIndex(ColumnRatesPlaces.RATES_PLACE_ID.name()));

		int regionId = cursor.getInt(cursor
				.getColumnIndex(ColumnRatesPlaces.REGION_ID.name()));

		int bankIdIndex = cursor.getColumnIndex(ColumnRatesPlaces.BANK_ID
				.name());
		boolean bankIsNull = cursor.isNull(bankIdIndex);
		Integer bankId = !bankIsNull ? cursor.getInt(bankIdIndex) : null;

		return new RatePoint(placeId, regionId, bankId, placeDescription, x, y,
				addr, workHours, phones);
	}

	public Double fetchWorstRate(Region region, CurrencyCode currency,
			OperationType operation) {

		Double worstValue = null;

		String function = OperationType.BUY.equals(operation) ? "MIN" : "MAX";

		Cursor cursor = this.database.rawQuery(
				"select " + function + "(r." + ColumnBestRates.VALUE + ") "
						+ " from " + BestRatesTable.TABLE + " r join "
						+ RatesPlacesTable.TABLE + " p on r."
						+ ColumnBestRates.RATES_PLACE_ID + " = p."
						+ ColumnRatesPlaces.RATES_PLACE_ID + " where p."
						+ ColumnRatesPlaces.REGION_ID + " = ? " + " and r."
						+ ColumnBestRates.CURRENCY_ID
						+ " = ? " //
						+ " and r." + ColumnBestRates.EXCHANGE_TYPE_ID
						+ " = ? ",
				new String[] { String.valueOf(region.getId()),
						String.valueOf(currency.getId()),
						String.valueOf(operation.getId()) });

		if (cursor.moveToFirst()) {
			worstValue = cursor.getDouble(0);
			Log.d(LOG_TAG, "worst rate value for " + region + ", " + currency
					+ ", " + operation + " is " + worstValue);
		}

		cursor.close();

		return worstValue;
	}

	public Long fetchLastUpdateTime(Region region) {
		Long lastTime = null;

		Cursor cursor = this.database.rawQuery(LAST_UPDATE_TIME_QUERY,
				new String[] { String.valueOf(region.getId()) });

		if (cursor.moveToFirst()) {
			lastTime = cursor.getLong(0);
			Log.d(LOG_TAG, "last update for " + region + " was at " + lastTime);
		}

		cursor.close();

		return lastTime;
	}
}
