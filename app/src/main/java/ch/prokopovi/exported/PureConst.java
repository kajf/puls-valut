package ch.prokopovi.exported;

import java.util.*;

public class PureConst {

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


	public enum Bank {
		// belarus
		ABSOLUT(1001, "Абсолютбанк"), //
		ALFA(1002, "Альфа-Банк"), //
		BBMB(true, 1003, "Белорусский Банк Малого Бизнеса"), //
		BEL_VEB(1004, "БелВЭБ"), //
		VTB(1005, "Банк ВТБ"), //
		MM(1006, "Банк Москва-Минск"), //
		BELAGROPROM(1007, "Белагропромбанк"), //
		BELARUS(1008, "Беларусбанк"), //
		BEL_GAZPROM(1009, "Белгазпромбанк"), //
		BEL_INVEST(1010, "Белинвестбанк"), //
		BEL_SWISS(1011, "БелСвиссБанк", "БСБ Банк"), //
		BIT(true, 1012, "БИТ-Банк"), //
		BNB(1013, "БНБ-Банк"), //
		BPS(1014, "БПС-Сбербанк"), //
		BTA(1015, "БТА Банк"), //
		DELTA(true, 1016, "Дельта Банк"), //
		EVRO(true, 1017, "Евробанк"), //
		EVROTORGINVEST(1018, "Евроторгинвестабанк", "Евроторгинвестбанк"), //
		IDEA(1019, "Идея Банк"), //
		INTER_PAY(true, 1020, "ИнтерПэйБанк"), //
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
		;

		private final int id;
		private final Set<String> titleSet;
		public final boolean deprecated;

		Bank(int id, String... titles) {
			this(false, id, titles);
		}

		Bank(boolean deprecated, int id, String... titles) {

			this.id = id;
			this.titleSet = new HashSet<>();
			for (String title : titles) {
				this.titleSet.add(title.toLowerCase(Locale.US));
			}
			this.deprecated = deprecated;
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

		MyfinRegion(int id, int masterId) {
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
}
