package ch.prokopovi.auxiliary;

import ch.prokopovi.exported.PureConst.Bank;
import ch.prokopovi.exported.PureConst.MyfinRegion;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.util.*;

public class MyFinCollectorBot extends AbstractCollectorBot {

    private static final String OUT_FILE = "output.sql";
    private static String URL_POISK = "http://myfin.by/banki/poisk";
    private static String URL_SERVICE_FMT = "http://myfin.by/scripts/xml_new/work/banks_city_%d.xml";

    public static void main(String[] args) throws Throwable {

        System.out.println("--- start ---");

        List<Place> res = loadAndParse();

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
            System.out.println("atm fileter is not passed: " + p);
            return false;
        } else if (name.contains(kiosk)) {
            System.out.println("kiosk fileter is not passed: " + p);
            return false;
        }

        return true;
    }

    private static List<Place> clenaupCopies(List<Place> res) {

        System.out.println("### removing copies ...");

        List<Place> newList = new ArrayList<>();

        Iterator<Place> it = res.iterator();
        while (it.hasNext()) {
            Place p = it.next();

            Place p2 = find(p, newList);
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

    private static List<Place> loadAndParse() throws Exception {
        System.out.println("### loading all cities ...");

        List<Place> res = new ArrayList<>();

        for (MyfinRegion city : MyfinRegion.values()) {

            System.out.printf("%s, \n", city.name());

            // poisk page
            String[][] poiskParams = buildPoiskParams(city.getId());
            String postPoisk = post(URL_POISK, poiskParams);

            String strJsonPoisk = extract(postPoisk, "myPoints = ", "myPoints.forEach(function (point)");

            JSONArray placesPoisk = new JSONArray(strJsonPoisk);
            for (int i = 0; i < placesPoisk.length(); i++) {
                JSONObject placePoisk = placesPoisk.getJSONObject(i);

                //				{
                //					coords: [53.926849,27.589382],
                //					adr: 'г. Минск, ул. Сурганова, 43',
                //					show_icon: '0',
                //					main: '<p><a href="/bank/alfabank"><img src="/images/bank_logos/alfabank.png" width="100px"/></a></p>
                // 								 <p>Время работы:</p><p>юр. лицам: пн-чт: 09:00 - 17:00, пт: 09:00 - 16:00 сб, вс: выходной</p>
                //                 <a href="/bank/alfabank/department/15">Подробнее</a>',
                //					name: 'Головной офис ЗАО «Альфа-Банк»',
                //					type: 'Главное отделение',
                //					phone: '198 (круглосуточно)<br>+375 (44,29,25) 733 33 32',
                // 					icon: ''
                //				}

                Place parsedPlace = parse(placePoisk);
                if (!isFilterPassed(parsedPlace))
                    continue;

                // find existing
                Place place = find(parsedPlace, res);
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

        return res;
    }

    private static String[][] buildPoiskParams(int cityId) {

        String[][] params = new String[][]{
                {"Mapobject[city_id]", String.valueOf(cityId)},
                {"Mapobject[filial_type_id][]", "1"},
                {"Mapobject[filial_type_id][]", "3"},
                //{"Mapobject[filial_type_id][]", "4"},
                {"Mapobject[filial_type_id][]", "5"},};

        return params;
    }

    private static Place parse(JSONObject place) throws JSONException {
        JSONArray coords = place.getJSONArray("coords");

        Double cx = coords.getDouble(0);
        Double cy = coords.getDouble(1);
        String name = place.getString("name");
        String adr = place.getString("adr");

        if (cx == null || cy == null || name == null || adr == null)
            return null;

        if (isZero(cx) && isZero(cy))
            return null;

        String main = place.getString("main");

        // department/15">Подробнее
        String strUid = extract(main, "department/", "\">Подробнее");
        int uid = Integer.valueOf(strUid);

        String wh = extractWh(main);

        name = name.trim();
        adr = adr.trim();

        Bank bank = Bank.getByPart(name);

        Place p = new Place(uid, bank, name, cx, cy, adr, wh);

        return p;
    }

    private static String extract(String src, String from, String to) {

        int fromIndex = src.indexOf(from) + from.length();
        int toIndex = src.indexOf(to);

        return src.substring(fromIndex, toIndex).trim();
    }

    private static String extractWh(String src) {
        return src.split("<p>")[3].split("</p>")[0].replace("<br>", "").replace("<br />", "");
    }

    private static void updateFromService(Iterable<Place> places, MyfinRegion city) throws Exception {

        // using map to search by id
        HashMap<Integer, Place> map = new HashMap<>();
        for (Place place : places) {
            map.put(place.getId(), place);
        }
        //

        String location = String.format(URL_SERVICE_FMT, city.getId());
        URL url = new URL(location);

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource(url.openStream());
        Document doc = builder.parse(is);
        NodeList banks = doc.getElementsByTagName("bank");

        for (int i = 0; i < banks.getLength(); i++) {

            Node node = banks.item(i);

            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            Element bank = (Element) node;

            //      <bank>
            //      <bankid>5</bankid>
            //      <filialid>1142</filialid>
            //      <date>19.05.2015</date>
            //      <bankname>ЗАО "Абсолютбанк"</bankname>
            //      <bankaddress>
            //       г.Минск, пр-т. Независимости, 23 (у-м "Центральный")
            //      </bankaddress>
            //      <bankphone>+375 17 211-83-56 (Кассовый центр)</bankphone>
            //      <filialname>Касса №5 ЗАО "Абсолютбанк"</filialname>
            //      <usd_buy>14000</usd_buy>
            //      <usd_sell>14200</usd_sell>
            //      <eur_buy>15660</eur_buy>
            //      <eur_sell>16000</eur_sell>
            //      <rur_buy>281</rur_buy>
            //      <rur_sell>288</rur_sell>
            //      <pln_buy>3800</pln_buy>
            //      <pln_sell>4000</pln_sell>
            //      <ltl_buy>-</ltl_buy>
            //      <ltl_sell>-</ltl_sell>
            //      <uah_buy>350</uah_buy>
            //      <uah_sell>650</uah_sell>
            //      <eurusd_buy>1.104</eurusd_buy>
            //      <eurusd_sell>1.146</eurusd_sell>
            //      </bank>

            Integer filialId = Integer.valueOf(bank.getElementsByTagName("filialid").item(0).getTextContent());

            Place place = map.get(filialId);
            if (place == null) {
                System.out.printf("!!! place: %d is not found list but exists in service xml.\n", filialId);
                continue;
            }

            if (place.getBank() == null) {
                String bankName = bank.getElementsByTagName("bankname").item(0).getTextContent();
                Bank b = Bank.getByPart(bankName);
                place.setBank(b);
            }

            if (place.getBank() == null) {
                System.out.printf("- bank for [%s] is not found \n", place.getId());
            }

            place.setRegionId(city.getMasterId());

            String phone = bank.getElementsByTagName("bankphone").item(0).getTextContent();

            if (!phone.isEmpty() && !"-".equals(phone)) {
                place.updatePhoneWith(phone);
            }
        }
    }

    private static Place find(Place p, Iterable<Place> list) {

        double trh = 0.0001;

        for (Place place : list) {

            Double xDiff = place.getX() - p.getX();
            Double yDiff = place.getY() - p.getY();

            boolean xEq = (-trh <= xDiff && xDiff <= trh);
            boolean yEq = (-trh <= yDiff && yDiff <= trh);

            boolean sameName = place.getName().equals(p.getName());
            boolean sameCoords = xEq && yEq;
            boolean sameAddr = place.getAddr().equals(p.getAddr());
            if (sameName && (sameCoords || sameAddr)) {
                return place;
            }
        }

        return null;
    }
}
