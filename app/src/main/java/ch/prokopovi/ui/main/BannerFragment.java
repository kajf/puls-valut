package ch.prokopovi.ui.main;

import android.support.v4.app.Fragment;
import android.util.Log;

import com.google.android.gms.ads.*;

import org.androidannotations.annotations.*;

import ch.prokopovi.R;

@EFragment(R.layout.banner_layout)
public class BannerFragment extends Fragment {

	private static final String LOG_TAG = "BannerFragment";

    @ViewById(R.id.adView)
    AdView mAdView;

    @AfterViews
	void init() {
		Log.d(LOG_TAG, "init");

        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(int errorCode) {
                Fragment f = FragmentTag.BANNER.getFragment(getActivity());
                getActivity().getSupportFragmentManager().beginTransaction().hide(f).commit();
            }
        });
    }
}
