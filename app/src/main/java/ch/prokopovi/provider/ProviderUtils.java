package ch.prokopovi.provider;

import org.w3c.dom.*;
import org.xml.sax.*;

import java.io.*;
import java.net.*;

import javax.xml.xpath.*;

public class ProviderUtils {

	private static final String LOG_TAG = "ProviderUtils";

	/**
	 * get string response form url
	 * 
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public static String get(String url) throws Exception {
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

	public static String readFrom(String location, String xpath) throws Exception {

		URL url = new URL(location);
		InputSource src = new InputSource(url.openStream());

		String s = (String) newXpath().evaluate(xpath, src, XPathConstants.STRING);

		return s;

	}

	public static Node readFrom(String location) throws Exception {
		URL url = new URL(location);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestProperty("Authorization", "Basic cHVsczp2YWx1dA==");
		// FIXME: should only be applied for myfin.by but not for any call
		try {
			InputSource src = new InputSource(conn.getInputStream());

			return (Node) newXpath().evaluate("/", src, XPathConstants.NODE);
		} finally {
			conn.disconnect();
		}
	}

	public static String evaluateXPath(String xpath, Node root) {
		try {
			return newXpath().evaluate(xpath, root);
		} catch (XPathExpressionException e) {
			return null;
		}
	}

	public static XPath newXpath() {
		return XPathFactory.newInstance().newXPath();
	}
}
