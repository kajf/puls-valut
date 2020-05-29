package ch.prokopovi.ui.main;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import ch.prokopovi.R;

public enum FragmentTag {
    CONVERTER(ConverterFragment_.class.getName(), R.id.top_container), //
    BEST(BestRatesFragment_.class.getName(), R.id.main_container), //
    NEAR(NearFragment.class.getName(), R.id.main_container), //
    ABOUT(AboutFragment_.class.getName(), R.id.main_container), //
    RATE(RateAppFragment_.class.getName(), R.id.bottom_container), //
    BANNER(BannerFragment_.class.getName(), R.id.bottom_container);

    public final String tag;
    public final String className;
    public final int container;

    FragmentTag(String clazz, int container) {
        this.tag = name();
        this.className = clazz;
        this.container = container;
    }

    public <T extends Fragment> T getFragment(
            FragmentActivity context) {
        Fragment fragment = context.getSupportFragmentManager().findFragmentByTag(tag);

        if (fragment == null) {
            fragment = Fragment.instantiate(context, className);
        }

        return (T) fragment;
    }

    public boolean isDetachable() {
        return container != R.id.bottom_container;
    }

    public static FragmentTag byTag(String tag) {
        for (FragmentTag fragmentTag : values()) {
            if (fragmentTag.tag.equals(tag)) {
                return fragmentTag;
            }
        }
        return null;
    }
}
