package ch.prokopovi.ui.main;

import android.app.AlertDialog;
import android.content.Context;
import android.support.v4.app.Fragment;

import com.google.android.gms.common.GooglePlayServicesUtil;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;

import ch.prokopovi.R;

/**
 * Created by pavel_letsiaha on 29-Jan-15.
 */
@EFragment(R.layout.about_layout)
public class AboutFragment extends Fragment {

    @Click(R.id.bAboutNotice)
    void noticeClick() {
        Context ctx = this.getActivity();

        String licenseInfo = GooglePlayServicesUtil
                .getOpenSourceSoftwareLicenseInfo(ctx);
        AlertDialog.Builder LicenseDialog = new AlertDialog.Builder(ctx);
        LicenseDialog.setTitle(R.string.btn_about_notice);
        LicenseDialog.setMessage(licenseInfo);
        LicenseDialog.show();
    }
}
