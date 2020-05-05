/*
create grammar for ASTs
 */
package be.intimals.freqt.grammar;

import be.intimals.freqt.input.*;

import java.util.*;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import be.intimals.freqt.util.Variables;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

public class CreateGrammar extends ReadXML_Int {

    private Map<String,Set<String> > whiteLabels = new HashMap<>();

    //////////////////// NEW /////////////////////
    public void createGrammar(String path, String white, Map <String, ArrayList<String> > grammar) throws Exception {
        whiteLabels = readWhiteLabel(white);
        createGrammar(new File(path), grammar);
    }

    //create grammar from multiple files
    public void createGrammar(File f, Map <String, ArrayList<String> > grammar) throws Exception {
        File[] subdir = f.listFiles();
        Arrays.sort(subdir);
        for (File fi : subdir) {
            if (fi.isFile() && fi.getName().charAt(0)!='.') {
                String[] split = fi.getName().split("\\.");
                String ext = split[split.length - 1];
                if(ext.toLowerCase().equals("xml")){
                    //System.out.print("reading file ----------------");
                    //System.out.println(f+"/"+fi.getName());
                    File fXmlFile = new File(f+"/"+fi.getName());
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(fXmlFile);
                    doc.getDocumentElement().normalize();
                    //create grammar
                    readGrammarDepthFirst(doc.getDocumentElement(), grammar);
                }
            }else
            if (fi.isDirectory()) {
                //System.out.println("inside if check directory");
                createGrammar(fi, grammar);
            }
        }
    }

    //recur reading an AST node
    private void readGrammarDepthFirst(Node node , Map <String, ArrayList<String> > grammar ) {
        try {
            // make sure it's element is a node type.
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (grammar.containsKey(node.getNodeName())) {
                    updateNode(node,grammar);
                } else {
                    addNewNode(node,grammar);
                }
                if (node.hasChildNodes()) {
                    //get list of children
                    NodeList nodeList = node.getChildNodes();
                    if(whiteLabels.containsKey(node.getNodeName())){
                        Set<String> whiteChildren = whiteLabels.get(node.getNodeName());
                        // loop for each child
                        for (int i = 0; i < nodeList.getLength(); ++i) {
                            if(nodeList.item(i).getNodeType() == Node.ELEMENT_NODE)
                                if (whiteChildren.contains(nodeList.item(i).getNodeName())) {
                                    readGrammarDepthFirst(nodeList.item(i), grammar);
                                }
                        }
                    }else {
                        // loop for each child
                        for (int i = 0; i < nodeList.getLength(); ++i) {
                            if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                                readGrammarDepthFirst(nodeList.item(i), grammar);
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            System.out.println("Grammar error:" + e);
            e.printStackTrace();
        }
    }

    //add a new node to grammar
    private void addNewNode(Node node , Map <String, ArrayList<String> > grammar){
        int nbChildren = countNBChildren(node);
        NodeList childrenList = node.getChildNodes();
        ArrayList<String> tmp = new ArrayList<>();
        if (nbChildren == 0) {//add leaf node
            if (node.getNodeType() == Node.ELEMENT_NODE){
                tmp.add("ordered");
                //tmp.addElement("unordered");
                tmp.add("1");
                //keep leaf node in grammar if necessary
                //tmp.addElement(node.getTextContent().trim() + uniChar + "false");
                tmp.add("leaf-node" + Variables.uniChar + "false");
            }
        } else { //add internal node
            //1 - find children
            Map<String, String> childrenTemp = new LinkedHashMap<>();
            //find children of the current node
            boolean repeatedChild = isRepeatedChild(node, childrenList, childrenTemp);

            if(repeatedChild){
                tmp.add("unordered");
                tmp.add("1..*");
                for(Map.Entry<String, String> entry : childrenTemp.entrySet())
                    tmp.add(entry.getKey() + Variables.uniChar + "false");
            }else{
                tmp.add("ordered");
                //tmp.add(String.valueOf(childNumber-1));
                tmp.add(String.valueOf(childrenTemp.size()));
                for(Map.Entry<String, String> entry : childrenTemp.entrySet())
                    tmp.add(entry.getKey() + Variables.uniChar + entry.getValue());
            }
        }
        //System.out.println(node.getNodeName()+" "+tmp);
        grammar.put(node.getNodeName(), tmp);
    }

    //update a node
    private void updateNode(Node node , Map <String, ArrayList<String> > grammar ){
        int nbChildren = countNBChildren(node);
        if(nbChildren==0){//leaf node
            updateLeafNode(node,grammar);
        }else{//internal node
            updateInternalNode(node,grammar);
        }
    }

    //updating internal node
    private void updateInternalNode(Node node , Map <String, ArrayList<String> > grammar){
        //find grammar of this current node
        ArrayList<String> oldGrammar = grammar.get(node.getNodeName());
        String oldDegree = oldGrammar.get(1);
        //find children of the current node in grammar
        Map<String, String> oldChildren = new LinkedHashMap<>();
        for (int i = 2; i < oldGrammar.size(); ++i) {
            String[] temp = oldGrammar.get(i).split(Variables.uniChar);
            oldChildren.put(temp[0], temp[1]);
        }
        //find children of the current node
        NodeList childrenList = node.getChildNodes();
        Map<String, String> newChildren = new LinkedHashMap<>();
        boolean repeatedChild = isRepeatedChild(node, childrenList, newChildren);

        ArrayList<String> tmp = new ArrayList<>();
        if(repeatedChild){
            tmp.add("unordered");
            tmp.add("1..*");
            newChildren.putAll(oldChildren);
            for(Map.Entry<String, String> entry : newChildren.entrySet())
                tmp.add(entry.getKey()+Variables.uniChar + "false");
        }else{
            if(newChildren.size() == 1 && oldDegree.equals("1")){
                tmp.add("ordered");
                tmp.add("1");
                newChildren.putAll(oldChildren);
                if(newChildren.size()>1){
                    for(Map.Entry<String, String> entry : newChildren.entrySet())
                        tmp.add(entry.getKey() + Variables.uniChar + "false");
                }else{
                    for(Map.Entry<String, String> entry : newChildren.entrySet())
                        tmp.add(entry.getKey() + Variables.uniChar + "true");
                }
            }else{
                if(oldDegree.equals("1..*")){
                    tmp.add("unordered");
                    tmp.add("1..*");
                    newChildren.putAll(oldChildren);
                    for(Map.Entry<String, String> entry : newChildren.entrySet())
                        tmp.add(entry.getKey() + Variables.uniChar + "false");
                }else { // update grammar [unordered, N..M, list of children]
                    //calculate intersection of old and new children
                    Map<String,String> inter = inter(oldChildren, newChildren);
                    //calculate union of old and new children
                    newChildren.putAll(oldChildren);
                    tmp.add("ordered");
                    if(inter.size() != newChildren.size()){
                        tmp.add(String.valueOf(inter.size() + ".." + newChildren.size()));
                        //update children
                        for(Map.Entry<String, String> entry: newChildren.entrySet())
                            if(inter.containsKey(entry.getKey()))
                                tmp.add(entry.getKey() + Variables.uniChar + "true");
                            else
                                tmp.add(entry.getKey() + Variables.uniChar + "false");
                    }else{
                        //update degree
                        tmp.add(String.valueOf(inter.size()));
                        //update children
                        for(Map.Entry<String, String > entry : newChildren.entrySet())
                            tmp.add(entry.getKey() + Variables.uniChar + entry.getValue());
                    }
                }
            }
        }
        grammar.replace(node.getNodeName(), tmp);
    }

    //find children of a node
    private boolean isRepeatedChild(Node node, NodeList childrenList, Map<String, String> childrenTemp) {
        boolean repeatedChild = false;
        if(whiteLabels.containsKey(node.getNodeName())){
            Set<String> tmpChild = whiteLabels.get(node.getNodeName());
            for (int i = 0; i < childrenList.getLength(); ++i) {
                if (childrenList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    if(tmpChild.contains(childrenList.item(i).getNodeName())) {
                        if (childrenTemp.containsKey(childrenList.item(i).getNodeName())) {
                            childrenTemp.replace(childrenList.item(i).getNodeName(), "false");
                            repeatedChild = true;
                        } else {
                            childrenTemp.put(childrenList.item(i).getNodeName(), "true");
                        }
                    }
                }
            }
        }else{
            for (int i = 0; i < childrenList.getLength(); ++i) {
                if (childrenList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    if (childrenTemp.containsKey(childrenList.item(i).getNodeName())) {
                        childrenTemp.replace(childrenList.item(i).getNodeName(), "false");
                        repeatedChild = true;
                    } else {
                        childrenTemp.put(childrenList.item(i).getNodeName(), "true");
                    }
                }
            }
        }
        return repeatedChild;
    }

    //update node having only leafs
    private void updateLeafNode(Node node , Map <String, ArrayList<String> > grammar ){
        ArrayList<String> tmp = new ArrayList<>();
        tmp.add("ordered");
        //tmp.addElement("unordered");
        tmp.add("1");
        tmp.add("leaf-node"+Variables.uniChar+"false");
        /*
        //keep all leaf nodes if necessary
        oldChildren.put(node.getTextContent().trim(), "false");
        Iterator<Map.Entry<String, String>> iterTemp = oldChildren.entrySet().iterator();
        while (iterTemp.hasNext()) {
            Map.Entry<String, String> entry = iterTemp.next();
            tmp.addElement(entry.getKey() + uniChar + "false");
        }
        */
        grammar.replace(node.getNodeName(), grammar.get(node.getNodeName()), tmp);
    }

    //find intersection elements of two children lists
    private Map<String,String> inter(Map <String, String> oldChildren,Map <String, String> newChildren) {
        Map<String,String> inter = new LinkedHashMap<>();
        Iterator<Map.Entry<String, String>> iterTemp2 = oldChildren.entrySet().iterator();
        while (iterTemp2.hasNext()) {
            Map.Entry<String, String> entry2 = iterTemp2.next();
            if (newChildren.containsKey(entry2.getKey()) &&
                    entry2.getValue().equals("true"))
                inter.put(entry2.getKey(),entry2.getValue());
        }
        return inter;
    }

    //TODO: keep correct order of children ?
    private Map<String,String> union(Map <String, String> oldChildren, Map <String, String> newChildren) {

        Map<String,String> union = new LinkedHashMap<>();

        Iterator<Map.Entry<String, String>> iterTemp1 = newChildren.entrySet().iterator();
        Iterator<Map.Entry<String, String>> iterTemp2 = oldChildren.entrySet().iterator();

        while (iterTemp2.hasNext()) {
            Map.Entry<String, String> entry2 = iterTemp2.next();
            union.put(entry2.getKey(),entry2.getValue());
        }
        while (iterTemp1.hasNext()) {
            Map.Entry<String, String> entry1 = iterTemp1.next();
            union.put(entry1.getKey(),entry1.getValue());
        }
        return union;
    }
}
