package ch.prokopovi.ui.main;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.util.Log;
import ch.prokopovi.R;


@EFragment(R.layout.banner_layout)
public class BannerFragment extends Fragment {

	private static final String LOG_TAG = "BannerFragment";

	@Override
	public void onAttach(Activity activity) {

		Log.d(LOG_TAG, "onAttach");

		super.onAttach(activity);

		// This makes sure that the container activity has implemented
		// the callback interface. If not, it throws an exception
		try {

			//this.adListener = (AdListener) activity;

		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement AdListener");
		}
	}

	@AfterViews
	void init() {
		Log.d(LOG_TAG, "init");

		//this.mAdView.setAdListener(this.adListener);
	}
}
