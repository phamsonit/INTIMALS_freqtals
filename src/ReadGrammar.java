import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Vector;



import java.util.*;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;



public class ReadGrammar extends ReadXML {


    private static String readAttribute1(Node child, Map < String, Vector<String> > grammar){
        //add this child to grammar
        String mandatory="true";
        Set<String> tmp = new LinkedHashSet<>();
        NamedNodeMap nodeMapChild = child.getAttributes(); //get attributes
        for (int l = 0; l < nodeMapChild.getLength(); ++l) { //for each attribute
            Node n = nodeMapChild.item(l);
            switch (n.getNodeName()) {
                case "mandatory":
                    mandatory = String.valueOf(n.getNodeValue());
                    break;

                case "node": //a node has many values ???
                    //if the previous degree is 1..* ?
                    if (grammar.containsKey(child.getNodeName())) {
                        if (grammar.get(child.getNodeName()).elementAt(1).equals("1..*")) {
                            tmp.add(grammar.get(child.getNodeName()).elementAt(0));
                            tmp.add(grammar.get(child.getNodeName()).elementAt(1));
                        } else {
                            tmp.add("unordered");
                            tmp.add("1");
                        }
                    } else {
                        tmp.add("unordered");
                        tmp.add("1");
                    }
                    if (grammar.containsKey(child.getNodeName()))
                        tmp.addAll(grammar.get(child.getNodeName()).subList(2,
                                grammar.get(child.getNodeName()).size() - 1));
                    tmp.add(n.getNodeValue() + uniChar + "false");
                    grammar.put(child.getNodeName(), new Vector<>(tmp));

                    break;

                case "ordered-nodelist":
                    tmp.add("ordered");
                    tmp.add("1..*");
                    //temp.addElement(n.getNodeValue() + uniChar + "*");
                    if (grammar.containsKey(child.getNodeName()))
                        tmp.addAll(grammar.get(child.getNodeName()).subList(2,
                                grammar.get(child.getNodeName()).size() - 1));
                    tmp.add(n.getNodeValue() + uniChar + "false");
                    grammar.put(child.getNodeName(), new Vector<>(tmp));
                    break;

                case "unordered-nodelist":
                    tmp.add("unordered");
                    tmp.add("1..*");
                    if (grammar.containsKey(child.getNodeName()))
                        tmp.addAll(grammar.get(child.getNodeName()).subList(2,
                                grammar.get(child.getNodeName()).size() - 1));
                    tmp.add(n.getNodeValue() + uniChar + "false");
                    grammar.put(child.getNodeName(), new Vector<>(tmp));
                    break;

                case "simplevalue":
                    tmp.add("unordered");
                    tmp.add("1");
                    tmp.add(n.getNodeValue() + uniChar + "false");
                    grammar.put(child.getNodeName(), new Vector<>(tmp));
                    break;
            }



        }
        return mandatory;
    }


    private static void addAttribute(Node child,
                                     Map < String, Vector<String> > abstractNodes,
                                     Map < String, Vector<String> > grammar){
        NamedNodeMap nodeMap = child.getAttributes(); //get attributes
        Set<String> tmp = new LinkedHashSet<>();
        for (int j = 0; j < nodeMap.getLength(); ++j) { //for each attribute
            Node n = nodeMap.item(j);
            switch (n.getNodeName()) {
                case "node":
                    tmp.add("unordered");
                    tmp.add("1");
                    if (abstractNodes.containsKey(n.getNodeValue())) {
                        tmp.addAll(abstractNodes.get(n.getNodeValue()));
                        grammar.put(child.getNodeName(), new Vector<>(tmp));
                    } else {
                        tmp.add(n.getNodeValue()+uniChar+"false");
                        grammar.put(child.getNodeName(), new Vector<>(tmp));
                    }
                    break;

                case "ordered-nodelist":
                    tmp.add("ordered");
                    tmp.add("1..*");
                    if (abstractNodes.containsKey(n.getNodeValue())) {
                        tmp.addAll(abstractNodes.get(n.getNodeValue()));
                        grammar.put(child.getNodeName(), new Vector<>(tmp));
                    } else {
                        tmp.add(n.getNodeValue() + uniChar + "false");
                        grammar.put(child.getNodeName(), new Vector<>(tmp));
                    }
                    break;

                case "unordered-nodelist":
                    tmp.add("unordered");
                    tmp.add("1..*");
                    if (abstractNodes.containsKey(n.getNodeValue())) {
                        tmp.addAll(abstractNodes.get(n.getNodeValue()));
                        grammar.put(child.getNodeName(), new Vector<>(tmp));
                    } else {
                        tmp.add(n.getNodeValue() + uniChar + "false");
                        grammar.put(child.getNodeName(), new Vector<>(tmp));
                    }

                    break;
                case "simplevalue":
                    tmp.add("unordered");
                    tmp.add("1");
                    tmp.add(n.getNodeValue());
                    grammar.put(child.getNodeName(), new Vector<>(tmp));
                    break;
            }
        }

    }

    private static void updateAttribute(Node child,
                                        Map < String, Vector<String> > abstractNodes,
                                        Map < String, Vector<String> > grammar){
        //TODO: ??? a node is either node, node-list, keyword
        //System.out.println("update attribute "+child.getNodeName() );
        //check if old children == new children
        Set<String> oldChildren = new LinkedHashSet<>(grammar.get(child.getNodeName()));
        //System.out.println("old child "+oldChildren);

        NamedNodeMap nodeMap = child.getAttributes(); //get attributes
        Set<String> newChildren = new LinkedHashSet<>();

        for (int j = 0; j < nodeMap.getLength(); ++j) { //for each attribute
            Node n = nodeMap.item(j);
            if(abstractNodes.containsKey(n.getNodeValue())) {
                newChildren.addAll(abstractNodes.get(n.getNodeValue()));
                //System.out.println("new child " + newChildren);
            }else
                if(n.getNodeName().equals("node") ||
                   n.getNodeName().equals("ordered-nodelist") ||
                   n.getNodeName().equals("unordered-nodelist")
                   )
                   newChildren.add(n.getNodeValue()+uniChar+"false");
        }
        oldChildren.addAll(newChildren);
        //System.out.println("all child " + oldChildren);
        grammar.put(child.getNodeName(),new Vector<>(oldChildren));
    }


    //add a child of AST or Synthetic node to grammar
    private static void readAttribute(Node child,
                                        Map < String, Vector<String> > abstractNodes,
                                        Map < String, Vector<String> > grammar){
        if(grammar.containsKey(child.getNodeName())){
            updateAttribute(child,abstractNodes,grammar);
        }else{
            addAttribute(child,abstractNodes,grammar);
        }
    }

    private static String readMandatoryAttribute(Node child) {
        String mandatory="true";
        NamedNodeMap nodeMap = child.getAttributes(); //get attributes
        for (int j = 0; j < nodeMap.getLength(); ++j) { //for each attribute
            Node n = nodeMap.item(j);
            switch (n.getNodeName()) {
                case "mandatory":
                    mandatory = String.valueOf(n.getNodeValue());
                    break;
            }
        }
        return mandatory;
    }

    private static int findIndex(String node,Map < String, Vector<String> > grammar){
        Integer index = 0;
        Set<String> keySet = grammar.keySet();
        for (String s : keySet) {
            String[] ss = s.split(String.valueOf(uniChar));
            if (ss[0].equals(node)) {
                if (ss.length == 2)
                    index = Integer.valueOf(ss[1]) + 1;
                //else
                    //index = 1;
            }
        }
        if(index>1) return index;
        else return 1;
    }

    /**
     * find add abstract node in grammar
     * @param root
     * @return
     */
    private Map < String, Vector<String> > readAbstractNodes(Node root){
        Map<String,Vector<String> > abstractNodes = new LinkedHashMap<>();
        try{
            NodeList childrenNodes = root.getChildNodes();
            for(int i=0; i<childrenNodes.getLength();++i) { //for each abstract node
                if (childrenNodes.item(i).hasAttributes() &&
                    childrenNodes.item(i).hasChildNodes() &&
                    childrenNodes.item(i).getNodeType() == Node.ELEMENT_NODE)
                {
                    NamedNodeMap nodeMap = childrenNodes.item(i).getAttributes();
                    for (int j = 0; j < nodeMap.getLength(); ++j) { //check if a node is abstract
                        Node node = nodeMap.item(j);
                        if( (node.getNodeName().equals("abstract") &&
                                String.valueOf(node.getNodeValue()).equals("true")) ){

                            Vector<String> tmp1 = new Vector<>();
                            NodeList childrenList = childrenNodes.item(i).getChildNodes();
                            for(int k=0; k<childrenList.getLength(); ++k){//for each child of Abstract
                                if(childrenList.item(k).getNodeType() == Node.ELEMENT_NODE){
                                    if(!abstractNodes.containsKey(childrenNodes.item(i).getNodeName())) {
                                        tmp1.add(childrenList.item(k).getNodeName()+uniChar+"false");
                                        abstractNodes.put(childrenNodes.item(i).getNodeName(), tmp1);
                                    }else {
                                        abstractNodes.get(childrenNodes.item(i).getNodeName()).add(
                                                childrenList.item(k).getNodeName()+uniChar+"false");
                                    }
                                }
                            }
                        }
                    }
                }

            }

        }catch (Exception e){System.out.println("read abstract nodes error "+e);}
        return abstractNodes;
    }


    /**
     * find all synthetic node in grammar
     * @param root
     * @param abstractNodes
     * @param grammar
     * @return
     */
    private static Map < String, Vector<String> > readSyntheticNodes(Node root,
                                                              Map < String, Vector<String> > abstractNodes,
                                                               Map < String, Vector<String> > grammar){
        //find abstract/synthetic nodes
        Map<String,Vector<String> > syntheticNodes = new LinkedHashMap<>();

        NodeList childrenNodes = root.getChildNodes();
        for(int i=0; i<childrenNodes.getLength();++i) { //for each node
            if (childrenNodes.item(i).hasAttributes() &&
                    childrenNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {

                NamedNodeMap nodeMap = childrenNodes.item(i).getAttributes();
                for (int j = 0; j < nodeMap.getLength(); ++j) { //check if a node i is synthetic
                    Node node = nodeMap.item(j);
                    if( (node.getNodeName().equals("synthetic") && String.valueOf(node.getNodeValue()).equals("true")) )
                    {
                        //find all children of synthetic node i
                        Set<String> syntheticChildren = new HashSet<>();
                        NodeList childrenList = childrenNodes.item(i).getChildNodes();
                        for(int k=0; k<childrenList.getLength(); ++k){//for each child of Synthetic node
                            if(childrenList.item(k).getNodeType() == Node.ELEMENT_NODE) {
                                String mandatory = readMandatoryAttribute(childrenList.item(k));
                                syntheticChildren.add(childrenList.item(k).getNodeName()+uniChar+mandatory);
                                readAttribute(childrenList.item(k),abstractNodes,grammar);
                            }
                        }

                        if(syntheticNodes.containsKey(childrenNodes.item(i).getNodeName())) {
                            //find the index of rule, //create new synthetic rule
                            int index = findIndex(childrenNodes.item(i).getNodeName(),syntheticNodes);
                            syntheticNodes.put(childrenNodes.item(i).getNodeName()+uniChar+String.valueOf(index),
                                                new Vector<>(syntheticChildren));
                        }else
                            syntheticNodes.put(childrenNodes.item(i).getNodeName(),new Vector<>(syntheticChildren));
                    }
                }
            }

        }
        return syntheticNodes;
    }

    private static Boolean checkSyntheticNode(Node node){
        Boolean synthetic = false;
        NodeList childrenNodes = node.getChildNodes();
        for(int i=0; i<childrenNodes.getLength();++i){
            if (childrenNodes.item(i).hasAttributes() &&
                    childrenNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                String[] adhoc = childrenNodes.item(i).getNodeName().split("_");
                if (adhoc[0].equals("Adhoc")) {
                    synthetic =true;
                }
            }
        }
        return synthetic;
    }

    private static void readSimpleNode(Node node,
                                      Map < String, Vector<String> > abstractNodes,
                                      Map<String,Vector<String> > grammar)
    {
        Set<String> childrenListTmp = new LinkedHashSet<>(); //find all its children
        NodeList childrenList = node.getChildNodes();//create grammar for each child

        for (int i = 0; i < childrenList.getLength(); ++i) {
            if (childrenList.item(i).hasAttributes() &&
                    childrenList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                String mandatory = readMandatoryAttribute(childrenList.item(i));
                String currentChildLabel = childrenList.item(i).getNodeName();
                childrenListTmp.add(currentChildLabel + uniChar + String.valueOf(mandatory));
                readAttribute(childrenList.item(i),abstractNodes,grammar);
            }
        }
        //add the current node to grammar
        if(!childrenListTmp.isEmpty()){
            Vector<String> childrenListTmpVector = new Vector<>(childrenListTmp);
            childrenListTmpVector.add(0, "unordered");
            childrenListTmpVector.add(1, String.valueOf(childrenListTmpVector.size() - 1));
            //if this node exists in grammar then increase index
            if (grammar.containsKey(node.getNodeName())) {
                int index = findIndex(node.getNodeName(),grammar);
                grammar.put(node.getNodeName() + uniChar + String.valueOf(index), childrenListTmpVector);
            } else
                grammar.put(node.getNodeName(), childrenListTmpVector);
        }

    }

    private static Map<String, Vector<String>> getRules(String label, Map<String, Vector<String>> maps){

        //System.out.println("syntheticLabel "+label);

        Map<String,Vector<String> > rules = new LinkedHashMap<>();
        Set<String> keyList = maps.keySet();
        for(String s:keyList){
            String[] ss = s.split(String.valueOf(uniChar));
            if(ss[0].equals(label))
                rules.put(s,maps.get(s));
        }

        //printGrammar(rules);
        return rules;

    }

    private static void combineSyntheticNodes (Map<String, Map<String, Vector<String>>> syntheticChildren){

        Set<Vector<String>> combinations = new LinkedHashSet<>();
        int size = syntheticChildren.size();
        //for each synthetic rule we have some sub-rules (each rule is a new vector>
        //if having 1 vector --> oK
        //if having more than 1 vector

        /*
        public static void recursive_vector_printer(int d, String str) {
          if (d == vec.length) {
              System.out.println(str.substring(0, str.length()-1));
              return;
              }

              for (int k = 0; k < vec[d].length; k++) {
              recursive_vector_printer(d + 1, str + vec[d][k] + ",");
              }

              return;
        }

         */
            Iterator<Map.Entry<String, Map<String, Vector<String>> >> iter1 = syntheticChildren.entrySet().iterator();
            Map.Entry<String, Map<String, Vector<String>> > entry = iter1.next();

            //for each rule of synthetic node create a rule in grammar
            Iterator<Map.Entry<String, Vector<String>> > iter2 = entry.getValue().entrySet().iterator();
            while(iter2.hasNext()){
                Vector<String> allChildren = new Vector<>();
                Map.Entry<String, Vector<String> > entry2 = iter2.next();
                System.out.println(entry2.getKey()+" "+entry2.getValue());
                allChildren.addAll(entry2.getValue());
            }

    }

    private static void readSpecialNode(Node node,
                                       Map<String,Vector<String> > syntheticNodes,
                                       Map<String,Vector<String> > grammar){
        //get the set of children
        NodeList childrenNodes = node.getChildNodes();
        //find normal children
        //find set of rules of each synthetic node://example: WhiteStatement has 2 synthetic nodes
        Vector<String> normalChildren = new Vector<>();
        Map<String, Map<String, Vector<String>>> syntheticChildren = new LinkedHashMap<>();
        for(int i=0; i<childrenNodes.getLength(); ++i){
            if (childrenNodes.item(i).hasAttributes() &&
                    childrenNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                String[] adhoc = childrenNodes.item(i).getNodeName().split("_");
                if (adhoc[0].equals("Adhoc")) { //synthetic child
                    String syntheticLabel="";

                    for(int j=1;j<adhoc.length-1;++j)
                        syntheticLabel = syntheticLabel+adhoc[j]+"_";
                    syntheticLabel = syntheticLabel+ adhoc[adhoc.length-1];

                    syntheticChildren.put(childrenNodes.item(i).getNodeName(),getRules(syntheticLabel,syntheticNodes));
                }
                else{//normal child
                    String mandatory = readMandatoryAttribute(childrenNodes.item(i));
                    normalChildren.add(childrenNodes.item(i).getNodeName()+uniChar+mandatory);
                }
            }
        }

        //create all cases of synthetic nodes,
        //i.e, node A has 3 synthetic child, and
        //synthetic child 1 has 2 cases
        //synthetic child 2 has 3 cases
        //synthetic child 3 has 4 cases
        //--> total how many combinations ? --> for each case create one rule
        //how to know mandatory of these children


        // combine allChildren = normalChild + each item in syntheticChild
        int size = syntheticChildren.size();
        int index=0;
        if (size == 1){//node has only one synthetic child
            Iterator<Map.Entry<String, Map<String, Vector<String>> >> iter1 = syntheticChildren.entrySet().iterator();
            while (iter1.hasNext()) {
                Map.Entry<String, Map<String, Vector<String>> > entry = iter1.next();
                //for each rule of synthetic node create a rule in grammar
                Iterator<Map.Entry<String, Vector<String>> > iter2 = entry.getValue().entrySet().iterator();
                while(iter2.hasNext()){
                    Vector<String> allChildren = new Vector<>(normalChildren);
                    Map.Entry<String, Vector<String> > entry2 = iter2.next();
                    //System.out.println(entry2.getKey()+" "+entry2.getValue());
                    allChildren.addAll(entry2.getValue());
                    //create one rule in grammar
                    allChildren.add(0,"unordered");
                    allChildren.add(1,String.valueOf(allChildren.size()-1) );
                    if(index==0) {
                        grammar.put(node.getNodeName(),allChildren);
                    }
                    else {
                        grammar.put(node.getNodeName()+uniChar+String.valueOf(index), allChildren);
                    }
                    index++;
                }
            }
        }
        else{//node has many synthetic child


        }

    }



    /**
     * find all AST node in grammar
     * @param root
     * @param abstractNodes
     * @param syntheticNodes
     * @param grammar
     */
    private static void readASTNodes(Node root,
                                     Map<String,Vector<String> > abstractNodes,
                                     Map<String,Vector<String> > syntheticNodes,
                                     Map<String,Vector<String> > grammar){

        NodeList childrenNodes = root.getChildNodes();
        for(int t=0; t<childrenNodes.getLength(); ++t) {//for each child (AST node)
            if (!childrenNodes.item(t).hasAttributes() && //if it is not abstract and not synthetic node
                    childrenNodes.item(t).hasChildNodes() && // it has children
                    childrenNodes.item(t).getNodeType() == Node.ELEMENT_NODE) {

                if(checkSyntheticNode(childrenNodes.item(t))){
                    readSpecialNode(childrenNodes.item(t),syntheticNodes,grammar);
                }else{
                    readSimpleNode(childrenNodes.item(t),abstractNodes,grammar);
                }
            }
        }
    }


    //create grammar from file
    public void readGrammar(String path, Map < String, Vector<String> > grammar) {
        try {

            //for each file in folder create one tree
            File fXmlFile = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            Node root = doc.getDocumentElement();
            Map< String,Vector<String> > abstractNodes = readAbstractNodes(root);
            Map< String,Vector<String> > syntheticNodes = readSyntheticNodes(root,abstractNodes,grammar);
            //printGrammar(syntheticNodes);
            readASTNodes(root,abstractNodes,syntheticNodes,grammar);

        } catch (Exception e) { System.out.println("read grammar file error "+e);}
    }

    public static void printGrammar(Map<String,Vector<String> > grammar) {

        Iterator<Map.Entry<String, Vector<String>>> iter1 = grammar.entrySet().iterator();
        while (iter1.hasNext()) {
            Map.Entry<String, Vector<String>> entry = iter1.next();
            System.out.println(entry.getKey() + " " + entry.getValue());
        }
    }

    //create grammar from file
    public void readGrammarOld(String path, Map < String, Vector<String> > grammar) {
        try {

            //for each file in folder create one tree
            File fXmlFile = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            Node root = doc.getDocumentElement();
            NodeList childrenNodes = root.getChildNodes();

            //find abstract/synthetic nodes and all attribute of synthetic nodes
            Map<String,Vector<String> > abstractNodes = readAbstractNodes(root);

            //find all AST nodes and attributes
            for(int t=0; t<childrenNodes.getLength(); ++t){

                if(!childrenNodes.item(t).hasAttributes() && //if it is not abstract and synthetic node
                        childrenNodes.item(t).hasChildNodes() && // node has children
                        childrenNodes.item(t).getNodeType() == Node.ELEMENT_NODE )
                {
                    Set<String> childrenListTmp = new LinkedHashSet<>();

                    //create grammar for each child
                    NodeList childrenList = childrenNodes.item(t).getChildNodes();

                    /*
                    if(grammar.containsKey(childrenNodes.item(t).getNodeName())) {
                        updateChildrenList(childrenNodes.item(t),grammar,childrenListTmp);
                    }
                    else{
                        childrenListTmp.add("unordered");
                        childrenListTmp.add(String.valueOf(countChildren(childrenNodes.item(t))));
                    }
                    */

                    for (int i = 0; i < childrenList.getLength(); ++i) {
                        if (childrenList.item(i).hasAttributes() &&
                                childrenList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                            String currentChildLabel;
                            Vector<String> currentChildList;// = new Vector<>();
                            String mandatory="true"; //edited in java: optional="true" --> mandatory="false"
                            //check abstract/synthetic attributes; don't add these nodes to grammar
                            //if this child is Adhoc replace this node with its children in abstractNodes
                            //else add this child to grammar
                            String[] adhoc = childrenList.item(i).getNodeName().split("_");
                            if (adhoc[0].equals("Adhoc")) {
                                String abstractChildLabel = adhoc[1] + "_" + adhoc[2];
                                //find current children label
                                currentChildList = abstractNodes.get(abstractChildLabel);
                                for(int k=0; k< currentChildList.size();++k){
                                    childrenListTmp.add(currentChildList.elementAt(k)+uniChar+"false");
                                }


                            } else {

                                currentChildLabel = childrenList.item(i).getNodeName();
                                //add attributes (all children of AST node) to grammar
                                //if an attribute exists in the grammar --> update it in grammar
                                //else add a new node in grammar
                                //List<String> tmp = new LinkedList<>();
                                Set<String> tmp = new LinkedHashSet<>();
                                NamedNodeMap nodeMap = childrenList.item(i).getAttributes(); //get attributes
                                for (int j = 0; j < nodeMap.getLength(); ++j) { //for each attribute
                                    Node n = nodeMap.item(j);
                                    switch (n.getNodeName()) {
                                        case "mandatory":
                                            mandatory = String.valueOf(n.getNodeValue());
                                            break;

                                        case "node": //a node has many values ???
                                            //if the previous degree is 1..* ?
                                            if (grammar.containsKey(childrenList.item(i).getNodeName()))
                                            {
                                                if(grammar.get(childrenList.item(i).getNodeName()).elementAt(1).equals("1..*")) {
                                                    tmp.add(grammar.get(childrenList.item(i).getNodeName()).elementAt(0));
                                                    tmp.add(grammar.get(childrenList.item(i).getNodeName()).elementAt(1));
                                                }else{
                                                    tmp.add("unordered");
                                                    tmp.add("1");
                                                }
                                            }
                                            else{
                                                tmp.add("unordered");
                                                tmp.add("1");
                                            }

                                            if (abstractNodes.containsKey(n.getNodeValue())) {
                                                tmp.addAll(abstractNodes.get(n.getNodeValue()));
                                                grammar.put(childrenList.item(i).getNodeName(), new Vector<>(tmp));
                                            } else {
                                                if (grammar.containsKey(childrenList.item(i).getNodeName()))
                                                    tmp.addAll(grammar.get(childrenList.item(i).getNodeName()).subList(2,
                                                            grammar.get(childrenList.item(i).getNodeName()).size()-1));
                                                tmp.add(n.getNodeValue()+ uniChar + "false");
                                                grammar.put(childrenList.item(i).getNodeName(), new Vector<>(tmp));
                                            }
                                            break;

                                        case "ordered-nodelist":
                                            tmp.add("ordered");
                                            tmp.add("1..*");

                                            if (abstractNodes.containsKey(n.getNodeValue())) {
                                                tmp.addAll(abstractNodes.get(n.getNodeValue()));
                                                grammar.put(childrenList.item(i).getNodeName(), new Vector<>(tmp));
                                            } else {
                                                //temp.addElement(n.getNodeValue() + uniChar + "*");
                                                if (grammar.containsKey(childrenList.item(i).getNodeName()))
                                                    tmp.addAll(grammar.get(childrenList.item(i).getNodeName()).subList(2,
                                                            grammar.get(childrenList.item(i).getNodeName()).size()-1));
                                                tmp.add(n.getNodeValue()+ uniChar + "false");
                                                grammar.put(childrenList.item(i).getNodeName(), new Vector<>(tmp));
                                            }

                                            break;

                                        case "unordered-nodelist":
                                            tmp.add("unordered");
                                            tmp.add("1..*");

                                            if (abstractNodes.containsKey(n.getNodeValue())) {
                                                tmp.addAll(abstractNodes.get(n.getNodeValue()));
                                                grammar.put(childrenList.item(i).getNodeName(), new Vector<>(tmp));
                                            } else {
                                                if (grammar.containsKey(childrenList.item(i).getNodeName()))
                                                    tmp.addAll(grammar.get(childrenList.item(i).getNodeName()).subList(2,
                                                            grammar.get(childrenList.item(i).getNodeName()).size()-1));
                                                tmp.add(n.getNodeValue()+ uniChar + "false");
                                                grammar.put(childrenList.item(i).getNodeName(), new Vector<>(tmp));
                                            }

                                            break;
                                        case "simplevalue":
                                            tmp.add("unordered");
                                            tmp.add("1");
                                            tmp.add(n.getNodeValue() + uniChar + "false");
                                            grammar.put(childrenList.item(i).getNodeName(), new Vector<>(tmp));
                                            break;
                                    }
                                }
                                if (grammar.containsKey(childrenNodes.item(t).getNodeName())) {
                                    childrenListTmp.addAll(grammar.get(childrenNodes.item(t).getNodeName()));
                                }
                                childrenListTmp.add(currentChildLabel + uniChar + String.valueOf(mandatory));
                            }
                        }
                    }
                    //add the current node to grammar
                    grammar.put(childrenNodes.item(t).getNodeName(),new Vector<>(childrenListTmp));
                }
            }
        } catch (Exception e) {
            System.out.println("read grammar file error "+e);
        }
    }


    }//end of class
