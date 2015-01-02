package ch.prokopovi.db;

import java.util.HashSet;
import java.util.Set;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import ch.prokopovi.exported.RatesPlacesTable;
import ch.prokopovi.exported.RatesPlacesTable.ColumnRatesPlaces;

public class RatesPlacesDbAdapter {

	private static final String LOG_TAG = "Places DB Adapter";

	private final SQLiteDatabase database;

	public RatesPlacesDbAdapter(SQLiteDatabase database) {
		this.database = database;
	}

	public Cursor fetchAll() {
		Cursor cursor = this.database.query(RatesPlacesTable.TABLE, null, null,
				null, null, null, null);
		return cursor;
	}

	public Set<Long> existingPlaceIds() {

		Cursor cursor = this.database.query(RatesPlacesTable.TABLE,
				new String[] { ColumnRatesPlaces.RATES_PLACE_ID + " as _id" },
				null, null, null, null, null);

		Set<Long> res = new HashSet<Long>();
		while (cursor.moveToNext()) {

			long placeId = cursor.getLong(0);

			res.add(placeId);
		}

		cursor.close();

		Log.d(LOG_TAG, res.size() + " exiting ids fetched");

		return res;
	}
}
