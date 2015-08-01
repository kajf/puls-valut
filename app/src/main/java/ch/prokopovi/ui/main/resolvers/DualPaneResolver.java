package ch.prokopovi.ui.main.resolvers;

import android.support.v4.app.*;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import ch.prokopovi.ui.main.FragmentTag;
import ch.prokopovi.ui.main.UiHelper;
import ch.prokopovi.ui.main.api.OpenListener;

public class DualPaneResolver implements PaneResolver {
    private FragmentActivity context;

    public DualPaneResolver(FragmentActivity context) {
        this.context = context;
    }

    @Override
    public boolean isDisplayShowTitleEnabled() {
        return true;
    }

    @Override
    public void onCreate(FragmentTransaction ft) {
        UiHelper.addOrAttachFragment(context, ft, FragmentTag.BEST);
        UiHelper.addOrAttachFragment(context, ft, FragmentTag.NEAR);
    }

    @Override
    public void onOpen(LatLng latLng) {
        FragmentManager fm = context.getSupportFragmentManager();
        OpenListener f = (OpenListener) fm.findFragmentByTag(FragmentTag.NEAR.tag);
        f.onOpen(latLng);
    }

    @Override
    public void addDrawerItems(List<String> drawerItems) {
    }

    @Override
    public boolean isBestActive() {
        return true;
    }

    @Override
    public void showBest() {
    }

    @Override
    public <T extends Fragment> T showNear() {
        return null;
    }

    @Override
    public void showAbout() {
        DialogFragment df = FragmentTag.ABOUT.getFragment(context);
        df.show(context.getSupportFragmentManager(), FragmentTag.ABOUT.tag);
    }
}
