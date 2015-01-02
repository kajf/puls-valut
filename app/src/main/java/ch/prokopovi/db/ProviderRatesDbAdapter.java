package ch.prokopovi.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import ch.prokopovi.Util;
import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.db.ProviderRatesTable.ProviderRatesColumn;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.OperationType;
import ch.prokopovi.struct.Master.ProviderCode;
import ch.prokopovi.struct.Master.RateType;
import ch.prokopovi.struct.SimpleProviderRate;

public class ProviderRatesDbAdapter {

	private static final String LOG_TAG = "ProviderRatesDbAdapter";

	private static final String COUPLE_CONDITION = ProviderRatesColumn.PROVIDER_ID
			.name() + "=? and " //
			+ ProviderRatesColumn.RATE_TYPE_ID + "=? and " //
			+ ProviderRatesColumn.EXCHANGE_TYPE_ID + "=? and " //
			+ ProviderRatesColumn.CURRENCY_ID + "=? ";

	private final SQLiteDatabase database;

	public ProviderRatesDbAdapter(SQLiteDatabase database) {
		this.database = database;
	}

	public static List<ProviderRate> read(Context context,
			ProviderCode provider, RateType rateType) {
		DbHelper dbHelper = DbHelper.getInstance(context);
		SQLiteDatabase database = dbHelper.getDb();
		ProviderRatesDbAdapter dbAdapter = new ProviderRatesDbAdapter(database);

		List<ProviderRate> res = dbAdapter.find(provider, rateType);

		return res;
	}

	public List<ProviderRate> find(ProviderCode provider, RateType rateType) {

		List<ProviderRate> res = new ArrayList<ProviderRate>();

		if (provider == null || rateType == null) {
			Log.e(LOG_TAG, "find requires all input params filled");
			return res;
		}

		String strProviderId = String.valueOf(provider.getId());
		String strRateTypeId = String.valueOf(rateType.getId());

		String[] params = new String[] { strProviderId, strRateTypeId };

		Cursor cursor = this.database.query(ProviderRatesTable.TABLE,
				new String[] { ProviderRatesColumn.PROVIDER_RATE_ID.name(),
						ProviderRatesColumn.CURRENCY_ID.name(),
						ProviderRatesColumn.EXCHANGE_TYPE_ID.name(),
						ProviderRatesColumn.VALUE.name(),
						ProviderRatesColumn.TIME_UPDATED.name(),
						ProviderRatesColumn.TIME_EFFECTIVE.name() },
				//
				ProviderRatesColumn.PROVIDER_ID.name() + "=? and " //
						+ ProviderRatesColumn.RATE_TYPE_ID + "=? ",
				//
				params, null, null, ProviderRatesColumn.TIME_EFFECTIVE.name()
						+ " asc");

		while (cursor.moveToNext()) {
			long rateId = cursor
					.getInt(cursor
							.getColumnIndex(ProviderRatesColumn.PROVIDER_RATE_ID
									.name()));

			int currId = cursor.getInt(cursor
					.getColumnIndex(ProviderRatesColumn.CURRENCY_ID.name()));
			int exTypeId = cursor
					.getInt(cursor
							.getColumnIndex(ProviderRatesColumn.EXCHANGE_TYPE_ID
									.name()));

			int valueColumnIndex = cursor
					.getColumnIndex(ProviderRatesColumn.VALUE.name());
			boolean valueIsNull = cursor.isNull(valueColumnIndex);
			Double value = valueIsNull ? null : cursor
					.getDouble(valueColumnIndex);

			long timeUpdated = cursor.getLong(cursor
					.getColumnIndex(ProviderRatesColumn.TIME_UPDATED.name()));

			long timeEffective = cursor.getLong(cursor
					.getColumnIndex(ProviderRatesColumn.TIME_EFFECTIVE.name()));

			ProviderRate tmp = new SimpleProviderRate(rateId, provider,
					rateType, OperationType.get(exTypeId),
					CurrencyCode.get(currId), value, timeUpdated, timeEffective);

			res.add(tmp);
		}

		cursor.close();

		Log.d(LOG_TAG, ((res != null) ? res.size() : null)
				+ " records found for " + provider + ", " + rateType);

		return res;
	}

	/**
	 * get rate history (2 days) ordered by date
	 * 
	 * @param provider
	 * @param rateType
	 * @param operationType
	 * @param currency
	 * @return
	 */
	public List<ProviderRate> getCouple(ProviderCode provider,
			RateType rateType, OperationType operationType,
			CurrencyCode currency) {

		List<ProviderRate> res = new ArrayList<ProviderRate>();

		if (provider == null || rateType == null || operationType == null
				|| currency == null) {
			Log.e(LOG_TAG, "getCouple requires all input params filled");
			return res;
		}

		String strProviderId = String.valueOf(provider.getId());
		String strRateTypeId = String.valueOf(rateType.getId());
		String strExTypeId = String.valueOf(operationType.getId());
		String strCurrencyCodeId = String.valueOf(currency.getId());

		String[] params = new String[] { strProviderId, strRateTypeId,
				strExTypeId, strCurrencyCodeId };

		Cursor cursor = this.database.query(ProviderRatesTable.TABLE,
				new String[] { ProviderRatesColumn.PROVIDER_RATE_ID.name(),
						ProviderRatesColumn.VALUE.name(),
						ProviderRatesColumn.TIME_UPDATED.name(),
						ProviderRatesColumn.TIME_EFFECTIVE.name() },
				//
				COUPLE_CONDITION,
				//
				params, null, null, ProviderRatesColumn.TIME_EFFECTIVE.name()
						+ " asc");

		while (cursor.moveToNext()) {
			long rateId = cursor
					.getLong(cursor
							.getColumnIndex(ProviderRatesColumn.PROVIDER_RATE_ID
									.name()));

			double value = cursor.getDouble(cursor
					.getColumnIndex(ProviderRatesColumn.VALUE.name()));
			long timeUpdated = cursor.getLong(cursor
					.getColumnIndex(ProviderRatesColumn.TIME_UPDATED.name()));

			long timeEffective = cursor.getLong(cursor
					.getColumnIndex(ProviderRatesColumn.TIME_EFFECTIVE.name()));

			ProviderRate item = new SimpleProviderRate(rateId, provider,
					rateType, operationType, currency, value, timeUpdated,
					timeEffective);

			res.add(item);
		}

		cursor.close();

		return res;
	}

	public void save(ProviderRate record) {
		Log.d(LOG_TAG, "attempt to save: " + record);

		if (record == null) {
			Log.w(LOG_TAG, "nothing to save");
			return;
		}

		ProviderCode provider = record.getProvider();
		RateType rateType = record.getRateType();
		OperationType operationType = record.getExchangeType();
		CurrencyCode currency = record.getCurrencyCode();

		ContentValues values = new ContentValues();
		values.put(ProviderRatesColumn.TIME_UPDATED.name(),
				record.getTimeUpdated());
		values.put(ProviderRatesColumn.TIME_EFFECTIVE.name(),
				record.getTimeEffective());
		values.put(ProviderRatesColumn.VALUE.name(), record.getValue());

		List<ProviderRate> couple = getCouple(provider, rateType,
				operationType, currency);
		int len = couple.size();

		if (len < 2) {
			// adding record

			values.put(ProviderRatesColumn.PROVIDER_ID.name(), provider.getId());
			values.put(ProviderRatesColumn.CURRENCY_ID.name(), currency.getId());
			values.put(ProviderRatesColumn.EXCHANGE_TYPE_ID.name(),
					operationType.getId());
			values.put(ProviderRatesColumn.RATE_TYPE_ID.name(),
					rateType.getId());

			long row = this.database.insert(ProviderRatesTable.TABLE, null,
					values);
			Log.d(LOG_TAG, "insert result: " + row);

		} else {

			ProviderRate rate0 = couple.get(0);
			ProviderRate rate1 = couple.get(1);

			Long idToUpadate = null;

			// attempt to find id for update record for same day
			boolean inSameDay1 = Util.isInSameDay(record.getTimeEffective(),
					rate1.getTimeEffective());
			Log.d(LOG_TAG, "inSameDay1: " + inSameDay1);
			if (inSameDay1) {
				idToUpadate = rate1.getId();
			}

			// update eldest record
			if (idToUpadate == null) {
				idToUpadate = rate0.getId();
			}

			String strRecId = String.valueOf(idToUpadate);

			int updated = this.database.update(ProviderRatesTable.TABLE,
					values, ProviderRatesColumn.PROVIDER_RATE_ID + "=? ",
					new String[] { strRecId });
			Log.d(LOG_TAG, "rows updated: " + updated);
		}

	}
}
