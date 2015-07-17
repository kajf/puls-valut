package ch.prokopovi.auxiliary;

import ch.prokopovi.exported.PureConst.Bank;


class Place {

	private static final String[] CITIES = new String[] { "Минск", "Гродно",
			"Брест", "Витебск", "Гомель", "Могилев", "Барановичи", "Бобруйск",
			"Борисов", "Лида", "Мозырь", "Новополоцк", "Орша", "Пинск",
			"Полоцк", "Солигорск", "Молодечно", "Светлогорск", "Жлобин",
			"Речица", "Слуцк", "Жодино", "Киев", "Одесса", "Донецк", "Львов",
			"Днепропетровск" };

	int id;
	Integer regionId;
	Bank bank;
	String name;
	Double x;
	Double y;
	String addr;
	String wh;
	String phone = "";

	public Place(int id, Integer regionId, Bank bank, String name, Double x,
			Double y, String addr, String wh, String phone) {
		super();

		this.id = id;
		this.regionId = regionId;
		this.bank = bank;
		this.name = shortenName(name);
		this.x = x;
		this.y = y;
		this.addr = shortenAddr(addr);
		this.wh = shortenWorkHours(wh);
        this.phone = cleanUpPhone(phone);
    }

	public Place(int id, Bank bank, String name,
			Double x, Double y, String addr, String wh) {

		this(id, -1, bank, name, x, y, addr, wh, null);
	}

	private static String shortenAddr(String addr){

        if (addr == null)
            return addr;

        String res = addr.replace("г.", "").replace("Г.", "").trim();

		for (String city : CITIES) {
			String prefix1 = city + ", ";
			if (res.startsWith(prefix1)) {
				res = res.replace(prefix1, "").trim();
				break;
			}

			String prefix2 = city + " ";
			if (res.startsWith(prefix2)) {
				res = res.replace(prefix2, "").trim();
				break;
			}
		}

		res = res.replace("проспект", "пр.").replace("пр-т", "пр.");

		res = commonCleanUp(res);

		return res;
	}

	private static String shortenName(String nm) {
        if (nm == null)
            return nm;

        final String pov = "ПОВ";
		final String cbu = "ЦБУ";

		String res = commonCleanUp(nm);

		res = res.replace("Центр банковских услуг", cbu)
				//
				.replace("Центр Банковских услуг", cbu)
				//
				.replace("Центр Банковских Услуг", cbu)
				//
				.replace("Обменный пункт", pov)
				//
				.replace("Пункт обмена валют", pov)
				//
				.replace("Операционная касса", "ОК")
				//
				.replace("Расчетно-кассовый центр", "РКЦ")
				.replace("Расчётно-кассовый центр", "РКЦ") //
				.replace("г. ", "").replace("г.", "");

		res = commonCleanUp(res);

		return res;
	}

	/**
	 * shorten some words (weekdays) in provided string
	 *
	 * @param wh
	 * @return
	 */
	static String shortenWorkHours(String wh) {
        if (wh == null)
            return wh;

        String res = commonCleanUp(wh);

        String[] days = new String[] {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};

        // (?ui) case insensitive pattern modifier

        res = res.replace("Время работы:", "")
				.trim()
				.replaceAll("(?ui)понедельник", days[0])
                .replaceAll("(?ui)вторник", days[1])
                .replaceAll("(?ui)среда", days[2])
                .replaceAll("(?ui)четверг", days[3])
                .replaceAll("(?ui)пятница", days[4])
                .replaceAll("(?ui)суббота", days[5])
                .replaceAll("(?ui)воскресенье", days[6]);


        for (String day : days) {
          res = res
              .replaceAll("(?ui)"+ day + "\\.", day) // пн. -> Пн
              .replaceAll("(?ui)" + day + "-", day + "-") // пн- -> Пн-
              .replaceAll("(?ui)" + "-" + day, "-" + day) // -пт -> -Пт
              .replace(day.toUpperCase(), day);
        }

		String brake = "тех. пер.";
		String lunch = "обед";
		String allHours = "круглосут.";
		String dayOff = "вых.";
		String allDays = "ежедн.";

		res = res.replaceAll("(?ui)выходной", dayOff) //
				.replaceAll("(?ui)ежедневно", allDays) //
				.replaceAll("(?ui)технический перерыв", brake) //
				.replaceAll("(?ui)технические перерывы", brake) //
				.replaceAll("(?ui)технологические перерывы", brake) //
				.replaceAll("(?ui)обеденный перерыв", lunch) //
				.replaceAll("(?ui)последний рабочий день месяца", "посл. раб. день мес.")
				.replaceAll("(?ui)исключения в режиме работы:", "")
				.replaceAll("(?ui)круглосуточно", allHours);

		res = commonCleanUp(res);

		return res;
	}

	static String cleanUpPhone(String phone) {
		String[] prefixes = new String[] {
				"15", "152", "154", "1545",
				"16", "162", "163", "1642", "165", "1653",
				"17", "174", "176", "177", "1742", "1775", "1777", "1795",
				"21", "212", "214", "216",
				"22", "222", "225",
				"23", "232", "2332", "2334", "2339", "2340", "2342", "236", "2363",
                "25",
                "29", "291",
				"33",
				"44"};

		String comma = ", ";

		if (phone == null)
			return phone;

		String res = commonCleanUp(phone);

		String [] replaceArr = new String [] {//
				"Контакт центр", "контакт центр:",
				"(круглосуточно)",
				"– единый городской номер",
				"(МТС, Life:), Velcom)", "(velcom, МТС и life)", "для абонентов velcom, МТС и life:)", "для velcom, МТС, life:)", //
				"(МТС)", "МТС",
				"(факс)", "факс:", "тел/факс", "Факс:",
				"(Минск)"};
		for (String s : replaceArr) {
			res = res.replace(s, "");
		}

        String[] brs = new String[] {"<br>", "<br />", "<br/>", "</br>", "&lt;br&gt;", "&lt;br /&gt;", "&lt;/br&gt;"};

        for (String br : brs) {
          res = res.replace(br, comma);
        }

		if (res.endsWith(comma)) {
			res = res.substring(0, res.length() - comma.length());
		} // remove trailing comma

		res = res //
				.replace(
						" (круглосуточно, звонок со стационарного телефона бесплатно)",
						"") //
				.replace(" (бесплатно со стационарного телефона)", "");

        res = res
				.replace("+ ", "+")
        		.replace("-", " ")
				.replace("8+375", "+375");

        for (String prefix : prefixes) {
			res = res
                    .replace("(+375 " + prefix + ")", "+375 " + prefix)
                    .replace("+375 (0" + prefix + ")", "+375 " + prefix)
					.replace("(+375 0" + prefix + ")", "+375 " + prefix)
					.replace("+375 ("+prefix + ")", "+375 " + prefix)
					.replace("+375(" + prefix + ")", "+375 " + prefix)

					.replace("(8 0" + prefix + ")", "+375 " + prefix)
                    .replace("8 0" + prefix, "+375 " + prefix)
                    .replace("(80" + prefix + ")", "+375 " + prefix)
                    .replace("8 (0"+prefix + ")", "+375 " + prefix)

					.replace("(0"+prefix+")", "+375 " + prefix);
		}// un-brecket prefixes

		res = commonCleanUp(res);

		return res;
	}

	static String commonCleanUp(String str) {
		if (str == null)
			return str;

		String comma = ", ";

        String res = str.trim() //
                .replaceAll("\t", " ") // tabs
				.replaceAll(" +", " ") // replace 2 or more spaces with single
                        // space
                .replace("..", ".")
                .replace(",,", ",")
                .replace(" -", "-")
                .replace("- ", "-")
                .replace(" —", "—")
                .replace("— ",
            "—").replace(" " + comma, comma); // replace comma leading spaces


		return res;
	}

	public int getId() {
		return id;
	}

	public Integer getRegionId() {
		return regionId;
	}

	public Bank getBank() {
		return bank;
	}

	public String getName() {
		return name;
	}

	public Double getX() {
		return x;
	}

	public Double getY() {
		return y;
	}

	public String getAddr() {
		return addr;
	}

	public String getWh() {
		return wh;
	}

	public String getPhone() {
		return phone != null ? phone : "";
	}

    public void updatePhoneWith(String newPhone) {

        newPhone = cleanUpPhone(newPhone);

        if (phone == null) {
            phone = newPhone;
        } else if (!phone.contains(newPhone)) {
            phone = newPhone + ", " + phone;
        }

        this.phone = cleanUpPhone(phone);
	}

	public void setRegionId(Integer regionId) {
		this.regionId = regionId;
	}

    public void setBank(Bank b) {
        this.bank = b;
    }

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Place [id=");
		builder.append(id);
		builder.append(", regionId=");
		builder.append(regionId);
		builder.append(", bank=");
		builder.append(bank);
		builder.append(", name=");
		builder.append(name);
		builder.append(", x=");
		builder.append(x);
		builder.append(", y=");
		builder.append(y);
		builder.append(", addr=");
		builder.append(addr);
		builder.append(", wh=");
		builder.append(wh);
		builder.append(", phone=");
		builder.append(phone);
		builder.append("]");
		return builder.toString();
	}
}
