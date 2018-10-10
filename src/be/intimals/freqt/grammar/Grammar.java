package be.intimals.freqt.grammar;

import be.intimals.freqt.util.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Map;
import java.util.Vector;

public class Grammar {
    public enum Order {
        ORDERED, UNORDERED
    }

    public enum Children {
        ONE, N, ONE_OR_MORE
    }

    public class Rule {
        private Order order;
        private Children nbChildren;
    }

    private Map<String, Rule> grammar;

}
