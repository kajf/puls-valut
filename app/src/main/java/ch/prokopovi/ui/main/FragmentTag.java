package ch.prokopovi.ui.main;

import ch.prokopovi.R;

/**
 * Created by Pavel_Letsiaha on 21-Mar-15.
 */
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

    private FragmentTag(String clazz, int container) {
        this.tag = name();
        this.className = clazz;
        this.container = container;
    }
}
