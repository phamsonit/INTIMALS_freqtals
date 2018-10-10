package be.intimals.freqt.input;

import java.util.*;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import be.intimals.freqt.core.NodeFreqT;
import org.w3c.dom.*;

/**
 * create tree data from ASTs
 */
public class ReadXML {
    private int top;
    private int id;
    private Vector<Integer> sr;
    private Vector<Integer> sibling;
    private int totalFile = 0;
    private Map<String, Vector<String>> grammar;

    public static char uniChar = '\u00a5';// Japanese Yen symbol

    //return total number of reading files
    public int getTotalFile() {
        return this.totalFile;
    }

    //count number children of a node
    public int countChildren(Node node) {
        int nbChildren = 0;

        NodeList list = node.getChildNodes();

        for (int j = 0; j < list.getLength(); ++j) {
            if (list.item(j).getNodeType() != Node.TEXT_NODE && list.item(j).getNodeType() == Node.ELEMENT_NODE) {
                ++nbChildren;
            }
        }
        return nbChildren;

    }

    //count total number of nodes of a tree
    public int countNodes(Node node) {

        int nbChildren = 0;

        NodeList list = node.getChildNodes();
        /*

        for(int j = 0; j<list.getLength(); ++j) {
            if ( list.item(j).getNodeType() != Node.TEXT_NODE )
            {
                ++nbChildren;
            }
        }
        */

        nbChildren = countChildren(node);

        int c = nbChildren; //node.getChildNodes().getLength();
        int result = c;

        if (list.getLength() > 1) {
            for (int i = 0; i < list.getLength(); i++) {
                if (list.item(i).getNodeType() != Node.TEXT_NODE) {
                    result += countNodes(node.getChildNodes().item(i));
                }
            }
        } else {
            result++;
        }

        return result;
    }

    //read tree by breadth first traversal
    private void readTreeDepthFirst(Node node, Vector<NodeFreqT> trans) {
        try {
            // make sure it's element node.
            if (node.getNodeType() == Node.ELEMENT_NODE) {

                // add node name
                trans.elementAt(id).setNodeLabel(node.getNodeName()); //OK
                //System.out.println("node "+trans.elementAt(id).getNodeLabel());


                if (node.getNodeName().equals("EmptyStatement"))
                    System.out.println(node.getNodeName());

                //add node degree
                int nbChildren = countChildren(node);


                //get attribute if need
                //add ordered or unordered
                /*
                if (node.hasAttributes()) {
                    // get attributes names and values
                    NamedNodeMap nodeMap = node.getAttributes();
                    //System.out.println(nodeMap.getNamedItem("ordered").getNodeValue());
                    trans.elementAt(id).setNodeOrdered(Boolean.valueOf(nodeMap.getNamedItem("ordered").getNodeValue()));
                }
                */

                //keep positions to calculate relationships: parent - child - sibling
                sr.addElement(id);
                ++id;

                if (node.hasChildNodes()) {
                    //get list of children
                    NodeList nodeList = node.getChildNodes();

                    //if node is a parent of a leaf node
                    if (node.getChildNodes().getLength() == 1) {
                        //add leaf node label
                        trans.elementAt(id).setNodeLabel("*" + node.getTextContent().trim());
                        //trans.elementAt(id).setNodeLabel("**");
                        //System.out.println("node "+trans.elementAt(id).getNodeLabel());

                        sr.addElement(id);
                        ++id;

                        //////close a node and calculate parent, child, sibling

                        top = sr.size() - 1;

                        int child = sr.elementAt(top);

                        int parent = sr.elementAt(top - 1);

                        trans.elementAt(child).setNodeParent(parent);

                        if (trans.elementAt(parent).getNodeChild() == -1)
                            trans.elementAt(parent).setNodeChild(child);

                        if (sibling.elementAt(parent) != -1)
                            trans.elementAt(sibling.elementAt(parent)).setNodeSibling(child);

                        sibling.setElementAt(child, parent);

                        sr.setSize(top);

                        ///////////////

                    } else {
                        /*
                        //check a node has ordered or unordered children
                        Map<String, Integer> newChildren = new LinkedHashMap<>();
                        boolean repeated = false;
                        for(int i=0; i< nodeList.getLength(); ++i)
                        {
                            if(newChildren.containsKey(nodeList.item(i).getNodeName()))
                                repeated = true;
                            else
                                newChildren.put(nodeList.item(i).getNodeName(),i);

                        }
                        */
                        String nodeOrder = "";
                        String nodeDegree = "";
                        if (grammar.containsKey(node.getNodeName())) {
                            //System.out.println(node.getNodeName());
                            //System.out.println(grammar.get(node.getNodeName()).elementAt(0));
                            //System.out.println(grammar.get(node.getNodeName()).elementAt(1));
                            nodeOrder = grammar.get(node.getNodeName()).elementAt(0);
                            nodeDegree = grammar.get(node.getNodeName()).elementAt(1);
                        }


                        //sort children if they are unordered and degree = "1..*"
                        if (nodeOrder.equals("unordered") && nodeDegree.equals("1..*")) {
                            //sort children by labels if node is a nodelist ==> OK
                            ArrayList<Node> sortedNodeList = new ArrayList<>();
                            for (int i = 0; i < nodeList.getLength(); ++i) {
                                if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                                    sortedNodeList.add(nodeList.item(i));
                                }
                            }

                            Collections.sort(sortedNodeList, new Comparator<Node>() {
                                @Override
                                public int compare(Node o1, Node o2) {
                                    return o1.getNodeName().compareToIgnoreCase(o2.getNodeName());
                                }
                            });


                            // loop for each sorted child
                            for (int i = 0; i < sortedNodeList.size(); ++i) {
                                readTreeDepthFirst(sortedNodeList.get(i), trans);
                            }
                        } else {
                            //loop for each child without sorted
                            for (int i = 0; i < nodeList.getLength(); ++i) {
                                if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                                    readTreeDepthFirst(nodeList.item(i), trans);
                                }
                            }
                        }


                    }
                }

                //close a node and calculate parent, child, sibling
                //System.out.println(" )");

                top = sr.size() - 1;

                if (top < 1) return;

                int child = sr.elementAt(top);

                int parent = sr.elementAt(top - 1);

                trans.elementAt(child).setNodeParent(parent);

                if (trans.elementAt(parent).getNodeChild() == -1)
                    trans.elementAt(parent).setNodeChild(child);

                if (sibling.elementAt(parent) != -1)
                    trans.elementAt(sibling.elementAt(parent)).setNodeSibling(child);

                sibling.setElementAt(child, parent);

                sr.setSize(top);

            }
        } catch (Exception e) {
            System.out.println(e);
        }


    }

    //create transaction from 1 folder
    public void createTransaction(String path, Vector<Vector<NodeFreqT>> transaction) {
        try {

            System.out.print("create tree data: ");

            //read folder
            File folder = new File(path);
            File[] listOfFiles = folder.listFiles();
            Arrays.sort(listOfFiles);

            for (int l = 0; l < listOfFiles.length; ++l)
                if (listOfFiles[l].isFile() && listOfFiles[l].getName().charAt(0) != '.') {

                    //for each file in folder create one tree
                    String fileName = path + "/" + listOfFiles[l].getName();
                    File fXmlFile = new File(fileName);
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(fXmlFile);
                    doc.getDocumentElement().normalize();

                    //System.out.println("file "+listOfFiles[l].getName());


                    //get total number of nodes
                    int size = countNodes(doc.getDocumentElement()) + 1;

                    //init trans and sibling
                    id = 0;
                    top = 0;
                    sr = new Vector<>();
                    sibling = new Vector<>();

                    Vector<NodeFreqT> trans = new Vector<>();

                    trans.setSize(size);
                    sibling.setSize(size);

                    for (int i = 0; i < size; ++i) {

                        NodeFreqT nodeTemp = new NodeFreqT();

                        nodeTemp.setNodeParent(-1);
                        nodeTemp.setNodeChild(-1);
                        nodeTemp.setNodeSibling(-1);
                        nodeTemp.setNodeDegree("0");
                        nodeTemp.setNodeOrdered(true);

                        trans.setElementAt(nodeTemp, i);
                        sibling.setElementAt(-1, i);
                    }

                    //create tree
                    readTreeDepthFirst(doc.getDocumentElement(), trans);

                    //add tree to transaction
                    transaction.addElement(trans);
                }
            System.out.println("finished");

        } catch (Exception e) {
            System.out.println("input error");
        }

    }

    //create transaction from many folder
    public void createTransaction(File f, Vector<Vector<NodeFreqT>> transaction, Map<String, Vector<String>> _grammar) {
        try {

            //System.out.print("create tree data: ");

            grammar = _grammar;

            File[] subdir = f.listFiles();
            Arrays.sort(subdir);

            for (File fi : subdir) {

                if (fi.isFile() && fi.getName().charAt(0) != '.') {
                    ++totalFile;
                    //System.out.print("reading file ----------------");
                    //System.out.println(f+"/"+fi.getName());
                    //for each file in folder create one tree
                    //String fileName = path + "/" + listOfFiles[l].getName();
                    File fXmlFile = new File(f + "/" + fi.getName());
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(fXmlFile);
                    doc.getDocumentElement().normalize();

                    //get total number of nodes
                    int size = countNodes(doc.getDocumentElement()) + 1;

                    //init trans and sibling
                    id = 0;
                    top = 0;
                    sr = new Vector<>();
                    sibling = new Vector<>();

                    Vector<NodeFreqT> trans = new Vector<>();

                    trans.setSize(size);
                    sibling.setSize(size);

                    for (int i = 0; i < size; ++i) {

                        NodeFreqT nodeTemp = new NodeFreqT();

                        nodeTemp.setNodeParent(-1);
                        nodeTemp.setNodeChild(-1);
                        nodeTemp.setNodeSibling(-1);
                        nodeTemp.setNodeDegree("0");
                        nodeTemp.setNodeOrdered(true);

                        trans.setElementAt(nodeTemp, i);
                        sibling.setElementAt(-1, i);
                    }
                    //create tree
                    readTreeDepthFirst(doc.getDocumentElement(), trans);

                    //add tree to transaction
                    transaction.addElement(trans);
                } else if (fi.isDirectory()) {
                    createTransaction(fi, transaction, grammar);
                }
            }

        } catch (Exception e) {
            System.out.println("input error");
        }

    }


}
