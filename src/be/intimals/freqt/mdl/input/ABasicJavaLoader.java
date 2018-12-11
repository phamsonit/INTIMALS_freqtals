package be.intimals.freqt.mdl.input;

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

    @Override
    public Database<T> loadDirectory(String path) throws IOException, XPathExpressionException, SAXException,
        ParserConfigurationException {
        init();
        List<File> files = XMLUtil.loadXMLDirectory(path, (String file) -> true);
        LOGGER.info("loadDirectory : " + path);
        for (File f : files) {
            loadFile(f);
        }
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

            //IDatabaseNode<T> newTreeRoot = traverse(root, null);
            IDatabaseNode<T> newTreeRoot = DatabaseNode.create(currentTID, getKeyForNode(root), null);
            traverseBFS(root, newTreeRoot, null);
            db.addTransaction(newTreeRoot);

            ++currentTID;
            newTreeRoot.resetID();
        }
    }

    /*
    private IDatabaseNode<T> traverse(Node currentNode, IDatabaseNode<T> parent) {
        IDatabaseNode<T> newTreeNode = null;

        if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
            newTreeNode  = DatabaseNode.create(currentTID, getKeyForNode(currentNode), parent);
            if (currentNode.hasChildNodes()) {
                List<IDatabaseNode<T>> treeChildren = new ArrayList<>();
                NodeList nodeList = currentNode.getChildNodes();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node child = nodeList.item(i);
                    IDatabaseNode<T> newChildNode = traverse(child, newTreeNode);
                    treeChildren.add(newChildNode);
                }
                newTreeNode.setChildren(treeChildren);
            }
        } else if (currentNode.getNodeType() == Node.TEXT_NODE) {
            // Base case
            newTreeNode  = DatabaseNode.create(currentTID, getKeyForNode(currentNode), parent);
        } else {
            LOGGER.severe("Unhandled XML nodes");
        }
        return newTreeNode;
    }
    */

    private void traverseBFS(Node currentNode, IDatabaseNode<T> current, IDatabaseNode<T> parent) {
        if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
            if (currentNode.hasChildNodes()) {
                List<IDatabaseNode<T>> treeChildren = new ArrayList<>();
                NodeList nodeList = currentNode.getChildNodes();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node child = nodeList.item(i);
                    IDatabaseNode<T> newChildNode = DatabaseNode.create(currentTID, getKeyForNode(child), current);
                    treeChildren.add(newChildNode);
                }
                current.setChildren(treeChildren);

                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node child = nodeList.item(i);
                    traverseBFS(child, treeChildren.get(i), current);
                }
            }
        } else if (currentNode.getNodeType() == Node.TEXT_NODE) {
            // Don't do anything, already created
        } else {
            LOGGER.severe("Unhandled XML nodes");
        }
    }

    protected abstract T getKeyForNode(Node current);

    protected void init() {
        currentTID = 0;
        db = Database.create();
        IDatabaseNode<T> temp = DatabaseNode.create();
        temp.resetID();
        temp.resetUID();
    }

}
