package ch.prokopovi.ui.main;

import java.util.List;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import ch.prokopovi.R;

import com.androidmapsextensions.ClusterOptions;
import com.androidmapsextensions.ClusterOptionsProvider;
import com.androidmapsextensions.Marker;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class ClusterDataProvider implements ClusterOptionsProvider {

	private static final int[] res = { R.drawable.m1, R.drawable.m2,
			R.drawable.m3, R.drawable.m4, R.drawable.m5 };

	private static final int[] forCounts = { 10, 100, 1000, 10000,
			Integer.MAX_VALUE };

	private final Bitmap[] baseBitmaps;

	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Rect bounds = new Rect();

	private final ClusterOptions clusterOptions = new ClusterOptions().anchor(
			0.5f, 0.5f);

	public ClusterDataProvider(Resources resources) {
		this.baseBitmaps = new Bitmap[res.length];
		for (int i = 0; i < res.length; i++) {
			this.baseBitmaps[i] = BitmapFactory.decodeResource(resources,
					res[i]);
		}
		this.paint.setColor(Color.WHITE);
		this.paint.setTextAlign(Align.CENTER);
		this.paint.setTextSize(resources.getDimension(R.dimen.text_size));
	}

	@Override
	public ClusterOptions getClusterOptions(List<Marker> markers) {

		int markersCount = markers.size();

		Bitmap base;
		int i = 0;
		do {
			base = this.baseBitmaps[i];
		} while (markersCount >= forCounts[i++]);

		Bitmap bitmap = base.copy(Config.ARGB_8888, true);

		String text = String.valueOf(markersCount);
		this.paint.getTextBounds(text, 0, text.length(), this.bounds);
		float x = bitmap.getWidth() / 2.0f;
		float y = (bitmap.getHeight() - this.bounds.height()) / 2.0f
				- this.bounds.top;

		Canvas canvas = new Canvas(bitmap);
		canvas.drawText(text, x, y, this.paint);

		BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(bitmap);
		return this.clusterOptions.icon(icon);
	}

}
