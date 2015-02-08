package ch.prokopovi.ui.main;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.androidannotations.annotations.AfterTextChange;
import org.androidannotations.annotations.BeforeTextChange;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import ch.prokopovi.R;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.OperationType;
import ch.prokopovi.struct.Master.Region;
import ch.prokopovi.ui.main.api.Closable;

@EFragment(R.layout.converter_layout)
public class ConverterFragment extends Fragment {

	private static final String LOG_TAG = "ConverterFragment";

	private static final DecimalFormat FMT_RATE_VALUE = new DecimalFormat(
			"###,###,###.####");

	private static final Map<CurrencyCode, String> CURRENCY_INITIAL_AMOUNTS = new HashMap<CurrencyCode, String>();
	static {
		CURRENCY_INITIAL_AMOUNTS.put(CurrencyCode.BYR, "500000");
		CURRENCY_INITIAL_AMOUNTS.put(CurrencyCode.UAH, "1000");

		CURRENCY_INITIAL_AMOUNTS.put(CurrencyCode.USD, "100");
		CURRENCY_INITIAL_AMOUNTS.put(CurrencyCode.EUR, "100");
		CURRENCY_INITIAL_AMOUNTS.put(CurrencyCode.RUR, "3500");
		CURRENCY_INITIAL_AMOUNTS.put(CurrencyCode.PLN, "300");
		CURRENCY_INITIAL_AMOUNTS.put(CurrencyCode.LTL, "250");
		CURRENCY_INITIAL_AMOUNTS.put(CurrencyCode.EUR_USD, "100");

		CURRENCY_INITIAL_AMOUNTS.put(CurrencyCode.CHF, "100");
		CURRENCY_INITIAL_AMOUNTS.put(CurrencyCode.GBP, "100");
	}

	public static class ConverterParams {
		private Region region;
		private CurrencyCode currency;
		private OperationType operationType;
		private Double rate;
		private Double altRate;

		private ConverterParams() {
		}

		public static ConverterParams instaniate(Region region,
				CurrencyCode currency, OperationType operationType,
				Double rate, Double altRate) {

			ConverterParams res = new ConverterParams();

			res.region = region;
			res.currency = currency;
			res.operationType = operationType;
			res.rate = rate;
			res.altRate = altRate != null ? altRate : rate;

			return res;
		}

	}

	@ViewById(R.id.ib_conv_close)
	ImageButton ibClose;

	@ViewById(R.id.et_i_have)
	EditText etIhave;

	@ViewById(R.id.tv_i_have_curr)
	TextView tvIhaveCurr;

	@ViewById(R.id.tv_conv_rate)
	TextView tvConvRate;

	@ViewById(R.id.tv_conv_i_will_get)
	TextView tvIwillGet;

	@ViewById(R.id.tv_conv_i_will_get_curr)
	TextView tvIwillGetCurr;

	@ViewById(R.id.tv_conv_profit)
	TextView tvConvProfit;

	@ViewById(R.id.tv_conv_profit_curr)
	TextView tvConvProfitCurr;

	private Closable closable;

	private ConverterParams params;

	private boolean isUserValue = false;

	public void setParams(ConverterParams params) {
		this.params = params;
	}

	@Override
	public void onAttach(Activity activity) {

		Log.d(LOG_TAG, "onAttach");

		super.onAttach(activity);

		// This makes sure that the container activity has implemented
		// the callback interface. If not, it throws an exception
		try {

			this.closable = (Closable) activity;

		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement Updater");
		}
	}

	private CurrencyCode getFirstCurrency() {

		if (CurrencyCode.EUR_USD.equals(this.params.currency)) {
			return CurrencyCode.EUR;
		} else {
			return this.params.currency;
		}
	}

	private CurrencyCode getSecondCurrency() {

		if (CurrencyCode.EUR_USD.equals(this.params.currency)) {
			return CurrencyCode.USD;
		} else {
			return this.params.region.getCountryCode().getCurrency();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		reload();
	}

	void reload() {
		Log.d(LOG_TAG, "reloading converter...");

		if (this.params == null) {
			Log.w(LOG_TAG, "converter params do not exist");
			return;
		}

		if (!isVisible()) {
			Log.w(LOG_TAG, "converter view is not visible");
			return;
		} // in case if method called separately (not on resume)

		final CurrencyCode mainCurrency = getFirstCurrency();
		final OperationType operType = this.params.operationType;
		final Double rate = this.params.rate;

		CurrencyCode secondCurrency = getSecondCurrency();

		CurrencyCode currencyFrom = secondCurrency;
		CurrencyCode currencyTo = mainCurrency;

		if (OperationType.BUY.equals(operType)) {
			currencyFrom = mainCurrency;
			currencyTo = secondCurrency;
		}

		if (!this.isUserValue) {
			String initialAmount = CURRENCY_INITIAL_AMOUNTS.get(currencyFrom);
			this.etIhave.setText(initialAmount);
		}

		this.tvIhaveCurr.setText(currencyFrom.name());

		this.tvConvRate.setText(FMT_RATE_VALUE.format(rate));

		this.tvIwillGetCurr.setText(currencyTo.name());
		this.tvConvProfitCurr.setText(currencyTo.name());

		UiHelper.applyFont(getActivity(),
				getActivity().findViewById(android.R.id.content), null);
	}

	@BeforeTextChange(R.id.et_i_have)
	void beforeInputSumChange(TextView tv, CharSequence text, int start,
			int count, int after) {

		// one char modified at a time
		this.isUserValue = (count == 0 || count == 1)
				&& (after == 0 || after == 1);
	}

	@AfterTextChange({ R.id.et_i_have, R.id.tv_conv_rate })
	void afterInputChange(TextView tv, Editable text) {

		final Double rate = this.params.rate;
		final Double altRate = this.params.altRate;

		double input = 0d;
		try {
			input = Double.valueOf(this.etIhave.getText().toString());
		} catch (NumberFormatException e) {
		}

		Double iWillGet = input * rate;

		Double profit = iWillGet - input * altRate;

		if (OperationType.SELL.equals(this.params.operationType)) {
			iWillGet = input / rate;
			profit = iWillGet - input / altRate;
		}

		this.tvIwillGet.setText(FMT_RATE_VALUE.format(iWillGet));

		this.tvConvProfit.setText(FMT_RATE_VALUE.format(profit));
	}

	@Click(R.id.ib_conv_close)
	void closeClick() {
		this.closable.close();
	}
}
