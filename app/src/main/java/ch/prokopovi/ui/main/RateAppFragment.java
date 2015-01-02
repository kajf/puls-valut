package ch.prokopovi.ui.main;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.view.View;
import ch.prokopovi.R;

@EFragment(R.layout.rate_me_layout)
public class RateAppFragment extends Fragment {

	interface RateAppListener {
		void onRate(int buttonId);
	}

	private RateAppListener externalListener;

	@Click({ R.id.b_rate, R.id.b_not_now, R.id.b_do_not_ask })
	void onClickAny(View v) {
		int buttonId = v.getId();

		RateAppFragment.this.externalListener.onRate(buttonId);
	}

	@Override
	public void onAttach(Activity activity) {

		super.onAttach(activity);

		// This makes sure that the container activity has implemented
		// the callback interface. If not, it throws an exception
		try {
			this.externalListener = (RateAppListener) activity;

		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement Updater");
		}
	}
}
