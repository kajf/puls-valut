package ch.prokopovi.auxiliary;

import org.w3c.dom.*;
import org.xml.sax.*;

import java.io.*;
import java.net.*;

import javax.xml.parsers.*;

public class PlaceCountChecker {
    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {

        for (int i = 0; i < 200; i++) {

            String location = String.format("http://myfin.by/scripts/xml_new/work/banks_city_%d.xml", i);
            try {

                URL url = new URL(location);

                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                InputSource is = new InputSource(url.openStream());
                Document doc = builder.parse(is);
                NodeList banks = doc.getElementsByTagName("bank");
                if (banks.getLength() > 10)
                    System.out.println("id " + i + " - " + banks.getLength());
            } catch (ParserConfigurationException | IOException | SAXException e) {
                //System.out.println("exception on location "+location);
                //e.printStackTrace();
            }
        }

    }
}
