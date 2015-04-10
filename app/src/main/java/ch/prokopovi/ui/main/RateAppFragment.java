package ch.prokopovi.ui.main;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.*;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;

import ch.prokopovi.R;
import ch.prokopovi.ui.main.resolvers.PaneResolverFactory;

@EFragment
public class RateAppFragment extends Fragment {

	interface RateAppListener {
		void onRate(int buttonId);
	}

	private RateAppListener externalListener;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        boolean isDualPane = PaneResolverFactory.isLargeLayout(getActivity());
        int layout = isDualPane ?
                R.layout.rate_me_layout_long :
                R.layout.rate_me_layout;

        return inflater.inflate(layout, container, false);
    }

    @Click({R.id.b_rate, R.id.b_not_now, R.id.b_do_not_ask})
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
