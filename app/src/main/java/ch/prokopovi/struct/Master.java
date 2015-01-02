package ch.prokopovi.struct;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;
import android.util.SparseArray;
import ch.prokopovi.R;
import ch.prokopovi.api.struct.Thumbed;
import ch.prokopovi.api.struct.ThumbedTitle;
import ch.prokopovi.api.struct.Titled;
import ch.prokopovi.exported.PureConst;
import ch.prokopovi.provider.AlfaProvider;
import ch.prokopovi.provider.ByAlfaProvider;
import ch.prokopovi.provider.CbrProvider;
import ch.prokopovi.provider.DeltaSravnibankUaProvider;
import ch.prokopovi.provider.MmProvider;
import ch.prokopovi.provider.MtbProvider;
import ch.prokopovi.provider.NbrbProvider;
import ch.prokopovi.provider.NbuProvider;
import ch.prokopovi.provider.OschadSravnibankUaProvider;
import ch.prokopovi.provider.PriorProvider;
import ch.prokopovi.provider.PrivatSravnibankUaProvider;
import ch.prokopovi.provider.SbrProvider;
import ch.prokopovi.provider.UnicreditSravnibankUaProvider;
import ch.prokopovi.ui.MiniWidgetConfigure;
import ch.prokopovi.ui.MiniWidgetProvider;
import ch.prokopovi.ui.MultiWidgetConfigure;
import ch.prokopovi.ui.MultiWidgetProvider;
import ch.prokopovi.ui.WideWidgetConfigure;
import ch.prokopovi.ui.WideWidgetProvider;
import ch.prokopovi.ui.WidgetConfigure;
import ch.prokopovi.ui.WidgetProvider;

import com.google.android.gms.maps.model.LatLng;

public final class Master {

	public enum WidgetSize {
		MINI(MiniWidgetConfigure.class, MiniWidgetProvider.class), //
		MEDIUM(WidgetConfigure.class, WidgetProvider.class), //
		LARGE(MultiWidgetConfigure.class, MultiWidgetProvider.class), //
		WIDE(WideWidgetConfigure.class, WideWidgetProvider.class);

		private final Class<?> configurator;
		private final Class<?> clazz;

		private WidgetSize(Class<?> configurator, Class<?> clazz) {
			this.configurator = configurator;
			this.clazz = clazz;
		}

		public static WidgetSize get(String className) {
			for (WidgetSize val : values()) {
				if (className.equals(val.clazz.getName())) {
					return val;
				}
			}

			return null;
		}

		public Class<?> getConfigurator() {
			return this.configurator;
		}
	}

	public enum CurrencyCode implements Parcelable, ThumbedTitle {
		USD(1, R.string.lbl_usd, 0), //
		EUR(2, R.string.lbl_eur, 0), //
		RUR(3, R.string.lbl_rur, 0), //
		UAH(4, R.string.lbl_uah, 0), //
		PLN(5, R.string.lbl_pln, 0), //
		GBP(6, R.string.lbl_gbp, 0), //
		JPY(7, R.string.lbl_jpy, 0), //
		EUR_USD(8, R.string.lbl_eurusd, 0), //
		CHF(9, R.string.lbl_chf, 0), //
		RON(10, R.string.lbl_ron, 0), //
		BYR(11, R.string.lbl_byr, 0), //
		LTL(12, R.string.lbl_ltl, 0), ;

		private static HashMap<String, CurrencyCode> map;

		static {
			map = new HashMap<String, CurrencyCode>();
			for (CurrencyCode val : values()) {
				map.put(val.name(), val);
			}
		}

		private static SparseArray<CurrencyCode> idMap;
		static {
			idMap = new SparseArray<CurrencyCode>();
			for (CurrencyCode val : values()) {
				idMap.put(val.getId(), val);
			}
		}

		public static CurrencyCode get(String name) {
			return map.get(name);
		}

		public static CurrencyCode get(Integer id) {
			return id != null ? idMap.get(id) : null;
		}

		/**
		 * collect values for UI dialog
		 * 
		 * @param context
		 * @return
		 */
		public static String[] getTitles(Context context) {

			CurrencyCode[] values = values();
			String[] res = new String[values.length];
			for (CurrencyCode currencyCode : values) {
				String title = context.getResources().getString(
						currencyCode.titleRes);
				res[currencyCode.ordinal()] = title;
			}

			return res;
		}

		private final int titleRes;
		private final int imgRes;

		private final int id;

		public static final Parcelable.Creator<CurrencyCode> CREATOR = new Parcelable.Creator<CurrencyCode>() {
			@Override
			public CurrencyCode createFromParcel(Parcel in) {
				return CurrencyCode.get(in);
			}

			@Override
			public CurrencyCode[] newArray(int size) {
				return new CurrencyCode[size];
			}
		};

		protected static CurrencyCode get(Parcel in) {
			return CurrencyCode.get(in.readString());
		}

		private CurrencyCode(int id, int titleRes, int imgRes) {
			this.id = id;
			this.titleRes = titleRes;
			this.imgRes = imgRes;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		public int getId() {
			return this.id;
		}

		@Override
		public int getThumbRes() {
			return this.imgRes;
		}

		@Override
		public int getTitleRes() {
			return this.titleRes;
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			out.writeString(name());
		}

		@Override
		public int getSecondThumbRes() {
			return 0;
		}

	}

	public enum OperationType implements Titled {
		BUY(1, R.string.lbl_bank_buy), SELL(2, R.string.lbl_bank_sell);

		private static SparseArray<OperationType> idMap;
		static {
			idMap = new SparseArray<OperationType>();
			for (OperationType val : values()) {
				idMap.put(val.getId(), val);
			}
		}

		public static OperationType get(Integer id) {
			return id != null ? idMap.get(id) : null;
		}

		private final int id;
		private final int titleRes;

		private OperationType(int id, int titleRes) {
			this.id = id;
			this.titleRes = titleRes;
		}

		public int getId() {
			return this.id;
		}

		@Override
		public int getTitleRes() {
			return this.titleRes;
		}

	}

	public enum CountryCode implements Thumbed {
		BY(R.drawable.by_flag, CurrencyCode.BYR), RU(R.drawable.ru_flag,
				CurrencyCode.RUR), UA(R.drawable.ua_flag, CurrencyCode.UAH);

		private final int thumbRes;

		private final CurrencyCode currency;

		private CountryCode(int thumbRes, CurrencyCode currency) {
			this.thumbRes = thumbRes;
			this.currency = currency;
		}

		public CurrencyCode getCurrency() {
			return this.currency;
		}

		@Override
		public int getThumbRes() {
			return this.thumbRes;
		}
	}

	public enum ProviderCode implements ThumbedTitle {
		NBRB(1, CountryCode.BY,//
				new int[] { Calendar.SUNDAY }, //
				R.string.lbl_nbrb_provider, //
				R.drawable.nbrb_thumb, //
				NbrbProvider.class), //
		PRIOR(2, CountryCode.BY, //
				new int[] { Calendar.SUNDAY }, //
				R.string.lbl_prior_provider, //
				R.drawable.prior_thumb,//
				PriorProvider.class), //
		MTB(3, CountryCode.BY,//
				new int[] { Calendar.SATURDAY, Calendar.SUNDAY }, //
				R.string.lbl_mtb_provider, //
				R.drawable.mtb_thumb,//
				MtbProvider.class), //
		MM(4, CountryCode.BY,//
				new int[] { Calendar.SATURDAY, Calendar.SUNDAY }, //
				R.string.lbl_mm_provider, //
				R.drawable.mm_thumb,//
				MmProvider.class), //
		BY_ALFA(5, CountryCode.BY,//
				new int[] { Calendar.SUNDAY }, //
				R.string.lbl_by_alfa_provider, //
				R.drawable.alfa_thumb,//
				ByAlfaProvider.class), //

		CBR(6, CountryCode.RU,//
				new int[] { Calendar.SUNDAY }, //
				R.string.lbl_cbr_provider, //
				R.drawable.cbr_thumb,//
				CbrProvider.class), //
		SBR(7, CountryCode.RU,//
				new int[] { Calendar.SUNDAY }, //
				R.string.lbl_sbr_provider, //
				R.drawable.sbr_thumb,//
				SbrProvider.class), //
		ALFA(11, CountryCode.RU, //
				new int[] { Calendar.SATURDAY, Calendar.SUNDAY }, //
				R.string.lbl_alfa_provider, //
				R.drawable.alfa_thumb,//
				AlfaProvider.class), //

		NBU(8, CountryCode.UA, //
				new int[] { Calendar.SATURDAY, Calendar.SUNDAY }, //
				R.string.lbl_nbu_provider, //
				R.drawable.nbu_thumb,//
				NbuProvider.class), //
		PRIVAT_SB_UA(9, CountryCode.UA, //
				new int[] { Calendar.SATURDAY, Calendar.SUNDAY }, //
				R.string.lbl_sb_ua_privat_provider, //
				R.drawable.privat_thumb,//
				PrivatSravnibankUaProvider.class), //
		OSCHAD_SB_UA(10, CountryCode.UA, //
				new int[] { Calendar.SATURDAY, Calendar.SUNDAY }, //
				R.string.lbl_sb_ua_oschad_provider, //
				R.drawable.oschad_thumb,//
				OschadSravnibankUaProvider.class), //
		UNICREDIT_SB_UA(12, CountryCode.UA, //
				new int[] { Calendar.SATURDAY, Calendar.SUNDAY }, //
				R.string.lbl_sb_ua_unicredit_provider, //
				R.drawable.unicredit_thumb,//
				UnicreditSravnibankUaProvider.class), //
		DELTA_SB_UA(13, CountryCode.UA, //
				new int[] { Calendar.SATURDAY, Calendar.SUNDAY }, //
				R.string.lbl_sb_ua_delta_provider, //
				R.drawable.delta_thumb,//
				DeltaSravnibankUaProvider.class), //
		;

		private final int id;
		private final CountryCode countryCode;
		private final int[] daysOff;

		private final int titleRes;
		private final int thumbRes;
		private final Class<?> clazz;

		private ProviderCode(int id, CountryCode countryCode, int[] daysOff,
				int titleRes, int thumbRes, Class<?> clazz) {
			this.id = id;
			this.countryCode = countryCode;
			this.daysOff = daysOff;
			this.titleRes = titleRes;
			this.thumbRes = thumbRes;
			this.clazz = clazz;
		}

		/**
		 * find nearest date from now except provided weekDaysOff *
		 * 
		 * @return valid date nearest to now
		 */
		public Date getLastValidDay() {
			int[] weekDaysOff = getDaysOff();

			Calendar cal = Calendar.getInstance();

			if (weekDaysOff != null && weekDaysOff.length > 0) {

				boolean valid = false;
				while (!valid) {
					int currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

					// search current day in days off
					boolean contains = false;
					for (int wdo : weekDaysOff) {
						if (currentDayOfWeek == wdo) {
							contains = true;
							break;
						}
					}

					if (contains) {
						cal.add(Calendar.DATE, -1);
					} else {
						valid = true;
					}
				}
			}

			return cal.getTime();
		}

		/**
		 * collect values for UI dialog
		 * 
		 * @param context
		 * @return
		 */
		public static String[] getTitles(Context context) {

			ProviderCode[] values = values();
			String[] res = new String[values.length];
			for (ProviderCode providerCode : values) {
				String title = context.getResources().getString(
						providerCode.titleRes);
				res[providerCode.ordinal()] = title;
			}

			return res;
		}

		public static ProviderCode get(String name) {
			for (ProviderCode p : values()) {
				if (p.name().equals(name)) {
					return p;
				}
			}
			return null;
		}

		public static ProviderCode getDefault() {
			return ProviderCode.NBRB;
		}

		public int getId() {
			return this.id;
		}

		public Class<?> getClazz() {
			return this.clazz;
		}

		public CountryCode getCountryCode() {
			return this.countryCode;
		}

		public int[] getDaysOff() {
			return this.daysOff;
		}

		@Override
		public int getThumbRes() {
			return this.thumbRes;
		}

		@Override
		public int getTitleRes() {
			return this.titleRes;
		}

		@Override
		public int getSecondThumbRes() {
			return this.countryCode.getThumbRes();
		}
	}

	public enum RateType {
		CARD(1, "9", R.string.lbl_card_type, R.string.lbl_card_type_short), //
		CASH(2, "3", R.string.lbl_cash_type, R.string.lbl_cash_type_short);

		private static HashMap<String, RateType> map;

		static {
			map = new HashMap<String, RateType>();
			for (RateType val : values()) {
				map.put(val.name(), val);
			}
		}

		public static RateType get(String name) {
			return map.get(name);
		}

		public static RateType getDefault() {
			return RateType.CASH;
		}

		/**
		 * collect values for UI dialog
		 * 
		 * @param context
		 * @param values
		 * @return
		 */
		public static String[] getTitles(Context context, RateType[] values) {

			String[] res = new String[values.length];
			for (RateType type : values) {
				String title = context.getResources().getString(type.titleRes);
				res[type.ordinal()] = title;
			}

			return res;
		}

		private final int id;
		private final String code;

		private final int titleRes;
		private final int shortTitleRes;

		private RateType(int id, String code, int titleRes, int shortTitleRes) {
			this.id = id;
			this.code = code;
			this.titleRes = titleRes;
			this.shortTitleRes = shortTitleRes;
		}

		public int getId() {
			return this.id;
		}

		public String getCode() {
			return this.code;
		}

		public int getShortTitleRes() {
			return this.shortTitleRes;
		}

	}

	/**
	 * Belarusian regions
	 * 
	 * @author public
	 * 
	 */
	public enum Region implements ThumbedTitle {

		// Belarus 0 - 999

		MINSK(PureConst.MINSK_ID, R.string.lbl_minsk, new LatLng(53.9,
				27.566667), CountryCode.BY), //
		GRODNO(PureConst.GRODNO_ID, R.string.lbl_grodno, new LatLng(53.666667,
				23.833333), CountryCode.BY), //
		BREST(PureConst.BREST_ID, R.string.lbl_brest, new LatLng(52.133333,
				23.666667), CountryCode.BY), //
		VITEBSK(PureConst.VITEBSK_ID, R.string.lbl_vitebsk, new LatLng(
				55.183333, 30.166667), CountryCode.BY), //
		GOMEL(PureConst.GOMEL_ID, R.string.lbl_gomel, new LatLng(52.445278,
				30.984167), CountryCode.BY), //
		MOGILEV(PureConst.MOGILEV_ID, R.string.lbl_mogilev, new LatLng(53.9,
				30.333333), CountryCode.BY), //

		BARANOVICHI(PureConst.BARANOVICHI_ID, R.string.lbl_baranovichi,
				new LatLng(53.133333, 26.016667), CountryCode.BY), //
		BOBRUISK(PureConst.BOBRUISK_ID, R.string.lbl_bobruisk, new LatLng(
				53.15, 29.233333), CountryCode.BY), //
		BORISOV(PureConst.BORISOV_ID, R.string.lbl_borisov, new LatLng(
				54.233333, 28.5), CountryCode.BY), //
		LIDA(PureConst.LIDA_ID, R.string.lbl_lida, new LatLng(53.883333, 25.3),
				CountryCode.BY), //
		MOZIR(PureConst.MOZIR_ID, R.string.lbl_mozir, new LatLng(52.05, 29.25),
				CountryCode.BY), //
		NOVOPOLOCK(PureConst.NOVOPOLOCK_ID, R.string.lbl_novopolock,
				new LatLng(55.533333, 28.65), CountryCode.BY), //
		ORSHA(PureConst.ORSHA_ID, R.string.lbl_orsha, new LatLng(54.509167,
				30.425833), CountryCode.BY), //
		PINSK(PureConst.PINSK_ID, R.string.lbl_pinsk, new LatLng(52.116667,
				26.1), CountryCode.BY), //
		POLOCK(PureConst.POLOCK_ID, R.string.lbl_polock, new LatLng(55.483333,
				28.8), CountryCode.BY), //
		SOLIGORSK(PureConst.SOLIGORSK_ID, R.string.lbl_soligorsk, new LatLng(
				52.8, 27.533333), CountryCode.BY), //

		MOLODZE4NO(PureConst.MOLODZE4NO_ID, R.string.lbl_molodze4no,
				new LatLng(54.31223616098262, 26.856765849999988),
				CountryCode.BY), //
		SVETLOGORSK(PureConst.SVETLOGORSK_ID, R.string.lbl_svetlogorsk,
				new LatLng(52.621392732902684, 29.73638535000009),
				CountryCode.BY), //
		ZLOBIN(PureConst.ZLOBIN_ID, R.string.lbl_zlobin, new LatLng(
				52.87965901034043, 30.008533050000096), CountryCode.BY), //
		RE4ICA(PureConst.RE4ICA_ID, R.string.lbl_re4ica, new LatLng(
				52.36071560832841, 30.39135944999998), CountryCode.BY), //
		SLUCK(PureConst.SLUCK_ID, R.string.lbl_sluck, new LatLng(
				53.02211499712371, 27.54504195000004), CountryCode.BY), //
		ZODINO(PureConst.ZODINO_ID, R.string.lbl_zodino, new LatLng(
				54.10266511033568, 28.33700184999998), CountryCode.BY), //

		// Ukraine 1000 - 1999

		KIEV(PureConst.KIEV_ID, R.string.lbl_kiev, new LatLng(
				50.402411394193074, 30.532690550000098), CountryCode.UA), //
		ODESSA(PureConst.ODESSA_ID, R.string.lbl_odessa, new LatLng(
				46.46015314862733, 30.711787500000014), CountryCode.UA), //
		DONECK(PureConst.DONECK_ID, R.string.lbl_doneck, new LatLng(
				47.99021347825789, 37.76152060000004), CountryCode.UA), //
		LVOV(PureConst.LVOV_ID, R.string.lbl_lvov, new LatLng(
				49.83273243784637, 24.012235550000014), CountryCode.UA), //
		DNEPROPETROVSK(PureConst.DNEPROPETROVSK_ID,
				R.string.lbl_dnepropetrovsk, new LatLng(48.46241037424381,
						35.00035649999995), CountryCode.UA), //
		;

		private static SparseArray<Region> idMap;
		static {
			idMap = new SparseArray<Region>();
			for (Region val : values()) {
				idMap.put(val.getId(), val);
			}
		}

		public static Region get(int id) {
			return idMap.get(id);
		}

		private final int id;
		private final int titleRes;
		private final LatLng coords;
		private final CountryCode countryCode;

		private Region(int id, int titleRes, LatLng coords,
				CountryCode countryCode) {
			this.id = id;
			this.titleRes = titleRes;
			this.coords = coords;
			this.countryCode = countryCode;
		}

		public int getId() {
			return this.id;
		}

		@Override
		public int getTitleRes() {
			return this.titleRes;
		}

		public CountryCode getCountryCode() {
			return this.countryCode;
		}

		public LatLng getCoords() {
			return this.coords;
		}

		@Override
		public int getThumbRes() {
			return 0;
		}

		@Override
		public int getSecondThumbRes() {
			return this.countryCode.getThumbRes();
		}
	}

	/**
	 * time units to measure elapsed time
	 * 
	 * @author Pavel_Letsiaha
	 * 
	 */
	public enum EtaUnit implements Titled {
		YEAR(DateUtils.YEAR_IN_MILLIS, R.string.lbl_short_year), WEEK(
				DateUtils.WEEK_IN_MILLIS, R.string.lbl_short_week), DAY(
				DateUtils.DAY_IN_MILLIS, R.string.lbl_short_day), HOUR(
				DateUtils.HOUR_IN_MILLIS, R.string.lbl_short_hour), MINUTE(
				DateUtils.MINUTE_IN_MILLIS, R.string.lbl_short_minute);

		private final long period;
		private final int titleRes;

		private EtaUnit(long period, int titleRes) {
			this.period = period;
			this.titleRes = titleRes;
		}

		public long getPeriod() {
			return this.period;
		}

		@Override
		public int getTitleRes() {
			return this.titleRes;
		}

	}

	public static Integer calcMapCode(ProviderCode providerCode,
			RateType rateType) {
		int hash = 7;

		hash = 31 * hash + (null == providerCode ? 0 : providerCode.hashCode());
		hash = 31 * hash + (null == rateType ? 0 : rateType.hashCode());

		return hash;
	}

	public static Integer calcMapCode(ProviderRequirements requirements) {

		Integer res = null;
		if (requirements != null) {
			res = calcMapCode(requirements.getProviderCode(),
					requirements.getRateType());
		}

		return res;
	}
}