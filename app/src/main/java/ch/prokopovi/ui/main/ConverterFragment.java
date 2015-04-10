package ch.prokopovi.ui.main;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.util.Log;
import android.view.*;
import android.widget.*;

import org.androidannotations.annotations.*;

import java.text.DecimalFormat;

import ch.prokopovi.R;
import ch.prokopovi.struct.Master.*;
import ch.prokopovi.ui.main.api.Closable;
import ch.prokopovi.ui.main.resolvers.PaneResolverFactory;

@EFragment
public class ConverterFragment extends Fragment {

	private static final String LOG_TAG = "ConverterFragment";

	private static final DecimalFormat FMT_RATE_VALUE = new DecimalFormat(
			"###,###,###.####");

    public static final String INITIAL_AMOUNT = "100";


    public static class ConverterParams {
		private Region region;
		private CurrencyCode currency;
		private OperationType operationType;
		private Double rate;
		private Double altRate;

		private ConverterParams() {
		}

        public static ConverterParams instantiate(Region region,
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

    @ViewById(R.id.et_amount)
    EditText etAmount;

    @ViewById(R.id.tv_conv_amount_curr)
    TextView tvAmountCurr;

    @ViewById(R.id.tv_conv_rate)
	TextView tvConvRate;

    @ViewById(R.id.tv_conv_lbl_get_or_give)
    TextView tvLblGetOrGive;

    @ViewById(R.id.tv_conv_get_or_give)
    TextView tvGetOrGive;

    @ViewById(R.id.tv_conv_get_curr)
    TextView tvGetCurr;

    @ViewById(R.id.tv_conv_lbl_profit)
    TextView tvConvLblProfit;

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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        boolean isDualPane = PaneResolverFactory.isLargeLayout(getActivity());
        int layout = isDualPane ?
                R.layout.converter_layout_long :
                R.layout.converter_layout;

        return inflater.inflate(layout, container, false);
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

		final OperationType operType = this.params.operationType;
		final Double rate = this.params.rate;


        boolean buy = OperationType.BUY.equals(operType);
        tvLblGetOrGive.setText(buy ? R.string.lbl_conv_i_get : R.string.lbl_conv_i_give);

        if (!this.isUserValue) {
            this.etAmount.setText(INITIAL_AMOUNT);
        }

        this.tvAmountCurr.setText(getFirstCurrency().name());

        this.tvConvRate.setText(FMT_RATE_VALUE.format(rate));

        CurrencyCode secondCurrency = getSecondCurrency();
        this.tvGetCurr.setText(secondCurrency.name());
        this.tvConvProfitCurr.setText(secondCurrency.name());

        UiHelper.applyFont(getActivity(),
				getActivity().findViewById(android.R.id.content), null);
	}

    private CurrencyCode getFirstCurrency() {

        if (CurrencyCode.EUR_USD.equals(this.params.currency)) {
            return CurrencyCode.EUR;
        } else {
            return this.params.currency;
        }
    }

    @Click(R.id.tv_conv_lbl_profit)
    public void clickProfitInfo(View v) {
        Toast.makeText(getActivity(), R.string.lbl_conv_profit_info, Toast.LENGTH_LONG).show();
    }

    @BeforeTextChange(R.id.et_amount)
    void beforeInputSumChange(TextView tv, CharSequence text, int start,
			int count, int after) {

		// one char modified at a time
		this.isUserValue = (count == 0 || count == 1)
				&& (after == 0 || after == 1);
	}

    @AfterTextChange({R.id.et_amount, R.id.tv_conv_rate})
    void afterInputChange(TextView tv, Editable text) {

		final Double rate = this.params.rate;
		final Double altRate = this.params.altRate;

		double input = 0d;
		try {
            input = Double.valueOf(this.etAmount.getText().toString());
        } catch (NumberFormatException e) {
		}

        Double total = input * rate;

        Double profit = Math.abs(total - input * altRate);

        this.tvGetOrGive.setText(FMT_RATE_VALUE.format(total));

        this.tvConvProfit.setText(FMT_RATE_VALUE.format(profit));
	}

	@Click(R.id.ib_conv_close)
	void closeClick() {
		this.closable.close();
	}
}
