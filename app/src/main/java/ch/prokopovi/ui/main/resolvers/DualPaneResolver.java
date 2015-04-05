package ch.prokopovi.ui.main.resolvers;

import android.support.v4.app.*;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import ch.prokopovi.ui.main.FragmentTag;
import ch.prokopovi.ui.main.UiHelper;
import ch.prokopovi.ui.main.api.OpenListener;

public class DualPaneResolver implements PaneResolver {
    private FragmentActivity context;
    private FragmentTag fragmentTag;

    public DualPaneResolver(FragmentActivity context, FragmentTag fragmentTag) {
        this.context = context;
        this.fragmentTag = fragmentTag;
    }

    @Override
    public boolean isDisplayShowTitleEnabled() {
        return true;
    }

    @Override
    public void onCreate(FragmentTransaction ft) {
        UiHelper.addOrAttachFragment(context, ft, fragmentTag);
    }

    @Override
    public void onOpen(LatLng latLng) {
        FragmentManager fm = context.getSupportFragmentManager();
        OpenListener f = (OpenListener) fm.findFragmentByTag(fragmentTag.tag);
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
}
