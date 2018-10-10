package be.intimals.freqt.input;

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

    //read tree by breadth first traversal
    private void readNode(Node node, Map<String, Vector<String>> grammar) {
        try {
            // make sure it's element node.
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (node.hasAttributes()) {
                    NamedNodeMap nodeMap1 = node.getAttributes();
                    Node n1 = nodeMap1.item(0);
                    if (n1.getNodeName().equals("abstract") && n1.getNodeValue().equals("false")) {

                        if (node.hasChildNodes()) {
                            NodeList childrenList = node.getChildNodes();
                            int nbChildren = countChildren(node);
                            Vector<String> tmp = new Vector<>();

                            if (grammar.containsKey(node.getNodeName())) {
                                //get old children
                                //find grammar of this current node
                                Vector<String> oldGrammar = grammar.get(node.getNodeName());
                                String oldOrdered = oldGrammar.elementAt(0);
                                String oldDegree = oldGrammar.elementAt(1);

                                Map<String, String> oldChildren = new LinkedHashMap<>();
                                for (int i = 2; i < oldGrammar.size(); ++i) {
                                    String[] temp = oldGrammar.elementAt(i).split(String.valueOf(uniChar));
                                    oldChildren.put(temp[0], temp[1]);
                                }

                                //get new children
                                Map<String, String> newChildren = new LinkedHashMap<>();
                                for (int i = 0; i < childrenList.getLength(); ++i) {
                                    if (childrenList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                                        newChildren.put(childrenList.item(i).getNodeName(), " ? ");
                                    }

                                }

                                tmp.addElement("false");
                                tmp.addElement("1..*");

                                Iterator<Map.Entry<String, String>> iter1 = newChildren.entrySet().iterator();
                                while (iter1.hasNext()) {
                                    Map.Entry<String, String> entry = iter1.next();
                                    oldChildren.put(entry.getKey(), "*");
                                    //totalNode += Integer.valueOf(entry.getKey());
                                }

                                Iterator<Map.Entry<String, String>> iter = oldChildren.entrySet().iterator();
                                while (iter.hasNext()) {
                                    Map.Entry<String, String> entry = iter.next();
                                    tmp.addElement(entry.getKey() + uniChar + "*");
                                    //totalNode += Integer.valueOf(entry.getKey());
                                }

                                grammar.put(node.getNodeName(), tmp);

                            } else {//add new node to grammar
                                Map<String, String> childrenTemp = new LinkedHashMap<>();
                                for (int i = 0; i < childrenList.getLength(); ++i) {
                                    if (childrenList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                                        boolean mandatory = true;
                                        String child = "";
                                        Vector<String> temp = new Vector<>();

                                        if (childrenList.item(i).hasAttributes()) {
                                            NamedNodeMap nodeMap = childrenList.item(i).getAttributes();
                                            //System.out.println(childrenList.item(i).getNodeName()+" has attribute "+nodeMap.getLength());
                                            for (int j = 0; j < nodeMap.getLength(); ++j) { //for each attribute
                                                Node n = nodeMap.item(j);
                                                switch (n.getNodeName()) {
                                                    case "mandatory":
                                                        if (n.getNodeValue().equals("false"))
                                                            mandatory = false;
                                                        break;

                                                    case "node":
                                                        temp.addElement("true");
                                                        temp.addElement("1");
                                                        temp.addElement(n.getNodeValue() + uniChar + "1");
                                                        grammar.put(childrenList.item(i).getNodeName(), temp);
                                                        break;
                                                    case "nodelist":
                                                        temp.addElement("false");
                                                        temp.addElement("1..*");
                                                        temp.addElement(n.getNodeValue() + uniChar + "*");
                                                        grammar.put(childrenList.item(i).getNodeName(), temp);

                                                        break;
                                                    case "simplevalue":
                                                        temp.addElement("true");
                                                        temp.addElement("1");
                                                        temp.addElement(n.getNodeValue() + uniChar + "1");
                                                        grammar.put(childrenList.item(i).getNodeName(), temp);
                                                        break;
                                                }
                                            }
                                        }
                                        childrenTemp.put(childrenList.item(i).getNodeName(), String.valueOf(mandatory));
                                    }
                                }

                                //System.out.println(childrenTemp);

                                tmp.addElement("true");
                                tmp.addElement(String.valueOf(nbChildren));

                                Iterator<Map.Entry<String, String>> iter = childrenTemp.entrySet().iterator();
                                while (iter.hasNext()) {
                                    Map.Entry<String, String> entry = iter.next();
                                    tmp.addElement(entry.getKey() + uniChar + entry.getValue());
                                }

                                //System.out.println(tmp);

                                grammar.put(node.getNodeName(), tmp);

                            }
                            //System.out.println(node.getNodeName()+tmp);
                            for (int i = 0; i < childrenList.getLength(); ++i) {
                                readNode(childrenList.item(i), grammar);
                            }
                        } else {
                            /////////else////
                            Vector<String> tmp = new Vector<>();
                            tmp.addElement("true");
                            tmp.addElement(String.valueOf("1"));
                            tmp.addElement(String.valueOf("*" + uniChar + "*"));
                            grammar.put(node.getNodeName(), tmp);
                            //////////////

                        }

                    }
                }

            }

        } catch (Exception e) {
            System.out.println("read node error");
        }


    }

    //create grammar from file
    public void readGrammarOld(String path, Map<String, Vector<String>> grammar) {
        try {
            System.out.print("read grammar: ");
            //for each file in folder create one tree
            File fXmlFile = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();


            //recur creating list of nodes
            readNode(doc.getDocumentElement(), grammar);

            System.out.println("finished");


        } catch (Exception e) {
            System.out.println("read grammar file error");
        }

    }


    //create grammar from file
    public void readGrammar(String path, Map<String, Vector<String>> grammar) {
        try {

            //for each file in folder create one tree
            File fXmlFile = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            Node root = doc.getDocumentElement();
            NodeList childrenNodes = root.getChildNodes();

            for (int t = 0; t < childrenNodes.getLength(); ++t) {
                if (!childrenNodes.item(t).hasAttributes() &&
                        (childrenNodes.item(t).getNodeType() == Node.ELEMENT_NODE)) //abstract = "true"
                {
                    NodeList childrenList = childrenNodes.item(t).getChildNodes();
                    Vector<String> childrenListTemp = new Vector<>();

                    childrenListTemp.addElement("unordered");
                    childrenListTemp.addElement(String.valueOf(countChildren(childrenNodes.item(t))));

                    for (int i = 0; i < childrenList.getLength(); ++i) {
                        if ((childrenList.item(i).getNodeType() == Node.ELEMENT_NODE) &&
                                (childrenList.item(i).hasAttributes())) {

                            //if this node exist in grammar --> update grammar
                            NamedNodeMap nodeMap = childrenList.item(i).getAttributes();
                            String mandatory = "false";
                            Vector<String> temp = new Vector<>();
                            //System.out.println(childrenList.item(i).getNodeName()+" has attribute "+nodeMap.getLength());
                            for (int j = 0; j < nodeMap.getLength(); ++j) { //for each attribute
                                Node n = nodeMap.item(j);
                                switch (n.getNodeName()) {
                                    case "mandatory":
                                        mandatory = String.valueOf(n.getNodeValue());
                                        break;

                                    case "node":
                                        temp.addElement("unordered");
                                        temp.addElement("1");
                                        temp.addElement(n.getNodeValue() + uniChar + "true");
                                        grammar.put(childrenList.item(i).getNodeName(), temp);
                                        break;

                                    case "ordered-nodelist":
                                        temp.addElement("ordered");
                                        temp.addElement("1..*");
                                        temp.addElement(n.getNodeValue() + uniChar + "*");
                                        grammar.put(childrenList.item(i).getNodeName(), temp);
                                        break;

                                    case "unordered-nodelist":
                                        temp.addElement("unordered");
                                        temp.addElement("1..*");
                                        temp.addElement(n.getNodeValue() + uniChar + "*");
                                        grammar.put(childrenList.item(i).getNodeName(), temp);

                                        break;
                                    case "simplevalue":
                                        temp.addElement("unordered");
                                        temp.addElement("1");
                                        temp.addElement(n.getNodeValue() + uniChar + "true");
                                        grammar.put(childrenList.item(i).getNodeName(), temp);
                                        break;
                                }
                            }
                            childrenListTemp.addElement(childrenList.item(i).getNodeName() + uniChar + String.valueOf(mandatory));
                        }
                    }
                    grammar.put(childrenNodes.item(t).getNodeName(), childrenListTemp);
                }

            }
        } catch (Exception e) {
            System.out.println("read grammar file error");
        }
    }


}
