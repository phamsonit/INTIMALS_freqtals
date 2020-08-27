package be.intimals.freqt.input;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.structure.NodeFreqT;
import be.intimals.freqt.util.Variables;
import be.intimals.freqt.util.XmlFormatter;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

    private boolean abstractLeafs = false;

    private String sep = "/";//File.separator;

    //////////////////////////////

    //read 2-class ASTs, and remove black labels
    public void readDatabase(Boolean _abstractLeaf, ArrayList <ArrayList<NodeFreqT>> database, int classID, File rootDirectory,
                             Map <Integer, String> labelIndex, ArrayList<Integer> classIndex, String whiteLabelPath) {

        ArrayList<String> files = new ArrayList<>();
        populateFileListNew(rootDirectory,files);
        Collections.sort(files);
        //read white labels from file
        Map<String,Set<String>> whiteLabels = readWhiteLabel(whiteLabelPath);

        abstractLeafs = _abstractLeaf;

        //System.out.print("Reading " + files.size() +" files ");
        //XmlFormatter formatter = new XmlFormatter();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            //for (File fi : files) {
            for (String fi : files) {
                countSection = 0;
                //store class label of transaction id
                classIndex.add(classID);

                //format XML file before create tree
                // String inFileTemp = rootDirectory+sep+"temp.xml";
                // Files.deleteIfExists(Paths.get(inFileTemp));
                // formatter.format(fi,inFileTemp);

                //read XML file
                File fXmlFile = new File(fi);
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(fXmlFile);
                doc.getDocumentElement().normalize();
                //get total number of nodes
                int size = countNBNodes(doc.getDocumentElement())+1;
                //initial tree parameters
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
                readTreeDepthFirst(doc.getDocumentElement(), trans, labelIndex, whiteLabels);
                //add tree to database
                database.add(trans);
                //delete temporary input file
                //Files.deleteIfExists(Paths.get(inFileTemp));
                //System.out.print(".");
            }
            //System.out.println(" reading ended.");
        } catch (Exception e) {
            System.out.println(" read AST error.");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    //collect full file names in a directory
    private void populateFileListNew(File directory, ArrayList<String> list){
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
        ArrayList<String> fullNames = new ArrayList<>();
        for(int i=0; i<files.length; ++i)
            fullNames.add(files[i].getAbsolutePath());
        list.addAll(fullNames);
        File[] directories = directory.listFiles(File::isDirectory);
        for (File dir : directories) populateFileListNew(dir,list);
    }

    //ignore black labels when reading tree by breadth first traversal,
    private void readTreeDepthFirst(Node node , ArrayList <NodeFreqT> trans, Map <Integer, String> labelIndex,
                                    Map<String,Set<String>> whiteLabels) {
        try {
            //if this is an internal node
            if(node.getNodeType() == Node.ELEMENT_NODE) {
                //System.out.println("internal node: " + node.getNodeName());
                //add node label to trans
                trans.get(id).setNodeLabel(node.getNodeName());
                //update index labels
                updateLabelIndex(node.getNodeName(), trans, labelIndex);
                //find line number of this node.
                String lineNbTemp = findLineNr(node);
                //add line number to current node id
                trans.get(id).setLineNr(lineNbTemp);
                //count SectionStatementBlock: only using for Cobol
                countSectionStatementBlock(node, lineNbTemp);

                //increase id
                sr.add(id);
                ++id;
                //recursively read children
                NodeList nodeList = node.getChildNodes();
                //only read children labels which are in the white list
                if(whiteLabels.containsKey(node.getNodeName())){
                    Set<String> temp = whiteLabels.get(node.getNodeName());
                    for(int i = 0; i < nodeList.getLength(); ++i)
                        if(temp.contains(nodeList.item(i).getNodeName())) {
                            readTreeDepthFirst(nodeList.item(i), trans, labelIndex, whiteLabels);
                        }
                }else {
                    for (int i = 0; i < nodeList.getLength(); ++i) {
                        readTreeDepthFirst(nodeList.item(i), trans, labelIndex, whiteLabels);
                    }
                }
                //calculate parent, child, sibling of internal node
                calculatePositions(trans);
            }
            else {//this is a leaf
                if(node.getNodeType() == Node.TEXT_NODE && !node.getTextContent().trim().isEmpty()){
                    //if a has sibling it is not a unique leaf
                    Node a = node.getNextSibling();
                    Node b = node.getPreviousSibling();

                    if(a == null && b == null){
                        //System.out.println("leaf node: "+node.getTextContent().trim());
                        String leafLabel;
                        if(abstractLeafs)
                            leafLabel = "**";
                        else
                            leafLabel = "*" + node.getTextContent().replace(",", Variables.uniChar).trim();
                        //add leaf node label to trans
                        trans.get(id).setNodeLabel(leafLabel);
                        //update labelIndex for leaf labels
                        if(!labels.contains(leafLabel)) {
                            trans.get(id).setNode_label_int(labelIndex.size()*(-1));
                            labelIndex.put(labelIndex.size()*(-1), leafLabel);
                            labels.add(leafLabel);
                        }else {
                            trans.get(id).setNode_label_int(labels.indexOf(leafLabel)*(-1));
                        }
                        //set line number of leaf node to -1
                        trans.get(id).setLineNr("-1");
                        //increase id
                        sr.add(id);
                        ++id;
                        //calculate parent, child, sibling of this leaf node
                        calculatePositions(trans);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void calculatePositions(ArrayList<NodeFreqT> trans) {
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

    private void countSectionStatementBlock(Node node, String lineNbTemp) {
        if(node.getNodeName().equals("SectionStatementBlock") && countSection < 2) {
            countSection++;
        }else
            if(countSection == 2) {
                lineNrs.add(Integer.valueOf(lineNbTemp));
                countSection++;
            }
    }

    private String findLineNr(Node node) {
        String lineNbTemp = "0";
        if (node.hasAttributes()) {
            // get attributes names and values
            NamedNodeMap nodeMap = node.getAttributes();
            for(int i=0; i<nodeMap.getLength(); ++i)
                if(nodeMap.item(i).getNodeName().equals("LineNr"))
                    lineNbTemp = nodeMap.getNamedItem("LineNr").getNodeValue();
        }
        return lineNbTemp;
    }

    private void updateLabelIndex(String nodeLabel, ArrayList<NodeFreqT> trans, Map<Integer, String> labelIndex) {
        //update labelIndex for internal labels
        if(labelIndex.isEmpty() && labels.isEmpty()) {
            trans.get(id).setNode_label_int(0);
            labelIndex.put(0, nodeLabel);
            labels.add(nodeLabel);
        }
        else{
            if(!labels.contains(nodeLabel)) {
                trans.get(id).setNode_label_int(labelIndex.size());
                labelIndex.put(labelIndex.size(), nodeLabel);
                labels.add(nodeLabel);
            }else{
                trans.get(id).setNode_label_int(labels.indexOf(nodeLabel));
            }
        }
    }

    //read white labels from given file
    public static Map<String,Set<String> > readWhiteLabel(String path){
        Map<String,Set<String> > _whiteLabels = new HashMap();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if( ! line.isEmpty() && line.charAt(0) != '#' ) {
                    String[] str_tmp = line.split(" ");
                    String ASTNode = str_tmp[0];
                    Set<String> children = new HashSet<>();
                    for(int i=1; i<str_tmp.length; ++i){
                        children.add(str_tmp[i]);
                    }
                    _whiteLabels.put(ASTNode,children);
                }
            }
        }catch (IOException e) {
            System.out.println("Error: reading white list "+e);
        }
        return _whiteLabels;
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

    //count total number of nodes of a Python XML
    private int countNBNodes(Node root) {
        int count = 0;
        if(root.getNodeType() == Node.ELEMENT_NODE) {
            count++;
            NodeList children = root.getChildNodes();
            for(int i=0; i<children.getLength(); ++i)
                count += countNBNodes(children.item(i));
        }
        else
            if (root.getNodeType() == Node.TEXT_NODE && !root.getTextContent().trim().isEmpty()) {
                Node a = root.getNextSibling();
                Node b = root.getPreviousSibling();
                if(a == null && b== null)
                    count++;
            }
        return count;
    }

    //return total number of reading files
    public ArrayList<Integer> getlineNrs(){return this.lineNrs;
    }
}
