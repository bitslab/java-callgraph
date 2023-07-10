package edu.uic.bitslab.callgraph.coverage;

import gr.gousiosg.javacg.stat.coverage.Report;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.FileReader;
import java.io.IOException;

public class JacocoCoverageParser {
    private static final String XML_LOAD_EXTERNAL_DTD =
            "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    private static final String SAX_VALIDATION = "http://xml.org/sax/features/validation";

    /**
     * Parse a JaCoCO XML file
     *
     * @param filepath the path to the xml file
     * @return A {@link gr.gousiosg.javacg.stat.coverage.Report} (These classes are automatically
     * generated and placed in the folder:
     * target/classes/gr/gousiosg/javacg/stat/support/coverage)
     */
    public static Report getReport(String filepath)
            throws JAXBException, IOException, SAXException, ParserConfigurationException {
        JAXBContext jc = JAXBContext.newInstance(Report.class);

        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setFeature(XML_LOAD_EXTERNAL_DTD, false);
        spf.setFeature(SAX_VALIDATION, false);

        XMLReader xmlReader = spf.newSAXParser().getXMLReader();
        InputSource inputSource = new InputSource(new FileReader(filepath));
        SAXSource source = new SAXSource(xmlReader, inputSource);

        Unmarshaller unmarshaller = jc.createUnmarshaller();
        return (Report) unmarshaller.unmarshal(source);
    }
}
