package ch.prokopovi.ui;

import ch.prokopovi.R;
import ch.prokopovi.struct.Master.WidgetSize;

public class WidgetProvider extends AbstractWidgetProvider {

	private static final MessageUiMap MSG_UI_MAP = new MessageUiMap(
			R.layout.msg_widget_layout, R.id.layMainMessage,
			R.id.tvMessageText, WidgetSize.MEDIUM);

	public static final WidgetUiMap UI_MAP = new WidgetUiMap(
			R.layout.widget_layout, null, R.id.b_upd, R.id.b_cfg,
			WidgetSize.MEDIUM, R.id.tvBrand, R.id.tv_type, R.id.iv_src_thumb,
			MSG_UI_MAP);

	static {
		RateValueUiMap buyValueViewIds = new RateValueUiMap(R.id.buy_arrow,
				R.id.tvBuy, R.id.tvBuyDynamic);

		RateValueUiMap sellValueViewIds = new RateValueUiMap(R.id.sell_arrow,
				R.id.tvSell, R.id.tvSellDynamic);

		RateUiMap rateUiMap = new RateUiMap(R.id.tvCurrency);
		rateUiMap.setBuy(buyValueViewIds);
		rateUiMap.setSell(sellValueViewIds);

		UI_MAP.getRates().add(rateUiMap);
	}
}
