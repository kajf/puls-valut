package ch.prokopovi.ui.main;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import ch.prokopovi.R;

public final class UiHelper {

    public static <T extends Fragment> T showFragment(FragmentActivity ctx, FragmentTag ftag) {
        FragmentManager fm = ctx.getSupportFragmentManager();

        FragmentTransaction ft = fm.beginTransaction();

        ft.setCustomAnimations(R.anim.abc_slide_in_top, 0);

        clearFragments(ctx, ft, ftag.tag);

        T fragment = addOrAttachFragment(ctx, ft, ftag);

        ft.commit();

        return fragment;
    }

    public static <T extends Fragment> T addOrAttachFragment(
            FragmentActivity context,
            FragmentTransaction ft,
            FragmentTag info) {

        Fragment fragment = context.getSupportFragmentManager().findFragmentByTag(info.tag);

        if (fragment == null) {

            fragment = Fragment.instantiate(context, info.className);

            ft.add(info.container, fragment, info.tag);
        } else {
            if (fragment.isDetached()) {
                ft.attach(fragment);
            }
        }

        return (T) fragment;
    }

    public static void clearFragments(FragmentActivity ctx, FragmentTransaction ft, String skipTag) {
        FragmentManager fm = ctx.getSupportFragmentManager();
        List<Fragment> fs = fm.getFragments();
        for (Fragment f : fs) {

            if (f == null) continue;

            if (skipTag != null && skipTag.equals(f.getTag())) continue;

            ft.detach(f);
        }
    }

    public static void applyFont(Context ctx, View parent, Typeface tf) {

        if (tf == null)
			tf = Typeface.createFromAsset(ctx.getAssets(),
					"fonts/Roboto-Light.ttf");

		if (parent instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) parent;

			int count = vg.getChildCount();
			for (int i = 0; i <= count; i++) {
				View v = vg.getChildAt(i);

				applyFont(ctx, v, tf);
			}
		} else if (parent instanceof TextView) {
			TextView tv = (TextView) parent;
			tv.setTypeface(tf);
		}
	}
}
