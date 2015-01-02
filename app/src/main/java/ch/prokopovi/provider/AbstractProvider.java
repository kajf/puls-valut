package ch.prokopovi.provider;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.htmlcleaner.TagNode;

import android.util.Log;
import ch.prokopovi.Util;
import ch.prokopovi.api.provider.Provider;
import ch.prokopovi.api.struct.ProviderRate;
import ch.prokopovi.err.WebUpdatingException;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.OperationType;
import ch.prokopovi.struct.Master.ProviderCode;
import ch.prokopovi.struct.Master.RateType;
import ch.prokopovi.struct.ProviderRateBuilder;
import ch.prokopovi.struct.ProviderRequirements;

abstract class AbstractProvider implements Provider {

	private static final String LOG_TAG = "AbstractProvider";

	/**
	 * post data request
	 * 
	 * @param url
	 * @param params
	 * @return
	 * @throws WebUpdatingException
	 */
	public static String post(String url, String[][] params)
			throws WebUpdatingException {

		Log.d(LOG_TAG, "<<< POST " + url);

		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(url);

		try {
			// Add your data
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			for (int i = 0; i < params.length; i++) {
				nameValuePairs.add(new BasicNameValuePair(params[i][0],
						params[i][1]));
				Log.d(LOG_TAG, "<<< PARAM " + params[i][0] + " " + params[i][1]);
			}

			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			// Execute HTTP Post Request
			HttpResponse response = httpclient.execute(httppost);

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent(), "UTF-8"));
			StringBuilder builder = new StringBuilder();
			for (String line = null; (line = reader.readLine()) != null;) {
				builder.append(line);
			}

			Log.d(LOG_TAG, "response data: " + builder);

			return builder.toString();

		} catch (Exception e) {
			Log.e(LOG_TAG, "web load error");
			throw new WebUpdatingException();
		}
	}

	/**
	 * list supported rate types. ancestors must override if support more then
	 * cash rate type
	 * 
	 * @return
	 */
	@Override
	public RateType[] getSupportedRateTypes() {
		return new RateType[] { RateType.CASH };
	}

	interface CurrencyCodable {
		CurrencyCode getCurrencyCode();
	}

	abstract CurrencyCodable[] getCurrencyCodables(RateType rt);

	@Override
	public CurrencyCode[] getSupportedCurrencyCodes(RateType rt) {

		CurrencyCodable[] values = getCurrencyCodables(rt);
		CurrencyCode[] res = new CurrencyCode[values.length];

		for (int i = 0; i < values.length; i++) {
			CurrencyCodable currencyCodable = values[i];
			res[i] = currencyCodable.getCurrencyCode();
		}

		return res;
	}

	/**
	 * collect value from node by path and parse them to double
	 * 
	 * @param root
	 *            node to evaluate xpath on
	 * @param path
	 *            xpath expression (should result a text() values of double )
	 * @param commaSepartor
	 *            true - comma decimal separator expected, false - dot separtor
	 * 
	 * @return two-elements array
	 * @throws Exception
	 */
	protected double extractValue(TagNode root, String path,
			boolean commaSepartor) throws Exception {
		double res = -1;

		Object[] val = root.evaluateXPath(path);

		if (val != null && val.length > 0) {
			String str = val[0].toString();
			Log.d(LOG_TAG, "str from xpath: " + str);
			double dbl = commaSepartor ? Util.parseCommaDouble(str) : Util
					.parseDotDouble(str);
			res = dbl;
		}

		return res;
	}

	/**
	 * assemble records list from two-element arrays by builder
	 * 
	 * @param builder
	 * @param currencyCode
	 * @param buy
	 * @param sell
	 * @return
	 */
	protected List<ProviderRate> assembleProviderRates(
			ProviderRateBuilder builder, CurrencyCode currencyCode, double buy,
			double sell) {

		if (builder == null || currencyCode == null) {
			Log.d(LOG_TAG, "invalid input for assembleProviderRates");
			return Collections.emptyList();
		}

		if (buy < 0.00001 || sell < 0.00001) {
			Log.d(LOG_TAG, "invalid values for assembleProviderRates");
			return Collections.emptyList(); // invalid rate
		}

		List<ProviderRate> res = new ArrayList<ProviderRate>();

		res.add(builder.build(OperationType.BUY, currencyCode, buy));
		res.add(builder.build(OperationType.SELL, currencyCode, sell));

		return res;
	}

	/**
	 * preliminary check of input
	 * 
	 * @param requirements
	 * @param providerCode
	 * 
	 * @return true - if requirements are Ok, false - otherwise
	 */
	private static boolean isValid(ProviderRequirements requirements,
			ProviderCode providerCode) {
		if (requirements == null) {
			Log.e(LOG_TAG, "requirements should not be null");
			return false;
		}

		ProviderCode provider = requirements.getProviderCode();
		if (!providerCode.equals(provider)) {
			Log.e(LOG_TAG, "wrong provider from requirements");
			return false;
		}

		Log.d(LOG_TAG, "requirements are Ok");

		return true;
	}

	/**
	 * provider of particular implementation
	 * 
	 * @return
	 */
	protected abstract ProviderCode getProviderCode();

	/**
	 * request the rates from particular web-service
	 * 
	 * @param requirements
	 *            rate requirements
	 * @param now
	 *            date to request rates for
	 * @param builder
	 *            rates records builder
	 * 
	 * @return list of rates records
	 */
	protected abstract List<ProviderRate> requestRates(
			ProviderRequirements requirements, Date now,
			ProviderRateBuilder builder) throws WebUpdatingException;

	@Override
	public List<ProviderRate> update(ProviderRequirements requirements)
			throws WebUpdatingException {

		ProviderCode provider = getProviderCode();

		boolean valid = isValid(requirements, provider);
		if (!valid) {
			return Collections.emptyList();
		}

		Date now = provider.getLastValidDay();
		Log.d(LOG_TAG, "Last [" + provider + "] Valid Day: " + now);

		RateType rateType = requirements.getRateType();
		ProviderRateBuilder builder = new ProviderRateBuilder(provider,
				rateType, now.getTime());

		List<ProviderRate> res = requestRates(requirements, now, builder);

		Log.d(LOG_TAG, ">>> res size " + res.size());

		return res;
	}
}
