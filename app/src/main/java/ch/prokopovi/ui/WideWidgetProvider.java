package ch.prokopovi.ui;

import ch.prokopovi.R;
import ch.prokopovi.struct.Master.WidgetSize;

public class WideWidgetProvider extends AbstractWidgetProvider {

	private static final MessageUiMap MSG_UI_MAP = new MessageUiMap(
			R.layout.wide_msg_widget_layout, R.id.lay_wide_main_message,
			R.id.tv_wide_message_text, WidgetSize.WIDE);

	public static final WidgetUiMap UI_MAP = new WidgetUiMap(
			R.layout.wide_widget_layout, R.id.b_wide_launch_app,
			R.id.b_wide_upd, R.id.b_wide_cfg, WidgetSize.WIDE,
			R.id.tv_wide_date, R.id.tv_wide_type, R.id.iv_wide_src_thumb,
			MSG_UI_MAP);

	static {
		// 1 rate
		RateValueUiMap buyValueViewIds1 = new RateValueUiMap(
				R.id.iv_wide_buy_arrow_1, R.id.tvWideBuy_1,
				R.id.tvWideBuyDynamic_1);

		RateValueUiMap sellValueViewIds1 = new RateValueUiMap(
				R.id.iv_wide_sell_arrow_1, R.id.tvWideSell_1,
				R.id.tvWideSellDynamic_1);

		RateUiMap rateViewIds1 = new RateUiMap(R.id.tvWideCurrency_1);
		rateViewIds1.setBuy(buyValueViewIds1);
		rateViewIds1.setSell(sellValueViewIds1);

		// 2 rate
		RateValueUiMap buyValueViewIds2 = new RateValueUiMap(
				R.id.iv_wide_buy_arrow_2, R.id.tvWideBuy_2,
				R.id.tvWideBuyDynamic_2);

		RateValueUiMap sellValueViewIds2 = new RateValueUiMap(
				R.id.iv_wide_sell_arrow_2, R.id.tvWideSell_2,
				R.id.tvWideSellDynamic_2);

		RateUiMap rateViewIds2 = new RateUiMap(R.id.tvWideCurrency_2);
		rateViewIds2.setBuy(buyValueViewIds2);
		rateViewIds2.setSell(sellValueViewIds2);

		// assemble
		UI_MAP.getRates().add(rateViewIds1);
		UI_MAP.getRates().add(rateViewIds2);
	}
}
