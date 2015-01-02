package ch.prokopovi.auxiliary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ch.prokopovi.exported.PureConst;
import ch.prokopovi.exported.PureConst.Bank;
import ch.prokopovi.exported.PureConst.FinanceUaRegion;

public class FinanceUaCollectorBot extends AbstractCollectorBot {

	//private static final String UID_PREFIX = "7oiylpmiow8iy1sm";
	
	private static String URL_PLACES = "http://resources.finance.ua/ru/public/currency-cash.xml";
	
	private static String URL_LOCATION = "http://tables.finance.ua/ru/currency/orggmaplocation";

	public static void main(String[] args) throws Throwable {
				
		Document doc = prepare(URL_PLACES);
		parse(doc);				
	}
	
	public static Document prepare(String uri) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    	Document doc = dBuilder.parse(uri);
    		      
    	//optional, but recommended
    	//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
    	doc.getDocumentElement().normalize();
    	
    	return doc;
	}
	
	private static String[] collectPlaceIds(NodeList nList) {
		
		int length = nList.getLength();
		String[] ids = new String[length];
		
		for (int i = 0; i < length; i++) {
			Element eElement = (Element) nList.item(i);
			
			String uid = eElement.getAttribute("id");
			ids[i] = uid;
		}
	
		return ids;
	}
	
	private static String[][] buildLocationParams(String[] ids) {
		String[][] params = new String [ids.length][2];
		
		for (int i = 0; i < ids.length; i++) {
			String id = ids[i];
			
			params[i][0] = "org[]";
			params[i][1] = id;
		}
				
		return params;
	}	
	
	private static Map<String, Double[]> buildUidCoordMap(NodeList nList) throws Exception{
		
		HashMap<String, Double[]> coordsMap = new HashMap<String, Double[]>();
		
    	String[] placeIds = collectPlaceIds(nList);
		String[][] params = buildLocationParams(placeIds);
		String post = post(URL_LOCATION, params);
		
		JSONObject jsonMap = new JSONObject(post);
		Iterator<String> keys = jsonMap.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			
			JSONObject data = (JSONObject)jsonMap.get(key);
			
			coordsMap.put(key, new Double[]{data.getDouble("x"), data.getDouble("y")});			
		}
		
		return coordsMap;
	} 
	
	public static void parse(Document doc) throws Exception {
	     
	    	System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
	     
	    	NodeList nList = doc.getElementsByTagName("organization");
	    	int length = nList.getLength();
	    	
	    	// location page
	    	Map<String, Double[]> coordMap = buildUidCoordMap(nList);
			// 
	    	
	    	System.out.println("----------------------------");
	     	    	
	    	List<Place> res = new ArrayList<Place>();
	    	
			for (int i = 0; i < length; i++) {
	     
	    		Node nNode = nList.item(i);
	     
	    		//System.out.println("\nCurrent Element :" + nNode.getNodeName());
	    		//System.out.println();
	    		//System.out.println("parsing "+(i+1)+" of "+ length);
	     
	    		if (nNode.getNodeType() == Node.ELEMENT_NODE) {
	     
	    			Element eElement = (Element) nNode;
	     
	    			Place p = parse(eElement, coordMap);	    			
	    			if (p == null)
	    				continue;
	    			
	    			//System.out.println(p);
	    			
	    			res.add(p);	     
	    		}
	    	}
			
			// sort
			Collections.sort(res, new Comparator<Place>() {
				@Override
				public int compare(Place o1, Place o2) {
					return o1.getId() - o2.getId();
				}			
			});
			
			System.out.println();
			StringBuilder script = new StringBuilder();
			for (Place place : res) {
				script.append(getInsert(place));
			}
						
			toFile("../output_ua.sql", script.toString());
					
			System.out.printf("--- end. added %d of %d ---", res.size(), length);			
	}
	
	private static Place parse(Element eElement, Map<String, Double[]> coordMap) throws Exception {
		
//		<organization id="7oiylpmiow8iy1smbzh" oldid="1619" org_type="1">
//			<title value="Юнекс Банк ЧО №1"/>
//			<region id="ua,7oiylpmiow8iy1smacb"/>
//			<city id="7oiylpmiow8iy1smaea"/>
//			<phone value="0672180503"/>
//			<address value="б-р Шевченка, 145"/>
//			<link type="reference-info" href="http://organizations.finance.ua/ru/info/currency/~/7oiylpmiow8iy1smbzh/cash"/>
// 			...
//		</organization>
		
		String uid = eElement.getAttribute("id");
		int id = PureConst.financeUaPlaceIdTransform(uid);	
		//System.out.println("uid: " + uid + " id: "+ id);
		
		String orgTypeId = eElement.getAttribute("org_type");
		String type = getPlaceType(orgTypeId);
		//System.out.println("org_type: " + orgTypeId+ " type: " + type);		
		
		String name = ((Element) eElement.getElementsByTagName("title").item(0))
				.getAttribute("value");
		//System.out.println("title : " + bank);
		
		String cityUid = ((Element)eElement.getElementsByTagName("city").item(0)).getAttribute("id");
		FinanceUaRegion region = FinanceUaRegion.get(cityUid);	 
		
		if (region == null) {
			System.out.println("!!! NO MATCH for city " + cityUid);
			return null;
		}		
		//System.out.println("region uid : " + regionUid +" region id : " + regionId);
		
		String phone = ((Element)eElement.getElementsByTagName("phone").item(0)).getAttribute("value");
		//System.out.println("phone : " + phone);
		
		String addr = ((Element)eElement.getElementsByTagName("address").item(0)).getAttribute("value");
		//System.out.println("address : " + addr);
		
		Double[] coords = coordMap.get(uid);
		
		if (coords == null) {
			System.out.println("!!! NO MATCH for org " + uid);
			return null;
		}
		
		String wh = ""; // TODO		
				
		Bank bank = Bank.getByPart(name);
		if (bank == null) {
			System.out.printf("- bank for [%s] is not found \n", name);
		}

		// bank
		Place p = new Place(id, region.getRegionId(), bank, name, coords[0],
				coords[1], addr, wh, phone);
		
		return p;
	}
	
	private static String getPlaceType(String id){
		
		String type = null;
		
		if ("1".equals(id)) {
			type = "Банк";
		} else if ("2".equals(id)) {
			type = "ПОВ";			
		}
		
		return type;
	}

}
