package ch.prokopovi.exported;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

public class PureConst {

	public static final String ASSETS_DB_POSTFIX_FORMAT = "000";
	public static final int ASSETS_DB_PARTS_NUMBER = 3;

	// city ids
	public static final int MINSK_ID = 0;
	public static final int GRODNO_ID = 1;
	public static final int BREST_ID = 2;
	public static final int VITEBSK_ID = 3;
	public static final int GOMEL_ID = 4;
	public static final int MOGILEV_ID = 5;

	public static final int BARANOVICHI_ID = 20;
	public static final int BOBRUISK_ID = 21;
	public static final int BORISOV_ID = 22;
	public static final int LIDA_ID = 23;
	public static final int MOZIR_ID = 24;
	public static final int NOVOPOLOCK_ID = 25;
	public static final int ORSHA_ID = 26;
	public static final int PINSK_ID = 27;
	public static final int POLOCK_ID = 28;
	public static final int SOLIGORSK_ID = 29;

	public static final int MOLODZE4NO_ID = 30;
	public static final int SVETLOGORSK_ID = 31;
	public static final int ZLOBIN_ID = 32;
	public static final int RE4ICA_ID = 33;
	public static final int SLUCK_ID = 34;
	public static final int ZODINO_ID = 35;


    // TODO
    //public static final int VILEIKA_ID = 36;
//    37">Дзержинск
//    38">Марьина Горка
//    39">Горки
//
//    40">Осиповичи
//    41">Кричев
//    42">Калинковичи
//    43">Рогачев
//    44">Кобрин
//    45">Береза
//    46">Лунинец
//    47">Ивацевичи
//    48">Слоним
//    49">Волковыск
//
//    50">Сморгонь
//    51">Новогрудок

	public static final int KIEV_ID = 1000;
	public static final int ODESSA_ID = 1001;
	public static final int DONECK_ID = 1002;
	public static final int LVOV_ID = 1003;
	public static final int DNEPROPETROVSK_ID = 1004;

	//

	public enum Bank {
		// belarus
		ABSOLUT(1001, "Абсолютбанк"), //
		ALFA(1002, "Альфа-Банк"), //
		BBMB(1003, "Белорусский Банк Малого Бизнеса"), //
		BEL_VEB(1004, "БелВЭБ"), //
		VTB(1005, "Банк ВТБ"), //
		MM(1006, "Банк Москва-Минск"), //
		BELAGROPROM(1007, "Белагропромбанк"), //
		BELARUS(1008, "Беларусбанк"), //
		BEL_GAZPROM(1009, "Белгазпромбанк"), //
		BEL_INVEST(1010, "Белинвестбанк"), //
		BEL_SWISS(1011, "БелСвиссБанк"), //
		BIT(1012, "БИТ-Банк"), //
		BNB(1013, "БНБ-Банк"), //
		BPS(1014, "БПС-Сбербанк"), //
		BTA(1015, "БТА Банк"), //
		DELTA(1016, "Дельта Банк"), //
		EVRO(1017, "Евробанк"), //
		EVROTORGINVEST(1018, "Евроторгинвестабанк", "Евроторгинвестбанк"), //
		IDEA(1019, "Идея Банк"), //
		INTER_PAY(1020, "ИнтерПэйБанк"), //
		MTB(1021, "МТБанк", "МТБ Банк"), //
		NORD_EUROPEAN(1022, "Норд Европеан Банк"), //
		PARITET(1023, "Паритетбанк"), //
		PRIOR(1024, "Приорбанк"), //
		RRB(1025, "РРБ-Банк"), //
		TEHNO(1026, "Технобанк"), //
		TK(1027, "ТК Банк"), //
		TRUST(1028, "Трастбанк"), //
		FRANS(1029, "Франсабанк"), //
		HOME_CREDIT(1030, "Хоум Кредит Банк", "ХКБанк"), //
		ZEPTER(1031, "Цептер Банк", "Цептер-Банк"), //

		// ukraine
		CITY_COMMERCE(2001, "CityCommerceBank"), //
		AVANT(2002, "АВАНТ-БАНК"), //
		BM(2003, "БМ Банк"), //
		DIAMANT(2005, "Диамантбанк"), //
		CREDI_AGRIKOL(2006, "Креди Агриколь Банк"), //
		LEG(2007, "Легбанк"), //
		NATIONAL_CREDIT(2008, "Национальный Кредит"), //
		PROMINVEST(2009, "Проминвестбанк"), //
		REAL(2010, "РЕАЛ БАНК"), //
		SIC(2011, "Сич"), //
		TASCOM(2012, "ТАСкомбанк"), //
		TRAST(2013, "ТРАСТ"), //
		UKRGAZ(2014, "Укргазбанк"), //
		UKRGAZPROM(2015, "Укргазпромбанк"), //
		UKRSIB(2016, "УкрСиббанк"), //
		UKREKSIM(2017, "Укрэксимбанк"), //
		UPB(2018, "УПБ"), //
		FIN_INIT(2019, "Финансовая Инициатива"), //
		FINROST(2020, "Финростбанк"), //
		FORUM(2021, "Форум"), //
		UNEX(2022, "ЮНЕКС БАНК"), //
		;

		private final int id;
		private final Set<String> titleSet;

		private Bank(int id, String... titles) {

			this.id = id;
			this.titleSet = new HashSet<String>();
			for (String title : titles) {
				this.titleSet.add(title.toLowerCase(Locale.US));
			}
		}

		/**
		 * find one of bank titles inside wider string
		 * 
		 * @param line
		 * @return
		 */
		public static Bank getByPart(String line) {
			if (line == null)
				return null;

			String lowerCaseTitle = line.toLowerCase(Locale.US);

			for (Bank b : Bank.values()) {
				Iterator<String> iter = b.titleSet.iterator();
				while (iter.hasNext()) {
					String bankTitle = iter.next();
					if (lowerCaseTitle.contains(bankTitle)) {
						return b;
					}
				}
			}

			return null;
		}

		/**
		 * find bank by exact match of one of titles
		 * 
		 * @param title
		 * @return
		 */
		public static Bank get(String title) {
			if (title == null)
				return null;

			String lowerCaseTitle = title.toLowerCase(Locale.US);

			for (Bank b : Bank.values()) {
				if (b.titleSet.contains(lowerCaseTitle)) {
					return b;
				}
			}

			return null;
		}

		public int getId() {
			return this.id;
		}
	}

	/**
	 * myfin region codes
	 * 
	 * @author public
	 * 
	 */
	public enum MyfinRegion {

		MINSK(1, MINSK_ID), //
		VITEBSK(2, VITEBSK_ID), //
		GOMEL(3, GOMEL_ID), //
		GRODNO(4, GRODNO_ID), //
		BREST(5, BREST_ID), //
		MOGILEV(6, MOGILEV_ID),

		BARANOVICHI(20, BARANOVICHI_ID), //
		BOBRUISK(21, BOBRUISK_ID), //
		BORISOV(22, BORISOV_ID), //
		LIDA(23, LIDA_ID), //
		MOZIR(24, MOZIR_ID), //
		NOVOPOLOCK(25, NOVOPOLOCK_ID), //
		ORSHA(26, ORSHA_ID), //
		PINSK(27, PINSK_ID), //
		POLOCK(28, POLOCK_ID), //
		SOLIGORSK(29, SOLIGORSK_ID), //

		MOLODZE4NO(30, MOLODZE4NO_ID), //
		SVETLOGORSK(31, SVETLOGORSK_ID), //
		ZLOBIN(32, ZLOBIN_ID), //
		RE4ICA(33, RE4ICA_ID), //
		SLUCK(34, SLUCK_ID), //
		ZODINO(35, ZODINO_ID), //


        // TODO
//        36">Вилейка
//        37">Дзержинск
//        38">Марьина Горка
//        39">Горки
//
//        40">Осиповичи
//        41">Кричев
//        42">Калинковичи
//        43">Рогачев
//        44">Кобрин
//        45">Береза
//        46">Лунинец
//        47">Ивацевичи
//        48">Слоним
//        49">Волковыск
//
//        50">Сморгонь
//        51">Новогрудок

		;

		private final int id;
		private final int masterId;

		private MyfinRegion(int id, int masterId) {
			this.id = id;
			this.masterId = masterId;
		}

		public int getId() {
			return this.id;
		}

		public int getMasterId() {
			return this.masterId;
		}

		public static MyfinRegion get(int masterId) {
			for (MyfinRegion tmp : values()) {
				if (tmp.getMasterId() == masterId) {
					return tmp;
				}
			}
			return null;
		}
	}

	public enum FinanceUaRegion {

		KIEV("7oiylpmiow8iy1smadi", KIEV_ID), //
		ODESSA("7oiylpmiow8iy1smadk", ODESSA_ID), //
		DONECK("7oiylpmiow8iy1smaee", DONECK_ID), //
		LVOV("7oiylpmiow8iy1smadr", LVOV_ID), //
		DNEPROPETROVSK("7oiylpmiow8iy1smadm", DNEPROPETROVSK_ID), //
		;

		private final String uid;
		private final int regionId;

		private FinanceUaRegion(String uid, int masterId) {
			this.uid = uid;
			this.regionId = masterId;
		}

		public static FinanceUaRegion get(String uid) {
			for (FinanceUaRegion tmp : values()) {
				if (tmp.uid.equals(uid)) {
					return tmp;
				}
			}

			return null;
		}

		public static FinanceUaRegion get(int masterId) {
			for (FinanceUaRegion tmp : values()) {
				if (tmp.regionId == masterId) {
					return tmp;
				}
			}
			return null;
		}

		public String getUid() {
			return this.uid;
		}

		public int getRegionId() {
			return this.regionId;
		}
	}

	public static int financeUaPlaceIdTransform(String uid) {
		// final String uidPrefix = "7oiylpmiow8iy1sm";

		byte[] bytes = uid.getBytes();
		int length = bytes.length;

		byte[] b = new byte[4];
		System.arraycopy(bytes, length - 4, b, 0, 4);

		final int id = ByteBuffer.wrap(b).getInt() % 1000000;

		return id;
	}

	public static Integer toPositiveHashCode(String str) {

		return str != null ? Math.abs(str.hashCode()) : null;
	}
}
