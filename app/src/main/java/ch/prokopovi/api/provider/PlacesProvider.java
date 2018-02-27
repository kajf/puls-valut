package ch.prokopovi.api.provider;

import android.content.Context;

import java.util.List;
import java.util.Map.Entry;

import ch.prokopovi.api.struct.BestRatesRecord;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.Region;

public interface PlacesProvider {

	boolean isSupported(Region region);

	boolean isSupported(CurrencyCode currency);

	List<Entry<Long, BestRatesRecord>> getPlaces(Context context, Region region);
}
