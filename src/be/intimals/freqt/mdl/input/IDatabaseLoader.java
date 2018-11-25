package be.intimals.freqt.mdl.input;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;

public interface IDatabaseLoader<T> {
    Database<T> loadDirectory(String path) throws IOException, XPathExpressionException, SAXException, ParserConfigurationException;

    Database<T> loadFile(String path) throws IOException, XPathExpressionException, SAXException, ParserConfigurationException;

}
