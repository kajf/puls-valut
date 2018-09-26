package ch.prokopovi.auxiliary;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import ch.prokopovi.exported.PureConst;
import ch.prokopovi.exported.PureConst.Bank;
import ch.prokopovi.exported.PureConst.MyfinRegion;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MyFinCollectorBot extends AbstractCollectorBot {

  private static final String OUT_FILE = "output.sql";
  private static final String URL_POISK = "https://myfin.by/banki/otdelenija";
  private static final String URL_SERVICE_FMT = "https://admin.myfin.by/outer/authXml/%d";

  public static void main(String[] args) throws Throwable {

    long startTime = System.currentTimeMillis();
    System.out.println("--- start ---");

    List<Place> res = loadAndParse();

    List<Place> uniqList = cleanupCopies(res);

    // sort
    Collections.sort(uniqList, new PlaceIdComparator());

    System.out.println();

    StringBuilder script = new StringBuilder();
    for (int i = 0; i < uniqList.size(); i++) {

      Place place = uniqList.get(i);

      if (place.getRegionId() == null || "".equals(place.getPhone())) {
        System.out.println("!!! NO MATCH " + place);
      }

      if (i % 500 == 0) {
        script.append(";").append(getInsert()).append("\n").append("          ");
      } else {
        script.append("union all ");
      }

      script.append(getSelect(place));
    }
    script.append(";");

    /*System.out.println();
    System.out.println("--- script begin ---");
    System.out.println(script.toString());
    System.out.println("--- script end ---");*/

    toFile(OUT_FILE, script.toString());

    long timeSpent = System.currentTimeMillis() - startTime;
    System.out.println("--- end (" + timeSpent + ") ---");
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

  private static List<Place> cleanupCopies(List<Place> res) {

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

    int svcCnt = 0;
    for (MyfinRegion city : MyfinRegion.values()) {

      System.out.printf("%s, \n", city.name());

      // points page
      String foundPoints = get(getPoiskUrl(city));

      String strJsonPoisk = extract(
        foundPoints,
        "myPoints = $.parseJSON('",
        "myPoints.forEach(function (point)");

      JSONArray placesPoisk = new JSONArray(strJsonPoisk);
      System.out.println("gross points count: " + placesPoisk.length());

      for (int i = 0; i < placesPoisk.length(); i++) {
        JSONObject placePoisk = placesPoisk.getJSONObject(i);

        //{
//                        "coords":"[53.926849,27.589382]",
//                        "id":"15",
//                        "icon_b":"alfabank",
//                        "finalTime":[["09:00","17:00"],["09:00","17:00"],["09:00","17:00"],["09:00","17:00"],["09:00","16:00"],["00:00","00:00"],["00:00","00:00"]],

//                        "name":"\u0413\u043e\u043b\u043e\u0432\u043d\u043e\u0439 \u043e\u0444\u0438\u0441 \u0417\u0410\u041e \u00ab\u0410\u043b\u044c\u0444\u0430-\u0411\u0430\u043d\u043a\u00bb",
//
//                        "header":"<div class=\\\"map-descr\\\"><div class=\\\"b-logo\\\"><a href=\\\"\/bank\/alfabank\\\"><center><img src=\\\"https:\/\/admin.myfin.by\/images\/bank_logos\/alfabank.png\\\" height=\\\"35px\\\"\/><\/center><\/a><\/div><div class=\\\"b-name\\\"><a href=\\\"\/bank\/alfabank\/department\/15-minsk-ul-surganova-43\\\">\u0413\u043e\u043b\u043e\u0432\u043d\u043e\u0439 \u043e\u0444\u0438\u0441 \u0417\u0410\u041e \u00ab\u0410\u043b\u044c\u0444\u0430-\u0411\u0430\u043d\u043a\u00bb<\/a><div class=\\\"descr\\\">\u0413\u043b\u0430\u0432\u043d\u043e\u0435 \u043e\u0442\u0434\u0435\u043b\u0435\u043d\u0438\u0435<\/div><\/div>",
//
//                        "footer":"<a href=\\\"\/bank\/alfabank\/department\/15-minsk-ul-surganova-43\\\" class=\\\"link-more\\\">\u041f\u043e\u0434\u0440\u043e\u0431\u043d\u0435\u0435<\/a><\/div>",
//
//                        "main":"<div class=\\\"b-address\\\">\u0433. \u041c\u0438\u043d\u0441\u043a, \u0443\u043b. \u0421\u0443\u0440\u0433\u0430\u043d\u043e\u0432\u0430, 43<\/div><div class=\\\"b-time\\\">\u041f\u043d-\u0427\u0442: 09:00-17:00,<br \/>\u041f\u0442: 09:00-16:00,<br \/>\u0421\u0431-\u0412\u0441: \u0412\u044b\u0445\u043e\u0434\u043d\u043e\u0439<\/div><div class=\\\"b-tel\\\">(017) 217 64 64, 200 68 80, \u0444\u0430\u043a\u0441: (017) 200 17 00<\/div>"},

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

        String strPhone = extract(placePoisk.getString("main"), "b-tel\\\">", "</div>");
        place.updatePhoneWith(strPhone);

      }

      // update phones
      svcCnt += updateFromService(res, city);
    }

    System.out.printf(
      "--- Places in service %d of %d loaded (%d percent) \n",
      svcCnt,
      res.size(),
      svcCnt * 100 / res.size());

    return res;
  }

  private static String getPoiskUrl(PureConst.MyfinRegion city) {
    String url = URL_POISK;

    if (!MyfinRegion.MINSK.equals(city)) {
      url += "/" + city.name().toLowerCase();
    }

    return url;
  }

  private static Place parse(JSONObject place) throws JSONException {
    String coords = place.getString("coords");
    coords = coords.replace("[", "").replace("]", "");
    final String[] split = coords.split(",");

    Double cx = Double.valueOf(split[0]);
    Double cy = Double.valueOf(split[1]);
    String name = place.getString("name");
    String adr = extract(place.getString("main"), "b-address\\\">", "</div><div class=\\\"b-time");

    if (cx == null || cy == null || name == null || adr == null)
      return null;

    if (isZero(cx) && isZero(cy))
      return null;

    int uid = place.getInt("id");
    final String wh = extract(place.getString("main"), "b-time\\\">", "</div><div class=\\\"b-tel");

    name = name.trim();
    adr = adr.trim();

    Bank bank = Bank.getByPart(name);

    if (bank != null && bank.deprecated)
      return null;

    Place p = new Place(uid, bank, name, cx, cy, adr, wh);

    return p;
  }

  private static String extract(String src, String from, String to) {

    int fromIndex = src.indexOf(from) + from.length();
    String part = src.substring(fromIndex);
    int toIndex = part.indexOf(to);

    return part.substring(0, toIndex).trim();
  }

  private static int updateFromService(Iterable<Place> places, MyfinRegion city) throws Exception {

    // using map to search by id
    Map<Integer, Place> map = new HashMap<>();
    for (Place place : places) {
      map.put(place.getId(), place);
    }
    //
    SSLContext context = getSslContext();


    String location = String.format(URL_SERVICE_FMT, city.getId());
    URL url = new URL(location);
    HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
    conn.setSSLSocketFactory(context.getSocketFactory());

    conn.setRequestProperty("Authorization", "Basic cHVsczp2YWx1dA==");

    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

    Document doc = builder.parse(conn.getInputStream());

    NodeList banks = doc.getDocumentElement().getElementsByTagName("bank");

    int cnt = 0;
    for (int i = 0; i < banks.getLength(); i++) {

      Node node = banks.item(i);

      if (node.getNodeType() != Node.ELEMENT_NODE)
        continue;

      Element bank = (Element)node;

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
      cnt++;

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

    return cnt;
  }

  private static SSLContext getSslContext() throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
    // Load CAs from an InputStream
// (could be from a resource or ByteArrayInputStream or ...)
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
// From https://www.washington.edu/itconnect/security/ca/load-der.crt
    InputStream caInput = new BufferedInputStream(new FileInputStream("myfinby.crt"));
    Certificate ca;
    try {
      ca = cf.generateCertificate(caInput);
      //System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
    } finally {
      caInput.close();
    }

// Create a KeyStore containing our trusted CAs
    String keyStoreType = KeyStore.getDefaultType();
    KeyStore keyStore = KeyStore.getInstance(keyStoreType);
    keyStore.load(null, null);
    keyStore.setCertificateEntry("ca", ca);

// Create a TrustManager that trusts the CAs in our KeyStore
    String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
    tmf.init(keyStore);

// Create an SSLContext that uses our TrustManager

    SSLContext context = SSLContext.getInstance("TLS");
    context.init(null, tmf.getTrustManagers(), null);

    return context;
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
