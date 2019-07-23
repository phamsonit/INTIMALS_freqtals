package be.intimals.freqt.input;

import be.intimals.freqt.structure.NodeFreqT;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Vector;

/*
create tree data from ASTs
 */
//import java.io.File;


public class CreateSingleTree extends ReadXML {

    private int top;
    private int id;
    private Vector<Integer> sr;
    private Vector<Integer> sibling;

    private Map <String, Vector<String> > grammar;
    private int nbFiles = 0;

    public static  char uniChar = '\u00a5';// Japanese Yen symbol

    boolean abstractLeafs = false;

    Vector<Integer> lineNrs = new Vector<>();
    int countSection;

    private int nodeLevel;


    //merge trans into singleTree
    private void mergeTrees(Vector <NodeFreqT> trans, Vector <NodeFreqT> singleTree){
        if(singleTree.isEmpty()){
            System.out.println("create a new singleTree");
            //the first node
            NodeFreqT node = trans.elementAt(0);
            node.setNodeParentExt(-1);
            node.setNodeChildExt(1);
            node.setNodeSiblingExt(-1);
            singleTree.add(node);
            for(int i=1; i<trans.size()-1; ++i){

                node = trans.elementAt(i);
                node.setNodeParentExt(i-1);
                node.setNodeChildExt(i+1);
                node.setNodeSiblingExt(-1);
                singleTree.add(node);

            }
            //the last node
            node = trans.elementAt(trans.size()-1);
            node.setNodeParentExt(trans.size()-2);
            node.setNodeChildExt(-1);
            node.setNodeSiblingExt(-1);
            singleTree.add(node);
        }
        else{
            System.out.println("mergeTrees");
        }

    }

    //read tree by breadth first traversal
    private void readTreeDepthFirst(Node node, Vector <NodeFreqT> trans) {
        try {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                // add this node label into trans
                trans.elementAt(id).setNodeLabel(node.getNodeName());
                //System.out.print(node.getNodeName());

                trans.elementAt(id).setNodeLevel(nodeLevel);

                int nbChildren = countNBChildren(node);

                /////////find line number of this node//////
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
                trans.elementAt(id).setLineNr(lineNbTemp);
                ///////////////////////////////////////////////////////////

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
                        if(abstractLeafs) //abstract leafs of Cobol: change all leafs to **
                            trans.elementAt(id).setNodeLabel("**");
                        else
                            trans.elementAt(id).setNodeLabel("*" + node.getTextContent().replace(",",String.valueOf(uniChar)).trim());

                        trans.elementAt(id).setLineNr("-1");
                        ++nodeLevel;
                        trans.elementAt(id).setNodeLevel(nodeLevel);
                        //System.out.println("node "+trans.elementAt(id).getNodeLabel());
                        sr.addElement(id);
                        ++id;
                        //////close a node and calculate parent, child, sibling
                        top = sr.size() - 1;
                        nodeLevel = top;
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
                                ++nodeLevel;
                                readTreeDepthFirst(nodeList.item(i), trans);
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

    //create singleTree from ASTs in multiple folders
    public void createTree(boolean _abstractLeafs, File f, Map <String, Vector<String> > _grammar, Vector<NodeFreqT> singleTree) {
        try {
            //System.out.print("create tree data: ");

            //singleTree = new Vector<>();

            abstractLeafs = _abstractLeafs;
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
                        countSection=0;
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
                        nodeLevel=1;

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
                        //transaction.addElement(trans);
                        //merge trans into singleTree
                        mergeTrees(trans,singleTree);
                    }

                }else
                    if (fi.isDirectory()) {
                        createTree(abstractLeafs, fi , grammar, singleTree );
                    }
            }
            //System.out.println("input : "+nbFiles);
        } catch (Exception e) { System.out.println("create single tree error");}

    }


}
