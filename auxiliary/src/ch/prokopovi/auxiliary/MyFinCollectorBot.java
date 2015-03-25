package ch.prokopovi.auxiliary;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ch.prokopovi.exported.PureConst.Bank;
import ch.prokopovi.exported.PureConst.MyfinRegion;

public class MyFinCollectorBot extends AbstractCollectorBot {

	private static final String OUT_FILE = "output.sql";
	private static String URL_POISK = "http://myfin.by/banki/poisk";
	private static String URL_LOCATION = "http://myfin.by/banki/location/565";
	private static String URL_SERVICE_FMT = "http://myfin.by/scripts/xml_new/work/banks_city_%d.xml";

	private static String[][] buildLocationParams(int cityId) {

		String[][] params = new String [][] {
			{"city", String.valueOf(cityId)},
			{"operation", "sell"},
			{"currency", "usd"},
			{"range", "5000"}
		};
		return params;
	}

	private static String[][] buildPoiskParams(int cityId) {

		String[][] params = new String [][] {
				{"Mapobject[city_id]", String.valueOf(cityId)},
				{"Mapobject[filial_type_id][]", "1"},
				{"Mapobject[filial_type_id][]", "3"},
				//{"Mapobject[filial_type_id][]", "4"},
				{"Mapobject[filial_type_id][]", "5"},
		};
		
		return params;
	}

	private static Place parse(Integer uid, JSONObject place, HtmlCleaner cleaner, String whXpath) throws JSONException, XPatherException {
		JSONArray coords = 
				place.getJSONArray("coords");
		
		Double cx = coords.getDouble(0);
		if (cx == null)
			return null;

		Double cy = coords.getDouble(1);
		if (cy == null)
			return null;
		
		if (isZero(cx) && isZero(cy))
			return null;

		String name = place.getString("name");
		if (name == null)
			return null;

		String adr = place.getString("adr");
		if (adr == null)
			return null;
		
		String wh = extractTag(cleaner, place.getString("main"), whXpath);
				
		//wh = wh.replace("Время работы:", "").trim();
		
		// System.out.println("uid[" + uid + "] bank[" + bank +
		// "] type[" + type + "] addr[" + addr + "] wh[" + wh + "]");
		
		name = name.trim();
		adr = adr.trim();		
		if (uid == null)		
			uid = Math.abs((name+adr).hashCode());
		
		Bank bank = Bank.getByPart(name);
		if (bank == null) {
			System.out.printf("- bank for [%s] is not found \n", name);
		}

		Place p = new Place(uid, bank, name, cx, cy, adr, wh);
		
		return p;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Throwable {

		System.out.println("--- start ---");

		List<Place> res = new ArrayList<Place>();

		HtmlCleaner cleaner = initCleaner();

		updateFromLocation(res, cleaner);
		updateFromPoisk(res, cleaner);
		
		List<Place> uniqList = clenaupCopies(res);
		
		// sort
		Collections.sort(uniqList, new PlaceIdComparator());
		
		System.out.println();
		
		StringBuilder script = new StringBuilder();
		for (Place place : uniqList) {
			if (place.getRegionId() == null || "".equals(place.getPhone())) {
				System.out.println("!!! NO MATCH " + place);
			}

			script.append(getInsert(place));
		}
		
		/*System.out.println();
		System.out.println("--- script begin ---");
		System.out.println(script.toString());
		System.out.println("--- script end ---");*/
		
		toFile(OUT_FILE, script.toString());
				
		System.out.println("--- end ---");
	}
	
	private static boolean isFilterPassed(Place p) {
		if (p == null)
			return false;
		
		String name = p.getName().toLowerCase();
		
		String atm = "банкомат";
		String kiosk = "инфокиоск";
		
		if (name.contains(atm)) {
			System.out.println("atm fileter is not passed: "+p);
			return false;
		} else if (name.contains(kiosk)) {
			System.out.println("kiosk fileter is not passed: "+p);
			return false;
		}
		
		return true;	
	}
		
	private static List<Place> clenaupCopies(List<Place> res){
		
		System.out.println("### removing copies ...");
		
		List<Place> newList = new ArrayList<Place>();
		
		Iterator<Place> it = res.iterator();
		while (it.hasNext()) {
			Place p = it.next();
			
			Place p2 = find(p.getName(), p.getAddr(), p.getX(), p.getY(), newList);				
			if (p2 != null) {
				System.out.println("> copy found: ");
				System.out.println(p);
				System.out.println(p2);				
			} else {
				newList.add(p);
			}
		}		
		
		return newList;
	}
	
	private static void updateFromLocation(Collection<Place> res, HtmlCleaner cleaner) throws Exception {
		
		System.out.println("### loading cities ...");

		// location page
		//String[][] params = buildLocationParams(MyfinRegion.MINSK.getId());
		String post = get(URL_LOCATION);

		String strBegin = "myPoints = ";
		int from = post.indexOf(strBegin) + strBegin.length();
		int to = post.indexOf("myPoints.forEach(function (point)");

		String strJson = post.substring(from, to).trim();

		JSONArray places = new JSONArray(strJson);
		for (int i = 0; i < places.length(); i++) {
			JSONObject place = places.getJSONObject(i);

			int uid = place.getInt("id");
			
			Place p = parse(uid, place, cleaner, "//p[2]/text()");
						
			if (!isFilterPassed(p))
				continue;
			
			res.add(p);
		}
		System.out.printf(" %s loaded. ", res.size());

		System.out.println();
		
	}
	
	private static void updateFromPoisk(Collection<Place> res, HtmlCleaner cleaner) throws Exception {
		System.out.println("### loading all cities ...");
		
		for (MyfinRegion city : MyfinRegion.values()) {

			System.out.printf("%s, \n", city.name());
			
			// poisk page
			String[][] poiskParams = buildPoiskParams(city.getId());
			String postPoisk = post(URL_POISK, poiskParams);

			String strBeginPoisk = "myPoints = ";
			int fromPoisk = postPoisk.indexOf(strBeginPoisk)
					+ strBeginPoisk.length();
			int toPoisk = postPoisk
					.indexOf("myPoints.forEach(function (point)");

			String strJsonPoisk = postPoisk.substring(fromPoisk, toPoisk)
					.trim();

			JSONArray placesPoisk = new JSONArray(strJsonPoisk);
			for (int i = 0; i < placesPoisk.length(); i++) {
				JSONObject placePoisk = placesPoisk.getJSONObject(i);
				
//				{ 
//					coords: [55.193574,30.203313],
//					adr: 'г. Витебск, ул. Замковая, 4', 
//					main: '<p><a href="/bank/bvebank/o_banke"><img src="/images/bank_logos/bveb.png" width="100px"/></a></p><p>Время работы:</p><p>Пн — чт: 9:00 — 19:00, пт:9:00 — 18:00</p>',
//					name: 'Витебское региональное отделение ОАО "Банк БелВЭБ"',
//					type: 'Главное отделение',
//					phone: '205 (МТС, Life:), Velcom)<br>+375 (17) 209 29 44', 
//					icon: '/images/bank_logos/icons/bveb.png' 
//				}				

				
				Place parsedPlace = parse(null, placePoisk, cleaner, "//p[3]/text()");			
				if (!isFilterPassed(parsedPlace))
					continue;
								
				// find existing
				Place place = find(parsedPlace.getName(), parsedPlace.getAddr(), parsedPlace.getX(), parsedPlace.getY(), res);
				if (place == null) {
					//continue;
					place = parsedPlace;
					res.add(place);
				}
				
				place.setRegionId(city.getMasterId());
				
				String strPhone = placePoisk.getString("phone");
				place.updatePhoneWith(strPhone);
				
			}
			
			// update phones
			updateFromService(res, city);
		}	
				
		System.out.printf("%s loaded. ", res.size());
	}

	private static HtmlCleaner initCleaner() {
		HtmlCleaner cleaner = new HtmlCleaner();
		CleanerProperties props = cleaner.getProperties();
		props.setAllowHtmlInsideAttributes(true);
		props.setOmitComments(true);
		props.setNamespacesAware(false);
		return cleaner;
	}
	
	private static void updateFromService(Iterable<Place> places, MyfinRegion city) throws Exception {
		
		// using map to search by id
		HashMap<Integer,Place> map = new HashMap<Integer, Place>();
		for (Place place : places) {
			map.put(place.getId(), place);
		}
		//
		
		String location = String.format(URL_SERVICE_FMT, city.getId());
		URL url = new URL(location);
						
		HtmlCleaner cleaner = new HtmlCleaner();
		TagNode root = cleaner.clean(url).findElementByName("root", true);
				
		TagNode[] bankNodes = root.getAllElements(false);		
		for (TagNode bankNode : bankNodes) {	
			
//			<bank>
	//			<bankid>5</bankid>
	//			<filialid>1143</filialid>
	//			<date>17.01.2014</date>
	//			<bankname>ЗАО "Абсолютбанк"</bankname>
	//			<bankaddress>
	//				Минский район, 9-ый км.Московского шоссе (м-н "Виталюр")
	//			</bankaddress>
	//			<bankphone>+375 (17) 266 06 79</bankphone>
	//			<filialname>Касса №6 ЗАО "Абсолютбанк"</filialname>
	//			<usd_buy>9530</usd_buy>
	//			<usd_sell>9620</usd_sell>
	//			<eur_buy>12940</eur_buy>
	//			<eur_sell>13120</eur_sell>
	//			<rur_buy>281</rur_buy>
	//			<rur_sell>288</rur_sell>
	//			<pln_buy>3080</pln_buy>
	//			<pln_sell>3150</pln_sell>
	//			<eurusd_buy>1.3477</eurusd_buy>
	//			<eurusd_sell>1.374</eurusd_sell>
//			</bank>			
			
			TagNode filialIdNode = bankNode.findElementByName("filialid", false);
			Integer filialId = Integer.valueOf(filialIdNode.getText().toString());
			
			Place place = map.get(filialId);
			if (place == null) {
				System.out.printf("!!! place: %d is not found list but exists in service xml.\n", filialId);
				continue;
			}
			
			place.setRegionId(city.getMasterId());
		
//			<bank>
	//			<bankid>29</bankid>
	//			<filialid>1491</filialid>
	//			<date>30.12.2013</date>
	//			<bankname>ЗАО «Идея Банк»</bankname>
	//			<bankaddress>
	//				г. Минск, пр-т Партизанский (ст. метро "Партизанская"), ТЦ "Подземный город"
	//			</bankaddress>
	//			<bankphone>+375 (17) 346 91 91</bankphone>
	//			<filialname>Пункт обмена валют №3 ЗАО "Идея Банк"</filialname>
	//			<usd_buy>9500</usd_buy>
	//			<usd_sell>9590</usd_sell>
	//			<eur_buy>13050</eur_buy>
	//			<eur_sell>13250</eur_sell>
	//			<rur_buy>287.5</rur_buy>
	//			<rur_sell>294</rur_sell>
	//			<pln_buy>-</pln_buy>
	//			<pln_sell>-</pln_sell>
	//			<eurusd_buy>1.363</eurusd_buy>
	//			<eurusd_sell>1.389</eurusd_sell>
//			</bank>			
			
			TagNode phoneNode = bankNode.getElementsByName("bankphone", false)[0];
			String text = phoneNode.getText().toString();
			
			if (!text.isEmpty() && !"-".equals(text)) {

        place.updatePhoneWith(text);
			}
		}
	}

	private static String extractTag(HtmlCleaner cleaner, String src, String xpath) throws JSONException, XPatherException{
		TagNode tagNode = cleaner.clean(src);

		Object[] whNodes = tagNode.evaluateXPath(xpath);

		String wh = "";
		if (whNodes.length > 0) {
			wh = whNodes[0].toString();
		}
		
		return wh;
	}
	
	private static Place find(String name, String addr, Double x, Double y,
			Iterable<Place> list) {
		
		double trh = 0.0001;

		for (Place place : list) {

			Double xDiff = place.getX() - x;
			Double yDiff = place.getY() - y;

			boolean xEq = (-trh <= xDiff && xDiff <= trh);
			boolean yEq = (-trh <= yDiff && yDiff <= trh);

			boolean sameName = place.getName().equals(name);
			boolean sameCoords = xEq && yEq;
			boolean sameAddr = place.getAddr().equals(addr);
			if (
					sameName &&  (sameCoords || sameAddr)
				) {
				return place;
			}
		}

		return null;
	}
}
