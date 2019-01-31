package be.intimals.freqt.input;

import be.intimals.freqt.structure.*;
/*
create tree data from ASTs
 */
import java.lang.reflect.Array;
import java.util.*;
import java.io.*;
//import java.io.File;
import java.nio.file.Files;
import javax.management.Attribute;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.*;


public class ReadXML {

    private int top;
    private int id;
    private Vector<Integer> sr;
    private Vector<Integer> sibling;

    private Map <String, Vector<String> > grammar;
    private int nbFiles = 0;

    public static  char uniChar = '\u00a5';// Japanese Yen symbol

    //return total number of reading files
    public int getnbFiles(){
        return this.nbFiles;
    }

    //count number children of a node
    public int countNBChildren(Node node){
        int nbChildren = 0;
        NodeList list = node.getChildNodes();
        for(int j = 0; j<list.getLength(); ++j) {
            if ( list.item(j).getNodeType() != Node.TEXT_NODE && list.item(j).getNodeType() == Node.ELEMENT_NODE ){
                ++nbChildren;
            }
        }
        return nbChildren;
    }

    //count total number of nodes of a tree
    public int countNBNodes(Node root) {

        NodeList childrenNodes = root.getChildNodes();
        int nbChildren = countNBChildren(root);
        int c = nbChildren; //node.getChildNodes().getLength();
        int result = c;

        if(childrenNodes.getLength() > 1){
            for (int i=0; i< childrenNodes.getLength(); i++) {
                if (childrenNodes.item(i).getNodeType() != Node.TEXT_NODE) {
                    result += countNBNodes(root.getChildNodes().item(i));
                }
            }
        }
        else {
            result++;
        }

        return result;
    }

    //read tree by breadth first traversal
    private void readTreeDepthFirst(Node node , Vector <NodeFreqT> trans) {
        try {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                // add this node to trans
                trans.elementAt(id).setNodeLabel(node.getNodeName());
                //System.out.print(node.getNodeName());
                //add node degree
                int nbChildren = countNBChildren(node);
                //get LineNr attribute if need

                //find line number of this node.
                String lineNbTemp = "0";
                if (node.hasAttributes()) {
                    // get attributes names and values
                    NamedNodeMap nodeMap = node.getAttributes();
                    for(int i=0; i<nodeMap.getLength(); ++i)
                        if(nodeMap.item(i).getNodeName().equals("LineNr"))
                            lineNbTemp = nodeMap.getNamedItem("LineNr").getNodeValue();
                }
                //System.out.println(" "+lineNbTemp);
                trans.elementAt(id).setLineNr(lineNbTemp);

                //keep positions to calculate relationships: parent - child - sibling
                sr.addElement(id);
                ++id;

                if (node.hasChildNodes()) {
                    //get list of children
                    NodeList nodeList = node.getChildNodes();
                    //if node is a parent of a leaf node
                    if (node.getChildNodes().getLength() == 1) {
                        //add leaf node label
                        //System.out.println(node.getTextContent().trim());
                        trans.elementAt(id).setNodeLabel("*" + node.getTextContent().trim());
                        trans.elementAt(id).setLineNr("-1");
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
                    } else {//internal node
                        //add children without sorting
                        for (int i = 0; i < nodeList.getLength(); ++i) {
                            if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                                readTreeDepthFirst(nodeList.item(i), trans);
                            }
                        }

                        /*
                        //add children after sorted
                        String nodeOrder  = "";
                        String nodeDegree = "";
                        if(grammar.containsKey(node.getNodeName())) {
                            nodeOrder  = grammar.get(node.getNodeName()).elementAt(0);
                            nodeDegree = grammar.get(node.getNodeName()).elementAt(1);
                        }

                        //sort children if they are unordered and degree = "1..*"
                        if(nodeOrder.equals("unordered") && nodeDegree.equals("1..*")){
                            //sort children by labels if node is a nodelist ==> OK
                            ArrayList<Node> sortedNodeList = new ArrayList<>();
                            for(int i = 0; i< nodeList.getLength(); ++i)
                            {
                                if(nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
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
                        }else{
                            //loop for each child without sorted
                            for (int i = 0; i < nodeList.getLength(); ++i) {
                                if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                                    readTreeDepthFirst(nodeList.item(i), trans);
                                }
                            }
                        }
                        */
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
        }catch (Exception e){System.out.println("readTreeDepthFirst "+ e);}
    }

    //create transaction from ASTs in 1 folder
    public void createTransaction(String path, Vector < Vector<NodeFreqT> > transaction ) {
        try {

            System.out.print("create tree data: ");

            //read folder
            File folder = new File(path);
            File[] listOfFiles = folder.listFiles();
            Arrays.sort(listOfFiles);

            for (int l = 0; l < listOfFiles.length; ++l)
                if (listOfFiles[l].isFile() && listOfFiles[l].getName().charAt(0)!='.') {

                    //for each file in folder create one tree
                    String fileName = path + "/" + listOfFiles[l].getName();
                    File fXmlFile = new File(fileName);
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(fXmlFile);
                    doc.getDocumentElement().normalize();

                    //System.out.println("file "+listOfFiles[l].getName());


                    //get total number of nodes
                    int size = countNBNodes(doc.getDocumentElement());

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
                        nodeTemp.setLineNr("0");

                        trans.setElementAt(nodeTemp, i);
                        sibling.setElementAt(-1, i);
                    }

                    //create tree
                    readTreeDepthFirst(doc.getDocumentElement(), trans);

                    //add tree to transaction
                    transaction.addElement(trans);
                }
            System.out.println("finished");

        } catch (Exception e) { System.out.println("input error");}

    }

    //create transaction from ASTs in multiple folders
    public void createTransaction(File f, Map <String, Vector<String> > _grammar, Vector < Vector<NodeFreqT> > transaction) {
        try {
            //System.out.print("create tree data: ");
            grammar = _grammar;

            File[] subdir = f.listFiles();
            Arrays.sort(subdir);

            for (File fi : subdir) {
                if (fi.isFile() && fi.getName().charAt(0)!='.' ) {
                    String[] split = fi.getName().split("\\.");
                    String ext = split[split.length - 1];
                    if(ext.toLowerCase().equals("xml")){
                        //System.out.print("reading file ---------------- ");
                        //System.out.println(f+"/"+fi.getName());
                        ++nbFiles;
                        //for each file in folder create one tree
                        File fXmlFile = new File(f+"/"+fi.getName());
                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                        Document doc = dBuilder.parse(fXmlFile);
                        doc.getDocumentElement().normalize();

                        //get total number of nodes
                        int size = countNBNodes(doc.getDocumentElement())+1;

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
                        //test xml parsing
                        //visitNode(doc.getDocumentElement());
                        //create tree
                        readTreeDepthFirst(doc.getDocumentElement() , trans);
                        //add tree to transaction
                        transaction.addElement(trans);
                    }

                }else
                    if (fi.isDirectory()) {
                        createTransaction(fi , grammar, transaction );
                    }
            }
            //System.out.println("input : "+nbFiles);
        } catch (Exception e) { System.out.println("input error");}

    }

    private static void visitNode(Node node){
        if (node.getNodeType() == Node.ELEMENT_NODE){
            System.out.print("node: "+node.getNodeName()+" ; ");
            if(node.hasChildNodes()){
                System.out.print("children: ");
                NodeList nodeList = node.getChildNodes();
                if(nodeList.getLength()==1){
                    if(node.getNodeType()==Node.ELEMENT_NODE){
                        System.out.println("*"+node.getTextContent().trim());
                    }
                }else{
                    for(int i=0; i<nodeList.getLength();++i)
                    {
                        if(nodeList.item(i).getNodeType()==Node.ELEMENT_NODE){
                            System.out.print(nodeList.item(i).getNodeName()+" ");
                        }
                    }
                    System.out.println();

                    for(int i=0; i<nodeList.getLength();++i)
                    {
                        if(nodeList.item(i).getNodeType()==Node.ELEMENT_NODE){
                            visitNode(nodeList.item(i));
                        }
                    }
                }
            }
        }


    }

    public static void printTransaction(Vector < Vector<NodeFreqT> > trans){
        for(int i=0; i<trans.size(); ++i){
            for(int j=0; j<trans.elementAt(i).size(); ++j)
                System.out.print((trans.elementAt(i).elementAt(j).getNodeLabel())+" ");
            System.out.println();
        }
    }

}
