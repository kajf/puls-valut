package ch.prokopovi.provider;

import android.support.annotation.*;
import android.util.*;

import org.w3c.dom.*;

import java.io.*;
import java.net.*;
import java.util.*;

import ch.prokopovi.*;
import ch.prokopovi.api.provider.*;
import ch.prokopovi.api.struct.*;
import ch.prokopovi.err.*;
import ch.prokopovi.struct.Master.*;
import ch.prokopovi.struct.*;

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

		try {
			URLConnection urlConn = new URL(url).openConnection();
			urlConn.setDoInput(true);
			urlConn.setDoOutput(true);

			DataOutputStream wr = new DataOutputStream(urlConn.getOutputStream());

			// Add data
			for (int i = 0; i < params.length; i++) {

				String str = params[i][0] + "=" + params[i][1];
				if (i != params.length - 1) {
					str += "&";
				}

				wr.writeBytes(str);

				Log.d(LOG_TAG, "<<< PARAM " + str);
			}

			wr.flush();
			wr.close();

			BufferedReader in = new BufferedReader(
					new InputStreamReader(urlConn.getInputStream()));

			StringBuilder response = new StringBuilder();

			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			Log.d(LOG_TAG, "response data: " + response);

			return response.toString();

		} catch (Exception e) {
			throw new WebUpdatingException("web load error", e);
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

	protected static @Nullable Double extractCommaValue(Node root, String path, Double... defaultVal) throws Exception {
		Double res = (defaultVal.length) > 0 ? defaultVal[0] : null;

		String str = ProviderUtils.evaluateXPath(path, root);

		if (!Util.isBlank(str)) {
			res = Util.parseCommaDouble(str) ;
		}

		return res;
	}

	protected static @Nullable Double extractDotValue(Node root, String path, Double... defaultVal) throws Exception {
		Double res = (defaultVal.length) > 0 ? defaultVal[0] : null;

		String str = ProviderUtils.evaluateXPath(path, root);

		if (!Util.isBlank(str)) {
			res = Util.parseDotDouble(str) ;
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
			ProviderRateBuilder builder, CurrencyCode currencyCode, Double buy,
			Double sell) {

		if (builder == null || currencyCode == null) {
			Log.d(LOG_TAG, "invalid input for assembleProviderRates");
			return Collections.emptyList();
		}

		if (buy == null || sell == null) {
			Log.d(LOG_TAG, "empty values for assembleProviderRates");
			return Collections.emptyList(); // empty rate
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
