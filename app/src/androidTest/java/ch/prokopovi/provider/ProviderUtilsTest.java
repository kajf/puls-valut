package ch.prokopovi.provider;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.StringReader;

import javax.xml.xpath.XPathConstants;

public class ProviderUtilsTest extends TestCase {

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

    public void testXpath() throws Exception {
        // given

        String xpath = "//rates[@type='non-cash']//item[@currency-id='840']/@value-selling";

        InputSource src = new InputSource(new StringReader(xmlRecords));
        Node root = (Node) ProviderUtils.newXpath().evaluate("/", src, XPathConstants.NODE);

        // when
        String parsed = ProviderUtils.evaluateXPath(xpath, root);

        // then
        Assert.assertEquals(FIRST_SELLING, parsed);
    }

    public void testXpathArray() throws Exception {
        // given
        String xpath = "//rates[@type='non-cash']//item[2]/@value-selling";

        InputSource src = new InputSource(new StringReader(xmlRecords));
        Node root = (Node) ProviderUtils.newXpath().evaluate("/", src, XPathConstants.NODE);

        // when
        String parsed = ProviderUtils.evaluateXPath(xpath, root);

        // then
        Assert.assertEquals(SECOND_SELLING, parsed);
    }
}