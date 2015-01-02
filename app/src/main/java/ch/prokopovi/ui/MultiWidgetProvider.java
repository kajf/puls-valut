package ch.prokopovi.ui;

import ch.prokopovi.R;
import ch.prokopovi.struct.Master.WidgetSize;

public class MultiWidgetProvider extends AbstractWidgetProvider {

	private static final MessageUiMap MSG_UI_MAP = new MessageUiMap(
			R.layout.multi_msg_widget_layout, R.id.layMultiMainMessage,
			R.id.tvMultiMessageText, WidgetSize.LARGE);

	public static final WidgetUiMap UI_MAP = new WidgetUiMap(
			R.layout.multi_widget_layout, R.id.b_multi_launch_app,
			R.id.b_multi_upd, R.id.b_multi_cfg, WidgetSize.LARGE,
			R.id.tvMultiDate, R.id.tv_multi_type, R.id.iv_multi_src_thumb,
			MSG_UI_MAP);

	static {
		// 1 rate
		RateValueUiMap buyValueViewIds1 = new RateValueUiMap(
				R.id.iv_multi_buy_arrow_1, R.id.tvMultiBuy_1,
				R.id.tvMultiBuyDynamic_1);

		RateValueUiMap sellValueViewIds1 = new RateValueUiMap(
				R.id.iv_multi_sell_arrow_1, R.id.tvMultiSell_1,
				R.id.tvMultiSellDynamic_1);

		RateUiMap rateViewIds1 = new RateUiMap(R.id.tvMultiCurrency_1);
		rateViewIds1.setBuy(buyValueViewIds1);
		rateViewIds1.setSell(sellValueViewIds1);

		// 2 rate
		RateValueUiMap buyValueViewIds2 = new RateValueUiMap(
				R.id.iv_multi_buy_arrow_2, R.id.tvMultiBuy_2,
				R.id.tvMultiBuyDynamic_2);

		RateValueUiMap sellValueViewIds2 = new RateValueUiMap(
				R.id.iv_multi_sell_arrow_2, R.id.tvMultiSell_2,
				R.id.tvMultiSellDynamic_2);

		RateUiMap rateViewIds2 = new RateUiMap(R.id.tvMultiCurrency_2);
		rateViewIds2.setBuy(buyValueViewIds2);
		rateViewIds2.setSell(sellValueViewIds2);

		// 3 rate
		RateValueUiMap buyValueViewIds3 = new RateValueUiMap(
				R.id.iv_multi_buy_arrow_3, R.id.tvMultiBuy_3,
				R.id.tvMultiBuyDynamic_3);

		RateValueUiMap sellValueViewIds3 = new RateValueUiMap(
				R.id.iv_multi_sell_arrow_3, R.id.tvMultiSell_3,
				R.id.tvMultiSellDynamic_3);

		RateUiMap rateViewIds3 = new RateUiMap(R.id.tvMultiCurrency_3);
		rateViewIds3.setBuy(buyValueViewIds3);
		rateViewIds3.setSell(sellValueViewIds3);

		// assemble
		UI_MAP.getRates().add(rateViewIds1);
		UI_MAP.getRates().add(rateViewIds2);
		UI_MAP.getRates().add(rateViewIds3);
	}
}
