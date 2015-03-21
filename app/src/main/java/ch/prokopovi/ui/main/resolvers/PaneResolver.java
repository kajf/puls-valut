package ch.prokopovi.ui.main.resolvers;

import android.support.v4.app.FragmentTransaction;

import java.util.List;

import ch.prokopovi.ui.main.api.OpenListener;

/**
 * Created by Pavel_Letsiaha on 21-Mar-15.
 */
public interface PaneResolver extends OpenListener {
    void onCreate(FragmentTransaction ft);

    boolean isDisplayShowTitleEnabled();

    void addDrawerItems(List<String> drawerItems);
}
