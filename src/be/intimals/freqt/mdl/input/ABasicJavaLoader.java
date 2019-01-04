package be.intimals.freqt.mdl.input;

import be.intimals.freqt.util.DatabaseStatistics;
import be.intimals.freqt.util.XMLUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Basic abstract class to be able to load Java ASTS into a Database as-is.
 */
public abstract class ABasicJavaLoader<T> implements IDatabaseLoader<T> {
    private static final Logger LOGGER = Logger.getLogger(ABasicJavaLoader.class.getName());

    private Database<T> db = Database.create();
    private int currentTID = 0;
    protected DatabaseStatistics statsMain = DatabaseStatistics.create();
    protected DatabaseStatistics statsTree = DatabaseStatistics.create();

    @Override
    public Database<T> loadDirectory(String path) throws IOException, XPathExpressionException, SAXException,
        ParserConfigurationException {
        init();
        List<File> files = XMLUtil.loadXMLDirectory(path, (String file) -> true);
        LOGGER.info("loadDirectory : " + path);
        for (File f : files) {
            loadFile(f);
        }
        statsMain.write();
        return db;
    }

    @Override
    public Database<T> loadFile(String path) throws IOException, XPathExpressionException, SAXException,
        ParserConfigurationException {
        init();
        File f = XMLUtil.loadXMLFile(path, (String name) -> true);
        loadFile(f);
        return db;
    }

    private void loadFile(File f) throws IOException, XPathExpressionException, SAXException,
            ParserConfigurationException {
        if (f != null) {
            LOGGER.info("loadFile : " + f.getName());
            Node root = XMLUtil.getXMLRoot(f);

            statsTree = DatabaseStatistics.create();
            statsTree.nbTrees++;
            IDatabaseNode<T> newTreeRoot = DatabaseNode.create(currentTID, getKeyForNode(root), null);
            statsTree.labelsNT.add(getKeyForNode(root).toString());
            traverseHybrid(root, newTreeRoot);
            db.addTransaction(newTreeRoot);
            statsTree.addMaxFanoutVertices(statsTree.maxFanout);
            statsTree.addVertices(statsTree.nbNodes);
            statsMain.merge(statsTree);

            ++currentTID;
            newTreeRoot.resetID();
        }
    }

    private void traverseHybrid(Node currentNode, IDatabaseNode<T> current) {
        statsTree.nbNodes++;
        if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
            if (currentNode.hasChildNodes()) {
                statsTree.currentNonTerminals++;
                statsTree.labelsNT.add(getKeyForNode(currentNode).toString());

                List<IDatabaseNode<T>> treeChildren = new ArrayList<>();
                NodeList nodeList = currentNode.getChildNodes();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node child = nodeList.item(i);
                    IDatabaseNode<T> newChildNode = DatabaseNode.create(currentTID, getKeyForNode(child), current);
                    treeChildren.add(newChildNode);
                }
                current.setChildren(treeChildren);
                statsTree.addFanout(current.getChildrenCount());
                statsTree.updateMaxFanout(current.getChildrenCount());

                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node child = nodeList.item(i);
                    traverseHybrid(child, treeChildren.get(i));
                }
            } else {
                statsTree.nbLeaf++;
                statsTree.labelsT.add(getKeyForNode(currentNode).toString());
                //LOGGER.info("LEAF " + currentNode.toString());
            }
        } else if (currentNode.getNodeType() == Node.TEXT_NODE) {
            // Don't do anything, already created
            statsTree.nbLeaf++;
            statsTree.labelsT.add(getKeyForNode(currentNode).toString());
        } else {
            LOGGER.severe("Unhandled XML nodes");
        }
    }

    protected abstract T getKeyForNode(Node current);

    protected void init() {
        currentTID = 0;
        db = Database.create();
        statsMain = DatabaseStatistics.create();
        statsTree = DatabaseStatistics.create();
        IDatabaseNode<T> temp = DatabaseNode.create();
        temp.resetID();
        temp.resetUID();
    }

}
