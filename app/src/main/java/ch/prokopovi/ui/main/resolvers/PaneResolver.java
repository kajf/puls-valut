package ch.prokopovi.ui.main.resolvers;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.util.List;

import ch.prokopovi.ui.main.api.OpenListener;

public interface PaneResolver extends OpenListener {
    void onCreate(FragmentTransaction ft);

    boolean isDisplayShowTitleEnabled();

    void addDrawerItems(List<String> drawerItems);

    boolean isBestActive();

    void showBest();

    <T extends Fragment> T showNear();

    void showAbout();
}
