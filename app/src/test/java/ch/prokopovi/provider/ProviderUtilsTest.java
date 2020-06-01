package ch.prokopovi.provider;

import org.junit.Test;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.StringReader;

import javax.xml.xpath.XPathConstants;

import static org.junit.Assert.assertEquals;

public class ProviderUtilsTest {

    public static final String FIRST_SELLING = "51";
    public static final String SECOND_SELLING = "56";

    private static String xmlRecords =
            "<currency>" +
                    "<rates type=\"non-cash\">" +
                    "<item currency-id=\"840\" value-selling=\""+FIRST_SELLING+"\" value-buying=\"49\"/>" +
                    "<item currency-id=\"978\" value-selling=\""+SECOND_SELLING+"\" value-buying=\"54,2455\"/>" +
                    "</rates>" +
                    "<rates type=\"office\">" +
                    "<item currency-id=\"840\" value-selling=\"2\" value-buying=\"1\"/>" +
                    "<item currency-id=\"978\" value-selling=\"4\" value-buying=\"3\"/>" +
                    "</rates>" +
                    "</currency>";

    @Test
    public void xpath() throws Exception {
        // given

        String xpath = "//rates[@type='non-cash']//item[@currency-id='840']/@value-selling";

        InputSource src = new InputSource(new StringReader(xmlRecords));
        Node root = (Node) ProviderUtils.newXpath().evaluate("/", src, XPathConstants.NODE);

        // when
        String parsed = ProviderUtils.evaluateXPath(xpath, root);

        // then
        assertEquals(FIRST_SELLING, parsed);
    }

    @Test
    public void xpathArray() throws Exception {
        // given
        String xpath = "//rates[@type='non-cash']//item[2]/@value-selling";

        InputSource src = new InputSource(new StringReader(xmlRecords));
        Node root = (Node) ProviderUtils.newXpath().evaluate("/", src, XPathConstants.NODE);

        // when
        String parsed = ProviderUtils.evaluateXPath(xpath, root);

        // then
        assertEquals(SECOND_SELLING, parsed);
    }
}