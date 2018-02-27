package ch.prokopovi.provider;

import android.content.Context;

import org.w3c.dom.*;
import org.xml.sax.*;

import java.io.*;
import java.net.*;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
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

	public static Node readFrom(Context ctx, String location) throws Exception {
		SSLContext context = getSslContext(ctx);

		URL url = new URL(location);
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.setSSLSocketFactory(context.getSocketFactory());

		conn.setRequestProperty("Authorization", "Basic cHVsczp2YWx1dA==");
		// FIXME: should only be applied for myfin.by but not for any call
		try {
			InputSource src = new InputSource(conn.getInputStream());

			return (Node) newXpath().evaluate("/", src, XPathConstants.NODE);
		} finally {
			conn.disconnect();
		}
	}
	
	private static SSLContext getSslContext(Context ctx) throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
		// Load CAs from an InputStream
		// (could be from a resource or ByteArrayInputStream or ...)
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		InputStream caInput = new BufferedInputStream(ctx.getAssets().open("myfinby.crt"));
		Certificate ca;
		try {
			ca = cf.generateCertificate(caInput);
			System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
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
