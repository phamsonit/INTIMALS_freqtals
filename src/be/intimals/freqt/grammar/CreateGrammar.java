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


    private void addNewNode(Node node , Map <String, Vector<String> > grammar){

        int nbChildren = countNBChildren(node);
        NodeList childrenList = node.getChildNodes();
        Vector<String> tmp = new Vector<>();
        if (nbChildren == 0) {//add leaf node
            if (node.getNodeType() == Node.ELEMENT_NODE){
                tmp.addElement("ordered");
                //tmp.addElement("unordered");
                tmp.addElement("1");
                //keep leaf node in grammar if necessary
                //tmp.addElement(node.getTextContent().trim() + uniChar + "false");
                tmp.addElement("leaf-node" + Variables.uniChar + "false");
            }
        } else { //add internal node
            //1 - find children
            Map<String, String> childrenTemp = new LinkedHashMap<>();
            int childNumber = 1;
            boolean repeatedChild = false;
            //find children of this node
            for (int i = 0; i < childrenList.getLength(); ++i) {
                if (childrenList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    //find optional node: if it has no value
                    //i.e. <elseStatement ColumnNr="8" EndLineNr="585" ID="2044" LineNr="580"/>
                    //==> can not apply this to new grammar which has no nodes include empty
                    boolean mandatory = true;
                    if(!childrenList.item(i).hasChildNodes()) {
                        //System.out.println(childrenList.item(i).getNodeName() + " : optional");
                        mandatory = false;
                    }
                    //============================
                    if (childrenTemp.containsKey(childrenList.item(i).getNodeName())) {
                        childrenTemp.replace(childrenList.item(i).getNodeName(),
                                childrenTemp.get(childrenList.item(i).getNodeName()),
                                childrenTemp.get(childrenList.item(i).getNodeName()));
                        repeatedChild = true;
                    } else {
                        childrenTemp.put(childrenList.item(i).getNodeName(), String.valueOf(mandatory) );
                        ++childNumber;
                        //childrenTemp.put(childrenList.item(i).getNodeName(), Double.valueOf("1"));
                    }
                }
            }
            if(repeatedChild){
                tmp.addElement("unordered");
                tmp.addElement("1..*");
                Iterator<Map.Entry<String, String>> iter = childrenTemp.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, String> entry = iter.next();
                    tmp.addElement(entry.getKey() + Variables.uniChar + "*");
                    //tmp.addElement("*" + uniChar + "*");
                }
            }else{
                tmp.addElement("ordered");
                //tmp.addElement("unordered");
                tmp.addElement(String.valueOf(childNumber-1));
                Iterator<Map.Entry<String, String>> iter = childrenTemp.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, String> entry = iter.next();
                    tmp.addElement(entry.getKey() + Variables.uniChar + entry.getValue());
                }
            }
        }
        //System.out.println(node.getNodeName()+" "+tmp);
        grammar.put(node.getNodeName(), tmp);
    }

    private void updateLeafNode(Node node , Map <String, Vector<String> > grammar ){
        Vector<String> tmp = new Vector<>();
        tmp.addElement("ordered");
        //tmp.addElement("unordered");
        tmp.addElement("1");
        tmp.addElement("leaf-node"+Variables.uniChar+"false");
        /*
        //collect all leaf nodes if necessary
        oldChildren.put(node.getTextContent().trim(), "false");
        Iterator<Map.Entry<String, String>> iterTemp = oldChildren.entrySet().iterator();
        while (iterTemp.hasNext()) {
            Map.Entry<String, String> entry = iterTemp.next();
            tmp.addElement(entry.getKey() + uniChar + "false");
        }
        */
        grammar.replace(node.getNodeName(), grammar.get(node.getNodeName()), tmp);

    }

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

    private Map<String,String> union(Map <String, String> oldChildren,Map <String, String> newChildren) {

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



     private void updateInternalNode(Node node , Map <String, Vector<String> > grammar){

        //find grammar of this current node
        Vector<String> oldGrammar = grammar.get(node.getNodeName());
        String oldDegree = oldGrammar.elementAt(1);
        //find children of the current node in grammar
        Map<String, String> oldChildren = new LinkedHashMap<>();
        for (int i = 2; i < oldGrammar.size(); ++i) {
            String[] temp = oldGrammar.elementAt(i).split(Variables.uniChar);
            oldChildren.put(temp[0], temp[1]);
        }

        //find children of the current node
        NodeList childrenList = node.getChildNodes();
        Map<String, String> newChildren = new LinkedHashMap<>();
        boolean repeatedChild = false;
        for (int i = 0; i < childrenList.getLength(); ++i) {
            if (childrenList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                //find optional node: if it has no value
                //i.e. <elseStatement ColumnNr="8" EndLineNr="585" ID="2044" LineNr="580"/>
                boolean mandatory = true;
                if (newChildren.containsKey(childrenList.item(i).getNodeName())) {
                    newChildren.replace(childrenList.item(i).getNodeName(),
                            newChildren.get(childrenList.item(i).getNodeName()),
                            String.valueOf(mandatory));
                    repeatedChild = true;
                } else {
                    newChildren.put(childrenList.item(i).getNodeName(), String.valueOf(mandatory));
                }
            }
        }

/*
         if(node.getNodeName().equals("EvaluateStatement")) {
             System.out.print(node.getNodeName()+": ");
             //if (node.getNodeName().equals("MoveStatement"))
             {
                 for (int k = 0; k < childrenList.getLength(); ++k)
                     if(childrenList.item(k).getNodeType()== Node.ELEMENT_NODE)
                         System.out.print(childrenList.item(k).getNodeName() + " ");
                 System.out.println();

             }
         }
*/

        //if new node has repeated child--> update grammar [unordered, 1..*,list of children]

        if(repeatedChild){
            Vector<String> tmp = new Vector<>();
            tmp.addElement("unordered");
            tmp.addElement("1..*");
            Iterator<Map.Entry<String, String>> iter = newChildren.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, String> entry = iter.next();
                oldChildren.put(entry.getKey(), entry.getValue());
            }
            Iterator<Map.Entry<String, String>> iter1 = oldChildren.entrySet().iterator();
            while (iter1.hasNext()) {
                Map.Entry<String, String> entry = iter1.next();
                tmp.addElement(entry.getKey() + Variables.uniChar + "false");
            }
            grammar.replace(node.getNodeName(), grammar.get(node.getNodeName()), tmp);
        }else{
            int nbChildren = countNBChildren(node);
            Vector<String> tmp = new Vector<>();
            if(nbChildren == 1 && oldDegree.equals("1")){
                //System.out.println(node.getNodeName()+" internal node has only 1 child");
                tmp.addElement("ordered");
                tmp.addElement("1");

                Iterator<Map.Entry<String, String>> iter = newChildren.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, String> entry = iter.next();
                    oldChildren.put(entry.getKey(), entry.getValue());
                }

                Iterator<Map.Entry<String, String>> iter1 = oldChildren.entrySet().iterator();
                while (iter1.hasNext()) {
                    Map.Entry<String, String> entry = iter1.next();
                    //tmp.addElement(entry.getKey() + uniChar + "true");
                    newChildren.put(entry.getKey(),entry.getValue());
                }

                if(newChildren.size()>1){
                    Iterator<Map.Entry<String, String>> iter2 = newChildren.entrySet().iterator();
                    while (iter2.hasNext()) {
                        Map.Entry<String, String> entry = iter2.next();
                        tmp.addElement(entry.getKey() + Variables.uniChar + "false");
                    }
                }else{
                    Iterator<Map.Entry<String, String>> iter2 = newChildren.entrySet().iterator();
                    while (iter2.hasNext()) {
                        Map.Entry<String, String> entry = iter2.next();
                        tmp.addElement(entry.getKey() + Variables.uniChar + "true");
                    }
                }
                grammar.replace(node.getNodeName(), grammar.get(node.getNodeName()), tmp);
            }else{
                if(oldDegree.equals("1..*")){
                    tmp.addElement("unordered");
                    tmp.addElement("1..*");
                    Iterator<Map.Entry<String, String>> iter = newChildren.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry<String, String> entry = iter.next();
                        oldChildren.put(entry.getKey(), entry.getValue());
                    }

                    Iterator<Map.Entry<String, String>> iter1 = oldChildren.entrySet().iterator();
                    while (iter1.hasNext()) {
                        Map.Entry<String, String> entry = iter1.next();
                        tmp.addElement(entry.getKey() + Variables.uniChar + "false");
                    }
                    grammar.replace(node.getNodeName(), grammar.get(node.getNodeName()), tmp);
                }else { // update grammar [unordered, N..M, list of children]
                    //calculate intersection of old and new children
                    Map<String,String> inter = inter(oldChildren,newChildren);
                    //calculate union of old and new children
                    Map<String,String> union = union(oldChildren,newChildren);
                    Iterator<Map.Entry<String, String>> iterTemp1;
                    tmp.addElement("ordered");
                    if (inter.size() != union.size()) {
                        //update degree
                        tmp.addElement(String.valueOf(inter.size() + ".." + union.size()));
                        //update children
                        iterTemp1 = union.entrySet().iterator();
                        while (iterTemp1.hasNext()) {
                            Map.Entry<String, String> entry1 = iterTemp1.next();
                            if (inter.containsKey(entry1.getKey()))
                                tmp.addElement(entry1.getKey() + Variables.uniChar + "true");
                            else
                                tmp.addElement(entry1.getKey() + Variables.uniChar + "false");
                        }
                    } else {
                        //update degree
                        tmp.addElement(String.valueOf(inter.size()));
                        //update children
                        iterTemp1 = union.entrySet().iterator();
                        while (iterTemp1.hasNext()) {
                            Map.Entry<String, String> entry1 = iterTemp1.next();
                            tmp.addElement(entry1.getKey() + Variables.uniChar + "true");
                        }
                    }
                    grammar.replace(node.getNodeName(), grammar.get(node.getNodeName()), tmp);
                }
            }
        }
    }

    private void updateNode(Node node , Map <String, Vector<String> > grammar ){


        int nbChildren = countNBChildren(node);

        if(nbChildren==0){//leaf node
            updateLeafNode(node,grammar);
        }else{//internal node
            updateInternalNode(node,grammar);
        }

    }

    //read grammar from ASTs
    private void readGrammarDepthFirstNew(Node node , Map <String, Vector<String> > grammar ) {
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
                    // loop for each child
                    for (int i = 0; i < nodeList.getLength(); ++i) {
                        if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                            readGrammarDepthFirstNew(nodeList.item(i), grammar);
                        }
                    }
                }
            }
        }catch (Exception e){
            System.out.println("Grammar error:" + e);
            e.printStackTrace();
        }

    }

    //create grammar from multiple folder
    public void createGrammar(File f, Map <String, Vector<String> > grammar) throws Exception {

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
                    readGrammarDepthFirstNew(doc.getDocumentElement(), grammar);
                }


            }else
            if (fi.isDirectory()) {
                //System.out.println("inside if check directory");
                createGrammar(fi,grammar);
            }
        }
    }

    /**
     * Create grammar from folder
     * @param path
     * @param grammar
     * @throws Exception
     */
    public void createGrammar(String path, Map <String, Vector<String> > grammar) throws Exception {
        createGrammar(new File(path), grammar);
    }


}
