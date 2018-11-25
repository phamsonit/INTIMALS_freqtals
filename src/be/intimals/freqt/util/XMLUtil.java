package be.intimals.freqt.util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

public class XMLUtil {
    public interface DirectoryFilter {
        boolean apply(String path);
    }

    public static Node getXMLRoot(String path) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        return getXMLRoot(new File(path));
    }

    public static Node getXMLRoot(File fXmlFile) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile);
        doc.getDocumentElement().normalize();

        XPath xp = XPathFactory.newInstance().newXPath();
        NodeList nl = (NodeList) xp.evaluate("//text()[normalize-space(.)='']", doc, XPathConstants.NODESET);

        for (int i = 0; i < nl.getLength(); ++i) {
            Node node = nl.item(i);
            node.getParentNode().removeChild(node);
        }

        return doc.getDocumentElement();
    }

    public static List<File> loadXMLDirectory(String path, DirectoryFilter filter) {
        List<File> res = new ArrayList<>();
        File dir = new File(path);
        File[] dirFiles = dir.listFiles();

        for (File fi : dirFiles) {
            if (fi.isFile() && filter.apply(fi.getName())) {
                res.add(fi);
            } else if (fi.isDirectory()) {
                res.addAll(loadXMLDirectory(fi.getPath(), filter));
            }
        }
        return res;
    }

    public static File loadXMLFile(String path, DirectoryFilter filter) {
        File res = new File(path);
        if (res.isFile() && filter.apply(res.getName())) {
            return res;
        }
        return null;
    }

    public static String toString(Node node) {
        // https://gist.github.com/skermes/3138513
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            return "toString error " + node.getNodeName();
        }
    }

    public static Iterator<Node> asIterator(final NodeList nodeList) {
        return new Iterator<Node>() {

            private int index = 0;

            public boolean hasNext() {
                return index < nodeList.getLength();
            }

            public Node next() {
                if (!hasNext()) throw new NoSuchElementException();
                return nodeList.item(index++);
            }
        };
    }


}
