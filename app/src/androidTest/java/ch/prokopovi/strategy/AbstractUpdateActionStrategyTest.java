package ch.prokopovi.strategy;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.test.AndroidTestCase;
import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.OperationType;
import ch.prokopovi.struct.Master.ProviderCode;
import ch.prokopovi.struct.Master.RateType;
import ch.prokopovi.struct.SimpleProviderRate;

public class AbstractUpdateActionStrategyTest extends AndroidTestCase {
	private static final String LOG_TAG = "AbstractUpdateActionStrategyTest";

	/** build mock rates with random timeUpdated field but before specified max date. at least one rate should have timeUpdated equal to max date.
	 * 
	 * @param size rates list size
	 * @param maxDate max date
	 * @return
	 */
	private static List<ProviderRate> prepareMockRates(int size, Date maxDate){
		ArrayList<ProviderRate> rates = new ArrayList<ProviderRate>();

		if (size < 1) return rates;

		for (int i = 0; i < size; i++) {
			double random = Math.random();

			int hours = (int) (12*random);

			Calendar calendar = Calendar.getInstance();
			calendar.setTime(maxDate);
			calendar.add(Calendar.HOUR, -hours);
			long time = calendar.getTime().getTime();

			SimpleProviderRate rate = new SimpleProviderRate(
					ProviderCode.PRIOR, //
					RateType.CASH, //
					(i % 2 == 0) ? OperationType.BUY : OperationType.SELL, //
							CurrencyCode.USD, 2.5, time, time);
			rates.add(rate);
		}

		long maxTime = maxDate.getTime();
		SimpleProviderRate rateMaxDate = new SimpleProviderRate(
				ProviderCode.PRIOR, //
				RateType.CASH, //
				OperationType.BUY, //
				CurrencyCode.USD, 3.5, maxTime, maxTime);

		int randomMaxDatePlace = (int) (size*Math.random());

		rates.set(randomMaxDatePlace, rateMaxDate);

		return rates;
	}

	public void testIsExpired() {

		Date now = new Date();


		// check empty
		List<ProviderRate> mockRates0 = prepareMockRates(0, now);
		boolean expired0 = AbstractUpdateActionStrategy.isExpired(mockRates0);
		assertTrue("empty rates are not expired", expired0);

		// check new
		List<ProviderRate> mockRatesNew = prepareMockRates(8, now);
		boolean expiredNew = AbstractUpdateActionStrategy.isExpired(mockRatesNew);
		assertFalse("new rates are expired", expiredNew);

		// check old
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(now);
		calendar.add(Calendar.HOUR, -10);
		Date timeOld = calendar.getTime();

		List<ProviderRate> mockRatesOld = prepareMockRates(6, timeOld);
		boolean expiredOld = AbstractUpdateActionStrategy.isExpired(mockRatesOld);
		assertTrue("old rates are not expired", expiredOld);

	}
}
