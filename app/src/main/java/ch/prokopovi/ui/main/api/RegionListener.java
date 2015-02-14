package ch.prokopovi.ui.main.api;

import ch.prokopovi.struct.Master;

/**
 * Created by Pavel_Letsiaha on 14-Feb-15.
 */
public interface RegionListener {
    void onRegionChange(Master.Region newRegion);
}
