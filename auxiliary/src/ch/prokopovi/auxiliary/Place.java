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
		setPhone(phone);
	}

	public Place(int id, Bank bank, String name, 
			Double x, Double y, String addr, String wh) {
		
		this(id, -1, bank, name, x, y, addr, wh, null);		
	}
	
	private static String shortenAddr(String addr){
		
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
	private static String shortenWorkHours(String wh) {

		String res = commonCleanUp(wh);
		
		String mon = "Пн";
		String tue = "Вт";
		String wed = "Ср";
		String thu = "Чт";
		String fri = "Пт";
		String sat = "Сб";
		String sun = "Вс";

		res = res
				.replace("Время работы:", "")
				.trim()
				.replace("понедельник", mon)
				.replace("Понедельник", mon)
				.replace("Пн.", mon)
				//
				.replace("вторник", tue)
				.replace("Вторник", tue)
				.replace("Вт.", tue)
				//
				.replace("среда", wed)
				.replace("Среда", wed)
				.replace("Ср.", wed)
				//
				.replace("четверг", thu)
				.replace("Четверг", thu)
				.replace("Чт.", thu)
				//
				.replace("пятница", fri)
				.replace("Пятница", fri)
				.replace("Пт.", fri)
				//
				.replace("суббота", sat).replace("Суббота", sat)
				.replace("Сб.", sat)
				//
				.replace("воскресенье", sun).replace("Воскресенье", sun)
				.replace("Вс.", sun);

		String brake = "тех. пер.";
		String lunch = "обед";
		String allHours = "круглосут.";
		String dayOff = "вых.";
		String allDays = "ежедн.";

		res = res.replace("выходной", dayOff).replace("Выходной", dayOff) //
				.replace("ежедневно", allDays).replace("Ежедневно", allDays) //
				.replace("технический перерыв", brake) //
				.replace("Технический перерыв", brake) //
				.replace("технические перерывы", brake) //
				.replace("Технические перерывы", brake) //
				.replace("технологические перерывы", brake) //
				.replace("обеденный перерыв", lunch) //
				.replace("Обеденный перерыв", lunch) //
				.replace("последний рабочий день месяца", "посл. раб. день мес.")
				.replace("Исключения в режиме работы:", "")
				.replace("круглосуточно", allHours) //
				.replace("Круглосуточно", allHours);

		res = commonCleanUp(res);

		return res;
	}
	
	private static String cleanUpPhone(String phone) {
		String br = "<br>";
		String br2 = ";&lt;br /&gt; ";	
		String br3 = "&lt;br&gt; ";
		String br4 = "&lt;br&gt;";
				
		String[] prefixes = new String[] {												 
				"15", "152", "154", "1545",
				"16", "162", "163", "165", "1653",
				"17", "174", "176", "177", "1742", "1775", "1777", "1795", 				
				"21", "212", "214", "216",  
				"22", "222", "225",  
				"23", "232", "2334", "2339", "2340", "2342", "236", "2363",
				"29", "291",
				"33", 
				"44"};
				
		String comma = ", ";
		
		if (phone == null)
			return phone;
		
		String res = commonCleanUp(phone);

		String [] replaceArr = new String [] {"(круглосуточно)", "(МТС, Life:), Velcom)", "(velcom, МТС и life)",
				"(факс)", "факс:", "тел/факс", "Факс:"};
		for (String s : replaceArr) {
			res = res.replace(s, "");
		}
		
		res = res.replace(br, comma).replace(br2, comma).replace(br3, comma).replace(br4, comma);
		
		if (res.endsWith(comma)) {
			res = res.substring(0, res.length() - comma.length());
		} // remove trailing comma
		
		res = res //
				.replace(
						" (круглосуточно, звонок со стационарного телефона бесплатно)",
						"") //
				.replace(" (бесплатно со стационарного телефона)", "");
				
		for (String prefix : prefixes) {
			res = res
					.replace("(+375 "+prefix+")", "+375 " + prefix)
					.replace("+375 (0"+prefix+")", "+375 " + prefix)
					.replace("(8-0"+prefix+")", "+375 " + prefix)
					.replace("(8 0"+prefix+")", "+375 " + prefix)
					.replace("(80"+prefix+")", "+375 " + prefix)
					.replace("8 (0"+prefix+")", "+375 " + prefix)
					.replace("(0"+prefix+")", "+375 " + prefix)
					.replace("("+prefix+")", prefix);	
		}// un-brecket prefixes

		res = commonCleanUp(res);

		return res;
	}
	
	private static String commonCleanUp(String str) {
		if (str == null)
			return str;
		
		String comma = ", ";
				
		String res = str.trim() //
				.replaceAll("\t", " ") // tabs
				.replaceAll(" +", " ") // replace 2 or more spaces with single
						// space
				.replace(" -", "-").replace("- ", "-")
				.replace(" —", "—").replace("— ", "—")
				.replace(" " + comma, comma); // replace comma leading spaces
		
	
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

	public void setPhone(String phone) {		
		this.phone = cleanUpPhone(phone);
	}		

	public void setRegionId(Integer regionId) {
		this.regionId = regionId;
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
