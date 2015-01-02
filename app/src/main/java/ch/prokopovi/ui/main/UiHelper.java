package ch.prokopovi.ui.main;

import android.content.ContextWrapper;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public final class UiHelper {

	public static void applyFont(ContextWrapper ctx, View parent, Typeface tf) {

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
