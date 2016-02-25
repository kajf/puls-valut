package ch.prokopovi.ui.main.resolvers;

import android.content.*;
import android.content.res.*;
import android.support.v4.app.*;
import android.util.*;

import com.google.android.gms.common.*;

import ch.prokopovi.*;

public class PaneResolverFactory {
    private static final String LOG_TAG = "PaneResolverFactory";

    public static PaneResolver createPaneResolver(FragmentActivity context) {

        if (isDualPane(context)) {
            return new DualPaneResolver(context);
        }

        return new SinglePaneResolver(context);
    }

    /**
     * get whether layout is dual-paned or single-paned
     *
     * @return
     */
    public static boolean isDualPane(Context context) {

        int statusCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(context);
        boolean mapsAvailable = (statusCode == ConnectionResult.SUCCESS);
        Log.d(LOG_TAG, "maps available: " + mapsAvailable);

        Configuration cfg = context.getResources().getConfiguration();
        boolean isLandscape = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE;
        Log.d(LOG_TAG, "is landscape: " + isLandscape);

        boolean isLarge = isLargeLayout(context);

        boolean res = mapsAvailable && isLandscape && isLarge;

        Log.d(LOG_TAG, "dual pane: " + res);

        return res;
    }

    public static boolean isLargeLayout(Context context) {
        Configuration cfg = context.getResources().getConfiguration();

        int masked = cfg.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        boolean isLarge = masked == Configuration.SCREENLAYOUT_SIZE_LARGE
                || VersionHelper.isXlarge(cfg);
        Log.d(LOG_TAG, "is large: " + isLarge);
        return isLarge;
    }
}
