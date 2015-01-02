package ch.prokopovi.struct;

import android.content.Context;
import ch.prokopovi.api.struct.Titled;

public class ToStringWrapper {

	private final Context context;
	private final Titled item;

	public ToStringWrapper(Titled item, Context context) {
		super();
		this.item = item;
		this.context = context;
	}

	public Titled getItem() {
		return item;
	}

	@Override
	public String toString() {
		String title = context.getResources().getString(item.getTitleRes());
		return title;
	}
}
