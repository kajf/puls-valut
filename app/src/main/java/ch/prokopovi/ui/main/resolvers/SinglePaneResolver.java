package ch.prokopovi.ui.main.resolvers;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import ch.prokopovi.R;
import ch.prokopovi.ui.main.FragmentTag;
import ch.prokopovi.ui.main.UiHelper;
import ch.prokopovi.ui.main.api.OpenListener;

public class SinglePaneResolver implements PaneResolver {
    private FragmentActivity context;
    private FragmentTag fragmentTag;

    public SinglePaneResolver(FragmentActivity context, FragmentTag fragmentTag) {
        this.context = context;
        this.fragmentTag = fragmentTag;
    }

    @Override
    public boolean isDisplayShowTitleEnabled() {
        return false;
    }

    @Override
    public void onCreate(FragmentTransaction ft) {
    }

    @Override
    public void onOpen(LatLng latLng) {
        OpenListener f = (OpenListener) UiHelper.showFragment(context, fragmentTag);

        f.onOpen(latLng);
    }

    @Override
    public void addDrawerItems(List<String> drawerItems) {

        drawerItems.add(context.getResources().getString(R.string.lbl_best_rates));
        drawerItems.add(context.getResources().getString(R.string.lbl_near_rates));
    }
}
