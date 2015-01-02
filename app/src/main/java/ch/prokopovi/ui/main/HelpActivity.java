package ch.prokopovi.ui.main;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.TextView;
import ch.prokopovi.PrefsUtil;
import ch.prokopovi.R;

@EActivity(R.layout.help_layout)
public class HelpActivity extends ActionBarActivity {

	private static final String LOG_TAG = "HelpActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		// skin
		PrefsUtil.initSkin(this);

		super.onCreate(savedInstanceState);

		PrefsUtil.initFullscreen(this);
	}

	@AfterViews
	void init() {
		setTitle(R.string.msg_help_title);

		// tv has links specified by putting <a> tags in the string
		// resource. By default these links will appear but not
		// respond to user input. To make them active, you need to
		// call setMovementMethod() on the TextView object.

		TextView tv = (TextView) findViewById(R.id.tvHelpMessage);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(LOG_TAG, "paused");
		finish();
	}
}
