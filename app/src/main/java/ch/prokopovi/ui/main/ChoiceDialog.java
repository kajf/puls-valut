package ch.prokopovi.ui.main;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import ch.prokopovi.api.struct.Titled;

public class ChoiceDialog<T extends Titled> {

	public interface ChoiceCallback {
		void choice(Titled item);
	}

	private Context context;
	private ChoiceCallback callback;
	private int titleId;
	private T[] items;

	public ChoiceDialog<T> init(Context context, int titleId, T[] items,
			ChoiceCallback callback) {
		this.context = context;
		this.callback = callback;
		this.titleId = titleId;
		this.items = items;

		return this;
	}

	public Dialog create() {

		if (this.context == null || this.callback == null || this.items == null
				|| this.items.length < 2) {
			throw new NullPointerException(
					"init was not called properly (wrong params are present)");
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this.context);

		builder.setTitle(this.titleId).setItems(collectTitles(),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						ChoiceDialog.this.callback
								.choice(ChoiceDialog.this.items[which]);
					}
				});

		return builder.create();
	}

	/**
	 * collect array of items' titles
	 * 
	 * @return
	 */
	private String[] collectTitles() {
		String[] titles = new String[this.items.length];

		for (int i = 0; i < this.items.length; i++) {
			T item = this.items[i];

			titles[i] = this.context.getResources().getString(
					item.getTitleRes());
		}

		return titles;
	}
}
