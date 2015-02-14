package ch.prokopovi.ui.main.api;

import java.util.List;
import java.util.Map;

import android.database.Cursor;
import android.location.Location;
import android.util.SparseArray;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.OperationType;
import ch.prokopovi.struct.Master.Region;
import ch.prokopovi.struct.best.RateItem;
import ch.prokopovi.struct.best.RatePoint;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.android.gms.maps.model.LatLng;

public interface Updater {

	void read(Region region, boolean now);

	Location getLocation();

	LatLng getMapPosition();

	Double getWorstRate(Region region, OperationType operation,
			CurrencyCode currency);

	Cursor getData(Region region, OperationType operationType,
			CurrencyCode currencyCode, String searchQuery, int limit);

	SparseArray<RatePoint> getPlaces(Region region);

	Map<CurrencyCode, Map<OperationType, Double>> getBestRates(Region region);

	List<RateItem> getRates(int pointId);

	GoogleAnalyticsTracker getTracker();
}
