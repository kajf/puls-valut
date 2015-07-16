package ch.prokopovi.ui.main.resolvers;

import android.support.v4.app.*;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import ch.prokopovi.R;
import ch.prokopovi.ui.main.FragmentTag;
import ch.prokopovi.ui.main.UiHelper;
import ch.prokopovi.ui.main.api.OpenListener;

public class SinglePaneResolver implements PaneResolver {
    private FragmentActivity context;

    public SinglePaneResolver(FragmentActivity context) {
        this.context = context;
    }

    @Override
    public boolean isDisplayShowTitleEnabled() {
        return false;
    }

    @Override
    public void onCreate(FragmentTransaction ft) {
        UiHelper.addOrAttachFragment(context, ft, FragmentTag.BEST);
    }

    @Override
    public void onOpen(LatLng latLng) {
        OpenListener f = showNear();

        f.onOpen(latLng);
    }

    @Override
    public void addDrawerItems(List<String> drawerItems) {

        drawerItems.add(context.getResources().getString(R.string.lbl_best_rates));
        drawerItems.add(context.getResources().getString(R.string.lbl_near_rates));
    }

    @Override
    public boolean isBestActive() {

        Fragment best = context.getSupportFragmentManager().findFragmentByTag(
                FragmentTag.BEST.tag);
        return (best != null && best.isVisible());
    }

    @Override
    public void showBest() {

        UiHelper.showFragment(context, FragmentTag.BEST);
    }

    @Override
    public <T extends Fragment> T showNear() {

        T f = UiHelper.showFragment(context, FragmentTag.NEAR);

        return f;
    }
}
