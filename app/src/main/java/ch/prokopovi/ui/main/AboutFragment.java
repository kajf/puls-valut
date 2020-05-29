package ch.prokopovi.ui.main;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import com.google.android.gms.common.GooglePlayServicesUtil;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;

import ch.prokopovi.R;

@EFragment(R.layout.about_layout)
public class AboutFragment extends DialogFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, getTheme());
    }
}
