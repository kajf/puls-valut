package ch.prokopovi.provider;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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
		InputSource src = new InputSource(url.openStream());

		Node root = (Node) newXpath().evaluate("/", src, XPathConstants.NODE);

		return root;

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
