package ch.prokopovi.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.JobIntentService;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import ch.prokopovi.IntentFactory;
import ch.prokopovi.PrefsUtil;
import ch.prokopovi.R;
import ch.prokopovi.UpdateService;
import ch.prokopovi.api.provider.Provider;
import ch.prokopovi.api.struct.ThumbedTitle;
import ch.prokopovi.provider.ProviderFactory;
import ch.prokopovi.struct.Master.CurrencyCode;
import ch.prokopovi.struct.Master.ProviderCode;
import ch.prokopovi.struct.Master.RateType;
import ch.prokopovi.struct.WidgetPreferences;

public abstract class AbstractWidgetConfigure extends Activity {

	private class OkRateTypeListener implements DialogInterface.OnClickListener {

		private final WidgetPreferences prefs;

		private OkRateTypeListener(WidgetPreferences prefs) {
			super();
			this.prefs = prefs;
		}

		@Override
		public void onClick(DialogInterface dialog, int item) {

			RateType rateType = RateType.values()[item];

			Log.d(LOG_TAG, "selected rateType " + rateType);

			if (rateType != null) {
				this.prefs.setRateType(rateType);
			}

			dialog.dismiss();

			callNext(this.prefs);
		}
	}

	protected class OkCurrencyListener implements
			DialogInterface.OnClickListener {

		private final WidgetPreferences prefs;
		private final CurrencyCode[] currencyCodes;

		protected OkCurrencyListener(WidgetPreferences prefs,
				CurrencyCode[] currencyCodes) {
			super();
			this.prefs = prefs;
			this.currencyCodes = currencyCodes;
		}

		@Override
		public void onClick(DialogInterface dialog, int item) {

			CurrencyCode currencyCode = this.currencyCodes[item];

			Log.d(LOG_TAG, "selected currency " + currencyCode);

			if (currencyCode != null) {
				this.prefs.getCurrencyCodes().add(currencyCode);
			}

			dialog.dismiss();

			callNext(this.prefs);
		}
	}

	protected class CancelListener implements DialogInterface.OnCancelListener {
		@Override
		public void onCancel(DialogInterface dialog) {
			Log.d(LOG_TAG, "cancel config");
			dialog.dismiss();
			finish();
		}
	}

	private static final String LOG_TAG = "AbstractWidgetConfigure";

	/**
	 * create adapter for titled list dialog
	 * 
	 * @param context
	 * @param items
	 * @return
	 */
	public static <T extends ThumbedTitle> ListAdapter buildAdapter(
			final Context context, final T[] items) {

		ListAdapter adapter = new ArrayAdapter<T>(context,
				android.R.layout.select_dialog_item, android.R.id.text1, items) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = super.getView(position, convertView, parent);
				TextView tv = (TextView) v.findViewById(android.R.id.text1);

				// Put the image on the TextView
				tv.setCompoundDrawablesWithIntrinsicBounds(
						items[position].getThumbRes(), 0,
						items[position].getSecondThumbRes(), 0);

				tv.setText(items[position].getTitleRes());

				// Add margin between image and text (support various screen
				// densities)
				int dp5 = (int) (5 * context.getResources().getDisplayMetrics().density + 0.5f);
				tv.setCompoundDrawablePadding(dp5);

				return v;
			}
		};

		return adapter;
	}

	protected int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private final DialogInterface.OnClickListener okProviderListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int item) {

			ProviderCode providerCode = ProviderCode.values()[item];

			Log.d(LOG_TAG, "selected provider " + providerCode);

			WidgetPreferences prefs = new WidgetPreferences();
			if (providerCode != null) {
				prefs.setProviderCode(providerCode);
			}

			dialog.dismiss();

			callType(prefs);
		}
	};

	/**
	 * action to do after common configuration (e.g. next dialog)
	 * 
	 * @param prefs
	 *            selected preference values
	 */
	protected abstract void callNext(WidgetPreferences prefs);

	/**
	 * dialog step to define rate type (if it is possible)
	 * 
	 * @param prefs
	 */
	private void callType(WidgetPreferences prefs) {

		ProviderCode providerCode = prefs.getProviderCode();
		Provider provider = ProviderFactory.getProvider(providerCode);

		RateType[] supportedRateTypes = provider.getSupportedRateTypes();

		boolean hasRateTypes = supportedRateTypes != null
				&& supportedRateTypes.length > 1;
		Log.d(LOG_TAG, "hasRateTypes " + hasRateTypes);

		if (hasRateTypes) {

			OkRateTypeListener okRateTypeListener = new OkRateTypeListener(
					prefs);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.lbl_choose_rate_type)
					.setSingleChoiceItems(
							RateType.getTitles(this, supportedRateTypes), -1,
							okRateTypeListener)
					.setOnCancelListener(
							new DialogInterface.OnCancelListener() {
								@Override
								public void onCancel(DialogInterface dialog) {
									Log.d(LOG_TAG, "cancel config");

									finish();
								}
							}).create().show();
		} else {
			RateType rateType = supportedRateTypes[0]; // the only
														// type
			prefs.setRateType(rateType);
			callNext(prefs);
		}
	}

	/**
	 * successfull config callback. Saving prefs, finishing activity, etc.
	 * 
	 * @param context
	 * @param prefs
	 */
	protected void finishConfig(Context context, WidgetPreferences prefs) {

		try {
			PrefsUtil.save(context, this.mAppWidgetId, prefs);

			Intent pendingIntent = IntentFactory
					.createWeakUpdateServiceIntent(context, prefs);

			JobIntentService.enqueueWork(context, UpdateService.class, 100, pendingIntent);

			// Make sure we pass back the original appWidgetId
			Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
					this.mAppWidgetId);
			setResult(RESULT_OK, resultValue);

		} catch (JSONException e) {
			Log.e(LOG_TAG, "error during props save", e);
		} catch (Exception e) {
			Log.e(LOG_TAG, "error during update service send", e);
		}

		finish();
	}

	/**
	 * fill prefs with currencies by user select or with all available no more
	 * then needed
	 * 
	 * @param prefs
	 * @param currenciesNeeded
	 */
	protected void selectFewCurrencies(WidgetPreferences prefs,
			int currenciesNeeded) {

		ProviderCode providerCode = prefs.getProviderCode();
		RateType rateType = prefs.getRateType();

		Provider provider = ProviderFactory.getProvider(providerCode);
		CurrencyCode[] currencyCodes = provider
				.getSupportedCurrencyCodes(rateType);
		int currenciesAvailable = currencyCodes.length;

		if (currenciesAvailable <= currenciesNeeded) {
			List<CurrencyCode> list = Arrays.asList(currencyCodes);

			prefs.getCurrencyCodes().addAll(list);

			Log.d(LOG_TAG, "multi prefs: " + prefs);
			finishConfig(this, prefs);
			return;
		}// all currencies are in selection

		Set<CurrencyCode> pCurrCodes = prefs.getCurrencyCodes();
		int currenciesSelected = pCurrCodes.size();
		if (currenciesSelected < currenciesNeeded) {

			List<CurrencyCode> list = Arrays.asList(currencyCodes);
			ArrayList<CurrencyCode> modifiableList = new ArrayList<CurrencyCode>(
					list);
			modifiableList.removeAll(pCurrCodes);

			CurrencyCode[] absentCurrencies = modifiableList
					.toArray(new CurrencyCode[modifiableList.size()]);

			ListAdapter adapter = buildAdapter(this, absentCurrencies);

			final OkCurrencyListener okCurrencyListener = new OkCurrencyListener(
					prefs, absentCurrencies);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.lbl_choose_currency)
					.setAdapter(adapter, okCurrencyListener)
					.setOnCancelListener(new CancelListener()).create().show();

		} else {

			Log.d(LOG_TAG, "multi prefs: " + prefs);
			finishConfig(this, prefs);
			return;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(LOG_TAG, "onCreate");

		// Set the result to CANCELED. This will cause the widget host to
		// cancel
		// out of the widget placement if they press the back button.

		// If we try to set result like this
		// --------
		// Intent resultValue = new Intent();
		// resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
		// mAppWidgetId);
		// setResult(RESULT_CANCELED, resultValue);
		// ---------
		// no redundant id is created but redundant widget became visible on
		// cancel !!! (((
		// so just ids is the less evil

		setResult(RESULT_CANCELED);

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {

			this.mAppWidgetId = extras.getInt(
					AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
			Log.d(LOG_TAG, "configuring widget with id " + this.mAppWidgetId);

			final ProviderCode[] items = ProviderCode.values();

			/*
			 * ListAdapter adapter = new ArrayAdapter<ProviderCode>(this,
			 * android.R.layout.select_dialog_item, android.R.id.text1, items) {
			 * 
			 * @Override public View getView(int position, View convertView,
			 * ViewGroup parent) { // User super class to create the View View v
			 * = super.getView(position, convertView, parent); TextView tv =
			 * (TextView) v.findViewById(android.R.id.text1);
			 * 
			 * // Put the image on the TextView
			 * tv.setCompoundDrawablesWithIntrinsicBounds(
			 * items[position].getThumbRes(), 0, 0, 0);
			 * 
			 * tv.setText(items[position].getTitleRes());
			 * 
			 * // Add margin between image and text (support various screen //
			 * densities) int dp5 = (int) (5 *
			 * getResources().getDisplayMetrics().density + 0.5f);
			 * tv.setCompoundDrawablePadding(dp5);
			 * 
			 * return v; } };
			 */

			ListAdapter adapter = buildAdapter(this, items);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.lbl_choose_provider)
					// .setSingleChoiceItems(ProviderCode.getTitles(this), -1,
					// okProviderListener)
					.setAdapter(adapter, this.okProviderListener)
					.setOnCancelListener(
							new DialogInterface.OnCancelListener() {
								@Override
								public void onCancel(DialogInterface dialog) {
									Log.d(LOG_TAG, "cancel config");

									finish();
								}
							}).create().show();
		}

		// If they gave us an intent without the widget id, just bail.
		if (this.mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
		}
	}
}
