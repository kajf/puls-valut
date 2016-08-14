package ch.prokopovi.ui;

import ch.prokopovi.R;
import ch.prokopovi.struct.Master.WidgetSize;

public class MiniWidgetProvider extends AbstractWidgetProvider {

	private static final MessageUiMap MSG_UI_MAP = new MessageUiMap(
			R.layout.mini_msg_widget_layout, R.id.lay_mini_main_message,
			R.id.tv_mini_message_text, WidgetSize.MINI);

	public static final WidgetUiMap UI_MAP = new WidgetUiMap(
			R.layout.mini_widget_layout, R.id.lay_mini_main, null, null,
			WidgetSize.MINI, R.id.tv_mini_currency, R.id.tv_mini_type,
			R.id.iv_mini_src_thumb, MSG_UI_MAP);

	static {
		RateValueUiMap buyValueViewIds = new RateValueUiMap(
				R.id.iv_mini_buy_arrow, R.id.tv_mini_buy, null);

		RateValueUiMap sellValueViewIds = new RateValueUiMap(
				R.id.iv_mini_sell_arrow, R.id.tv_mini_sell, null);

		RateUiMap rateUiMap = new RateUiMap(null, null,buyValueViewIds,sellValueViewIds);

		UI_MAP.getRates().add(rateUiMap);
	}
}
