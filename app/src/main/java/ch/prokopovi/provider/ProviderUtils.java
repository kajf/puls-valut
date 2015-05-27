package ch.prokopovi.provider;

import android.util.Log;

import org.htmlcleaner.*;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import javax.xml.xpath.*;

import ch.prokopovi.err.WebUpdatingException;

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

	@Deprecated
	public static TagNode load(String location) throws WebUpdatingException {

		try {
			Log.d(LOG_TAG, "<<< GET " + location);
			URL url = new URL(location);

			HtmlCleaner cleaner = initHtmlCleaner();
			TagNode node = cleaner.clean(url);

			return node;
		} catch (Exception e) {
			Log.e(LOG_TAG, "load error", e);
			throw new WebUpdatingException(e);
		}
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

	@Deprecated
	public static HtmlCleaner initHtmlCleaner() {
		long start = new Date().getTime();

		// this is where the HtmlCleaner comes in, I initialize it here
		HtmlCleaner cleaner = new HtmlCleaner();
		CleanerProperties props = cleaner.getProperties();
		props.setAllowHtmlInsideAttributes(true);
		props.setOmitComments(true);

		Log.d(LOG_TAG,
				"html cleaner initialized. spent: "
						+ (new Date().getTime() - start));

		return cleaner;
	}
}
