package ch.prokopovi.provider.places;

import java.util.List;
import java.util.Map.Entry;

import android.test.AndroidTestCase;
import android.util.Log;
import ch.prokopovi.api.provider.PlacesProvider;
import ch.prokopovi.api.struct.BestRatesRecord;
import ch.prokopovi.err.WebUpdatingException;
import ch.prokopovi.struct.Master.Region;

public class FinanceUaProviderTest extends AndroidTestCase {

    private static final String LOG_TAG = "FinanceUaProviderTest";

    private PlacesProvider provider;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.provider = new FinanceUaPlacesProvider();
    }


    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testUpdate() throws WebUpdatingException {

        List<Entry<Long, BestRatesRecord>> places = this.provider.getPlaces(Region.KIEV);

        for (Entry<Long, BestRatesRecord> entry : places) {

            Log.d(LOG_TAG, "place " + entry.getKey()+ " : "+entry.getValue());
        }

        assertNotNull("result is null", places);
        assertTrue("result is empty", places.size() > 0);
    }
}
