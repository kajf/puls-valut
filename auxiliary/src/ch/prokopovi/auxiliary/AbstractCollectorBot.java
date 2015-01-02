package ch.prokopovi.auxiliary;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.Comparator;

import ch.prokopovi.exported.RatesPlacesTable;
import ch.prokopovi.exported.RatesPlacesTable.ColumnRatesPlaces;

public class AbstractCollectorBot {

	private static DecimalFormat COORD_FMT = new DecimalFormat("#.######"); 
	
	protected static class PlaceIdComparator implements Comparator<Place> {
		@Override
		public int compare(Place o1, Place o2) {
			return o1.getId() - o2.getId();
		}			
	}
	
	protected static boolean isZero(Double d) {
		double th = 0.000001;
		return -th < d && d < th;
	}

	protected static String getInsert(Place place) {
		StringBuilder script = new StringBuilder();
		
		script //
		.append("insert into ")
				.append(RatesPlacesTable.TABLE)
				.append(" (")
				.append(ColumnRatesPlaces.RATES_PLACE_ID)
				.append(", ")
				.append(ColumnRatesPlaces.REGION_ID)
				.append(", ")
				.append(ColumnRatesPlaces.BANK_ID)
				.append(", ")
				.append(ColumnRatesPlaces.X)
				.append(", ")
				.append(ColumnRatesPlaces.Y)
				.append(", ")
				.append(ColumnRatesPlaces.DESCRIPTION)
				.append(", ")
				.append(ColumnRatesPlaces.ADDR)
				.append(", ")
				.append(ColumnRatesPlaces.WORK_HOURS)
				.append(", ")
				.append(ColumnRatesPlaces.PHONE)
				.append(") values (")
				.append(place.getId())
				.append(", ")
				.append(place.getRegionId())
				.append(", ")
				.append(place.getBank() != null ? place.getBank().getId()
						: null)
				.append(", ")
				// local
				// masterId
				// id (not
				// providers)
				.append(COORD_FMT.format(place.getX())).append(", ").append(COORD_FMT.format(place.getY()))
				.append(", ").append("'").append(place.getName()).append("', ")
				.append("'")
				.append(place.getAddr()).append("', ").append("'")
				.append(place.getWh()).append("', ")
				.append("'").append(place.getPhone()).append("'")
				.append(");\n");

		return script.toString();
	}

	protected static String get(String url)
			throws Exception {
		StringBuilder response = new StringBuilder();

		URL siteUrl = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) siteUrl.openConnection();
		

		BufferedReader in = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));
		String line = "";
		while ((line = in.readLine()) != null) {
			response.append(line);
			// System.out.println(line);
		}
		in.close();

		return response.toString();
	}
	
	protected static String post(String url, String[][] data)
			throws Exception {
		StringBuilder response = new StringBuilder();

		URL siteUrl = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) siteUrl.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setDoInput(true);

		DataOutputStream out = new DataOutputStream(conn.getOutputStream());

		String content = "";
		for (int i = 0; i < data.length; i++) {
			String[] param = data[i];
			String key = param[0];
			String val = param[1];
			
			if (i != 0) {
				content += "&";
			}
			
			content += key + "=" + URLEncoder.encode(val, "UTF-8");
		}

		// System.out.println(content);

		out.writeBytes(content);
		out.flush();
		out.close();

		BufferedReader in = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));
		String line = "";
		while ((line = in.readLine()) != null) {
			response.append(line);
			// System.out.println(line);
		}
		in.close();

		return response.toString();
	}
	
	protected static void toFile(String pathname, String script) throws IOException {
		
		File file = new File(pathname);
		 
		// if file doesnt exists, then create it
		if (!file.exists()) {			
			file.createNewFile();
		}

		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(script);
		bw.close();
		
	}
}