package be.intimals.freqt.input;

import be.intimals.freqt.structure.NodeFreqT;
import be.intimals.freqt.util.Variables;
import be.intimals.freqt.util.XmlFormatter;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/*
create tree data from ASTs
 */
//import java.io.File;


public class ReadXML_Int {

    private int top;
    private int id;
    private ArrayList<Integer> sr;
    private ArrayList<Integer> sibling;

    private List<String> labels = new LinkedList<>();
    ArrayList<Integer> lineNrs = new ArrayList<>();
    int countSection;
    private boolean abstractLeafs;

    private String sep = "/";//File.separator;

    //////////////////////////////

    //return total number of reading files
    public ArrayList<Integer> getlineNrs(){return this.lineNrs;
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
        }else {
            result++;
        }
        return result;
    }

    //read tree by breadth first traversal
    private void readTreeDepthFirst(Node node , ArrayList <NodeFreqT> trans, Map <Integer, String> labelIndex ) {
        try {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                // add this node label into trans
                trans.get(id).setNodeLabel(node.getNodeName());
                //System.out.print(node.getNodeName());

                //update labelIndex for internal labels
                if(labelIndex.isEmpty() && labels.isEmpty()) {
                    trans.get(id).setNode_label_int(0);
                    labelIndex.put(0, node.getNodeName());
                    labels.add(node.getNodeName());
                }
                else{
                    if(!labels.contains(node.getNodeName())) {
                        trans.get(id).setNode_label_int(labelIndex.size());
                        labelIndex.put(labelIndex.size(), node.getNodeName());
                        labels.add(node.getNodeName());
                    }else{
                        trans.get(id).setNode_label_int(labels.indexOf(node.getNodeName()));
                    }
                }

                int nbChildren = countNBChildren(node);

                //find line number of this node.
                String lineNbTemp = "0";
                if (node.hasAttributes()) {
                    // get attributes names and values
                    NamedNodeMap nodeMap = node.getAttributes();
                    for(int i=0; i<nodeMap.getLength(); ++i)
                        if(nodeMap.item(i).getNodeName().equals("LineNr"))
                            lineNbTemp = nodeMap.getNamedItem("LineNr").getNodeValue();
                }
                //only using for Cobol
                if(node.getNodeName().equals("SectionStatementBlock") && countSection < 2) {
                    countSection++;
                }else
                    if(countSection==2) {
                        lineNrs.add(Integer.valueOf(lineNbTemp));
                        countSection++;
                    }
                //System.out.println(" "+lineNbTemp);
                trans.get(id).setLineNr(lineNbTemp);
                ///////////////////////////////////////////////////////////

                //keep positions to calculate relationships: parent - child - sibling
                sr.add(id);
                ++id;
                if (node.hasChildNodes()) {
                    //get list of children
                    NodeList nodeList = node.getChildNodes();
                    //if node is a parent of a leaf node
                    if (node.getChildNodes().getLength() == 1) {
                        String leafLabel;
                        if(abstractLeafs)
                            leafLabel = "**";
                        else
                            leafLabel = "*" + node.getTextContent().replace(",",Variables.uniChar).trim();
                        //add leaf node label
                        trans.get(id).setNodeLabel(leafLabel);
                        //update labelIndex for leaf labels
                        if(!labels.contains(leafLabel)) {
                            trans.get(id).setNode_label_int(labelIndex.size()*(-1));
                            labelIndex.put(labelIndex.size()*(-1), leafLabel);
                            labels.add(leafLabel);
                        }else {
                            trans.get(id).setNode_label_int(labels.indexOf(leafLabel)*(-1));
                        }

                        trans.get(id).setLineNr("-1");

                        //System.out.println("node "+trans.elementAt(id).getNodeLabel());
                        sr.add(id);
                        ++id;
                        //////close a node and calculate parent, child, sibling
                        top = sr.size() - 1;
                        int child = sr.get(top);
                        int parent = sr.get(top - 1);
                        trans.get(child).setNodeParent(parent);
                        if (trans.get(parent).getNodeChild() == -1)
                            trans.get(parent).setNodeChild(child);
                        if (sibling.get(parent) != -1)
                            trans.get(sibling.get(parent)).setNodeSibling(child);
                        sibling.set(parent,child);
                        sr.remove(top);
                        ///////////////
                    } else {//internal node
                        //add children without sorting
                        for (int i = 0; i < nodeList.getLength(); ++i) {
                            if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                                readTreeDepthFirst(nodeList.item(i), trans, labelIndex);
                            }
                        }
                        /*
                        //sort add children
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
                int child = sr.get(top);
                int parent = sr.get(top - 1);
                trans.get(child).setNodeParent(parent);
                if (trans.get(parent).getNodeChild() == -1)
                    trans.get(parent).setNodeChild(child);
                if (sibling.get(parent) != -1)
                    trans.get(sibling.get(parent)).setNodeSibling(child);
                sibling.set(parent,child);
                sr.remove(top);
            }
        }catch (Exception e){
            e.printStackTrace();
            System.exit(-1);
        }
    }

    //read tree from 2-class dataset
    public void readDatabase(ArrayList <ArrayList<NodeFreqT>> database, int classID, boolean _abstractLeafs,
                             File rootDirectory, Map <Integer, String> labelIndex,
                             ArrayList<Integer> classIndex) {
        //ArrayList < ArrayList<NodeFreqT> > database = new ArrayList < ArrayList<NodeFreqT> >();
        abstractLeafs = _abstractLeafs;

        ArrayList<String> files = new ArrayList<>();
        populateFileListNew(rootDirectory,files);
        Collections.sort(files);
        //ArrayList<File> files = new ArrayList<File>();
        //populateFileList(rootDirectory,files);
        System.out.print("Reading " + files.size() +" files ");
        XmlFormatter formatter = new XmlFormatter();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            //for (File fi : files) {
            for (String fi : files) {
                countSection=0;
                //store class label of transaction id
                classIndex.add(classID);

                //format XML file before create tree
                // String inFileTemp = rootDirectory+sep+"temp.xml";
                // Files.deleteIfExists(Paths.get(inFileTemp));
                // formatter.format(fi,inFileTemp);

                //create tree
                File fXmlFile = new File(fi);
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(fXmlFile);
                doc.getDocumentElement().normalize();

                //get total number of nodes
                int size = countNBNodes(doc.getDocumentElement())+1;

                id = 0;
                top = 0;
                sr = new ArrayList<>();
                sibling = new ArrayList<>(size);
                ArrayList<NodeFreqT> trans = new ArrayList<NodeFreqT>(size);

                for (int i = 0; i < size; ++i) {
                    NodeFreqT nodeTemp = new NodeFreqT(-1,-1,-1,"0",true);
                    trans.add(nodeTemp);
                    sibling.add(-1);
                }
                //create tree
                readTreeDepthFirst(doc.getDocumentElement(), trans, labelIndex);
                //add tree to database
                database.add(trans);
                //delete temporary input file
                //Files.deleteIfExists(Paths.get(inFileTemp));
                System.out.print(".");
            }
            System.out.println(" reading ended.");
        } catch (Exception e) {
            System.out.println(" read error.");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    //create transaction from ASTs in multiple folders
    public ArrayList <ArrayList<NodeFreqT>> readDatabase(boolean _abstractLeafs, File rootDirectory, Map <Integer, String> labelIndex) {
        ArrayList < ArrayList<NodeFreqT> > database = new ArrayList < ArrayList<NodeFreqT> >();
        abstractLeafs = _abstractLeafs;

        ArrayList<String> files = new ArrayList<>();
        populateFileListNew(rootDirectory,files);
        Collections.sort(files);
        //ArrayList<File> files = new ArrayList<File>();
        //populateFileList(rootDirectory,files);
        System.out.print("Reading " + files.size() +" files ");
        XmlFormatter formatter = new XmlFormatter();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            //for (File fi : files) {
            for (String fi : files) {
                countSection=0;
                //format XML file before create tree
                String inFileTemp = rootDirectory+sep+"temp.xml";
                Files.deleteIfExists(Paths.get(inFileTemp));
                formatter.format(fi,inFileTemp);

                //create tree
                File fXmlFile = new File(inFileTemp);
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(fXmlFile);
                doc.getDocumentElement().normalize();

                //get total number of nodes
                int size = countNBNodes(doc.getDocumentElement())+1;

                id = 0;
                top = 0;
                sr = new ArrayList<>();
                sibling = new ArrayList<>(size);
                ArrayList<NodeFreqT> trans = new ArrayList<NodeFreqT>(size);

                for (int i = 0; i < size; ++i) {
                    NodeFreqT nodeTemp = new NodeFreqT(-1,-1,-1,"0",true);
                    trans.add(nodeTemp);
                    sibling.add(-1);
                }
                //create tree
                readTreeDepthFirst(doc.getDocumentElement(), trans, labelIndex);
                //add tree to database
                database.add(trans);
                //delete temporary input file
                Files.deleteIfExists(Paths.get(inFileTemp));
                System.out.print(".");
            }
            System.out.println(" reading ended.");
        } catch (Exception e) {
            System.out.println(" read error.");
            e.printStackTrace();
            System.exit(-1);
        }
        return database;
    }

    private void populateFileList(File directory, ArrayList<File> list){
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
        Collections.addAll(list, files);
        File[] directories = directory.listFiles(File::isDirectory);
        for (File dir : directories) populateFileList(dir,list);
    }
	
    //collect full name of files in the directory
    private void populateFileListNew(File directory, ArrayList<String> list){
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
        ArrayList<String> fullNames = new ArrayList<>();
        for(int i=0; i<files.length; ++i)
            fullNames.add(files[i].getAbsolutePath());
        list.addAll(fullNames);
        File[] directories = directory.listFiles(File::isDirectory);
        for (File dir : directories) populateFileListNew(dir,list);
    }

    public static void printTransaction(ArrayList < ArrayList<NodeFreqT> > trans){
        for(int i=0; i<trans.size(); ++i){
            for(int j=0; j<trans.get(i).size(); ++j)
                System.out.print((trans.get(i).get(j).getNodeLabel())+"-"+trans.get(i).get(j).getNode_label_int()+" , ");
            System.out.println();
        }
    }

}
