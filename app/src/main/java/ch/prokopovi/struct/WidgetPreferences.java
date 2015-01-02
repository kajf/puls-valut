package ch.prokopovi.struct;

import java.util.LinkedHashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.ProviderCode;
import ch.prokopovi.struct.Master.RateType;

public class WidgetPreferences {
	private enum PropKey {
		W_ID, S, CS, T;
	}

	private int widgetId;
	private ProviderCode providerCode;
	private RateType rateType;

	// do NOT use the property (may be null). access through getter
	private Set<CurrencyCode> currencyCodes;

	public WidgetPreferences() {
	}

	/**
	 * load data from string
	 * 
	 * @param input
	 * @throws JSONException
	 */
	public WidgetPreferences(String input) throws JSONException {
		JSONObject json = new JSONObject(input);

		widgetId = json.optInt(PropKey.W_ID.name(), 0);

		String provider = json.optString(PropKey.S.name(), ProviderCode
				.getDefault().name());
		providerCode = ProviderCode.get(provider);

		String type = json.optString(PropKey.T.name(), RateType.getDefault()
				.name());
		rateType = RateType.get(type);

		getCurrencyCodes().clear();
		JSONArray jsonArray = json.getJSONArray(PropKey.CS.name());
		for (int i = 0; i < jsonArray.length(); i++) {
			String currency = (String) jsonArray.get(i);

			CurrencyCode currencyCode = CurrencyCode.get(currency);
			getCurrencyCodes().add(currencyCode);
		}

	}

	public Set<CurrencyCode> getCurrencyCodes() {
		if (currencyCodes == null)
			currencyCodes = new LinkedHashSet<CurrencyCode>();

		return currencyCodes;
	}

	public ProviderCode getProviderCode() {
		return providerCode;
	}

	public RateType getRateType() {
		return rateType;
	}

	public int getWidgetId() {
		return widgetId;
	}

	public void setProviderCode(ProviderCode providerCode) {
		this.providerCode = providerCode;
	}

	public void setRateType(RateType rateType) {
		this.rateType = rateType;
	}

	public void setWidgetId(int widgetId) {
		this.widgetId = widgetId;
	}

	/**
	 * pack data to string
	 * 
	 * @return
	 * @throws JSONException
	 */
	public String to() throws JSONException {
		JSONObject json = new JSONObject();

		json.put(PropKey.W_ID.name(), getWidgetId());
		json.put(PropKey.S.name(), getProviderCode().name());

		RateType rt = getRateType();
		if (rt != null)
			json.put(PropKey.T.name(), rt.name());

		JSONArray jsonCurrencies = new JSONArray();
		for (CurrencyCode code : getCurrencyCodes()) {
			jsonCurrencies.put(code.name());
		}

		json.put(PropKey.CS.name(), jsonCurrencies);

		String res = json.toString();

		return res;
	}

	@Override
	public String toString() {
		return "WidgetPreferences [widgetId=" + widgetId + ", providerCode="
				+ providerCode + ", rateType=" + rateType + ", currencyCodes="
				+ currencyCodes + "]";
	}

}
