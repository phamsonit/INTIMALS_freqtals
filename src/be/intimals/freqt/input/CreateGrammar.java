package be.intimals.freqt.input;

import java.util.*;
import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

/**
 * create grammar for ASTs
 */
public class CreateGrammar extends ReadXML {

    /**
     * read grammar from new ASTS
     *
     * @param node
     * @param grammar
     */
    private void readGrammarDepthFirstNew(Node node, Map<String, Vector<String>> grammar) {
        try {
            // make sure it's element node.
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                //get list of children
                int nbChildren = countChildren(node);
                NodeList childrenList = node.getChildNodes();

                Vector<String> tmp = new Vector<>();

                if (grammar.containsKey(node.getNodeName())) {

                    //find grammar of this current node
                    Vector<String> oldGrammar = grammar.get(node.getNodeName());
                    String oldOrdered = oldGrammar.elementAt(0);
                    String oldDegree = oldGrammar.elementAt(1);


                    Map<String, String> oldChildren = new LinkedHashMap<>();
                    for (int i = 2; i < oldGrammar.size(); ++i) {
                        String[] temp = oldGrammar.elementAt(i).split(String.valueOf(uniChar));
                        oldChildren.put(temp[0], temp[1]);
                    }

                    switch (oldOrdered) {
                        case "unordered":
                            //System.out.println("");
                            //if leaf node ...
                            //else if internal node
                            if (nbChildren == 0) {//leaf node
                                tmp.addElement("unordered");
                                tmp.addElement("1");
                                //collect all leaf nodes
                                oldChildren.put(node.getTextContent().trim(), "false");
                                Iterator<Map.Entry<String, String>> iterTemp = oldChildren.entrySet().iterator();
                                while (iterTemp.hasNext()) {
                                    Map.Entry<String, String> entry = iterTemp.next();
                                    tmp.addElement(entry.getKey() + uniChar + "false");
                                }
                                //update grammar
                                grammar.replace(node.getNodeName(), grammar.get(node.getNodeName()), tmp);
                            } else {
                                //find new children = newChildren
                                Map<String, String> newChildren = new LinkedHashMap<>();
                                int childNumber = 1;
                                boolean childRepeated = false;
                                for (int i = 0; i < childrenList.getLength(); ++i) {
                                    if (childrenList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                                        //find optional node: if it has no value
                                        //i.e. <elseStatement ColumnNr="8" EndLineNr="585" ID="2044" LineNr="580"/>
                                        boolean mandatory = true;
                                        if (newChildren.containsKey(childrenList.item(i).getNodeName())) {
                                            newChildren.replace(childrenList.item(i).getNodeName(),
                                                    newChildren.get(childrenList.item(i).getNodeName()),
                                                    String.valueOf(mandatory));
                                            childRepeated = true;
                                        } else {
                                            newChildren.put(childrenList.item(i).getNodeName(), String.valueOf(mandatory));
                                            ++childNumber;
                                        }
                                    }
                                }

                                Iterator<Map.Entry<String, String>> iterTemp1;
                                Iterator<Map.Entry<String, String>> iterTemp2;

                                //if new node has repeated child--> update grammar [unordered, 1..*,list of children]
                                if (childRepeated) {
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
                                        tmp.addElement(entry.getKey() + uniChar + "*");
                                    }
                                    grammar.replace(node.getNodeName(), grammar.get(node.getNodeName()), tmp);
                                } else {
                                    if (nbChildren == 1 && oldDegree.equals("1")) {
                                        //System.out.println(node.getNodeName()+" internal node has only 1 child");
                                        tmp.addElement("unordered");
                                        tmp.addElement("1");
                                        Iterator<Map.Entry<String, String>> iter = newChildren.entrySet().iterator();
                                        while (iter.hasNext()) {
                                            Map.Entry<String, String> entry = iter.next();
                                            oldChildren.put(entry.getKey(), entry.getValue());
                                        }

                                        Iterator<Map.Entry<String, String>> iterTemp = oldChildren.entrySet().iterator();
                                        while (iterTemp.hasNext()) {
                                            Map.Entry<String, String> entry = iterTemp.next();
                                            tmp.addElement(entry.getKey() + uniChar + "false");
                                        }
                                        grammar.replace(node.getNodeName(), grammar.get(node.getNodeName()), tmp);
                                    } else {
                                        if (nbChildren == 1 && oldDegree.equals("1..*")) {
                                            //System.out.println(node.getNodeName()+" internal node has only 1 child");
                                            tmp.addElement("unordered");
                                            tmp.addElement("1..*");
                                            Iterator<Map.Entry<String, String>> iter = newChildren.entrySet().iterator();
                                            while (iter.hasNext()) {
                                                Map.Entry<String, String> entry = iter.next();
                                                oldChildren.put(entry.getKey(), entry.getValue());
                                            }

                                            Iterator<Map.Entry<String, String>> iterTemp = oldChildren.entrySet().iterator();
                                            while (iterTemp.hasNext()) {
                                                Map.Entry<String, String> entry = iterTemp.next();
                                                tmp.addElement(entry.getKey() + uniChar + "false");
                                            }
                                            grammar.replace(node.getNodeName(), grammar.get(node.getNodeName()), tmp);
                                        } else {

                                            //else --> update grammar [unordered, N..M, list of children]
                                            //calculate intersection
                                            iterTemp2 = oldChildren.entrySet().iterator();
                                            Map<String, String> inter = new LinkedHashMap<>();
                                            while (iterTemp2.hasNext()) {
                                                Map.Entry<String, String> entry2 = iterTemp2.next();
                                                if (newChildren.containsKey(entry2.getKey()) &&
                                                        entry2.getValue().equals("true"))
                                                    inter.put(entry2.getKey(), entry2.getValue());
                                            }

                                            //calculate union
                                            iterTemp1 = newChildren.entrySet().iterator();
                                            iterTemp2 = oldChildren.entrySet().iterator();
                                            Map<String, String> union = new LinkedHashMap<>();

                                            while (iterTemp2.hasNext()) {
                                                Map.Entry<String, String> entry2 = iterTemp2.next();
                                                union.put(entry2.getKey(), entry2.getValue());
                                            }
                                            while (iterTemp1.hasNext()) {
                                                Map.Entry<String, String> entry1 = iterTemp1.next();
                                                union.put(entry1.getKey(), entry1.getValue());
                                            }

                                            tmp.addElement("unordered");
                                            if (inter.size() != union.size()) {
                                                tmp.addElement(String.valueOf(inter.size() + ".." + union.size()));
                                                iterTemp1 = union.entrySet().iterator();
                                                while (iterTemp1.hasNext()) {
                                                    Map.Entry<String, String> entry1 = iterTemp1.next();
                                                    if (inter.containsKey(entry1.getKey()))
                                                        tmp.addElement(entry1.getKey() + uniChar + "true");
                                                    else
                                                        tmp.addElement(entry1.getKey() + uniChar + "false");
                                                }
                                            } else {
                                                tmp.addElement(String.valueOf(inter.size()));
                                                iterTemp1 = union.entrySet().iterator();
                                                while (iterTemp1.hasNext()) {
                                                    Map.Entry<String, String> entry1 = iterTemp1.next();
                                                    tmp.addElement(entry1.getKey() + uniChar + "true");
                                                }
                                            }
                                            grammar.replace(node.getNodeName(), grammar.get(node.getNodeName()), tmp);
                                        }
                                    }
                                }
                            }
                            break;

                        case "ordered":
                            System.out.println("similar to unordered");
                            break;

                    }
                } else {//add new node to grammar
                    if (nbChildren == 0) {//add leaf node
                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            tmp.addElement("unordered");
                            tmp.addElement("1");
                            tmp.addElement(node.getTextContent().trim() + uniChar + "false");
                            //tmp.addElement("*" + uniChar + "*");
                        }
                    } else { //add internal node
                        //1 - find children
                        Map<String, String> childrenTemp = new LinkedHashMap<>();
                        int childNumber = 1;
                        boolean childRepeated = false;
                        //find children of this node
                        for (int i = 0; i < childrenList.getLength(); ++i) {
                            if (childrenList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                                //find optional node: if it has no value
                                //i.e. <elseStatement ColumnNr="8" EndLineNr="585" ID="2044" LineNr="580"/>
                                //==> can not apply this to new grammar which has no nodes include empty
                                boolean mandatory = true;
                                if (!childrenList.item(i).hasChildNodes()) {
                                    System.out.println(childrenList.item(i).getNodeName() + " : optional");
                                    mandatory = false;
                                }
                                //============================
                                if (childrenTemp.containsKey(childrenList.item(i).getNodeName())) {
                                    childrenTemp.replace(childrenList.item(i).getNodeName(),
                                            childrenTemp.get(childrenList.item(i).getNodeName()),
                                            childrenTemp.get(childrenList.item(i).getNodeName()));
                                    childRepeated = true;
                                } else {
                                    childrenTemp.put(childrenList.item(i).getNodeName(), String.valueOf(mandatory));
                                    ++childNumber;
                                    //childrenTemp.put(childrenList.item(i).getNodeName(), Double.valueOf("1"));
                                }
                            }
                        }
                        if (childRepeated) {
                            tmp.addElement("unordered");
                            tmp.addElement("1..*");
                            Iterator<Map.Entry<String, String>> iter = childrenTemp.entrySet().iterator();
                            while (iter.hasNext()) {
                                Map.Entry<String, String> entry = iter.next();
                                tmp.addElement(entry.getKey() + uniChar + "*");
                                //tmp.addElement("*" + uniChar + "*");
                            }
                        } else {
                            tmp.addElement("unordered");
                            tmp.addElement(String.valueOf(childNumber - 1));
                            Iterator<Map.Entry<String, String>> iter = childrenTemp.entrySet().iterator();
                            while (iter.hasNext()) {
                                Map.Entry<String, String> entry = iter.next();
                                tmp.addElement(entry.getKey() + uniChar + entry.getValue());
                            }
                        }
                    }
                    //System.out.println(node.getNodeName()+" "+tmp);
                    grammar.put(node.getNodeName(), tmp);
                }

                if (node.hasChildNodes()) {
                    //get list of children
                    NodeList nodeList = node.getChildNodes();
                    // loop for each child
                    for (int i = 0; i < nodeList.getLength(); ++i) {
                        if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                            //recur other children
                            readGrammarDepthFirstNew(nodeList.item(i), grammar);
                        }
                    }

                }
            }

        } catch (Exception e) {
            System.out.println(e);
        }

    }

    /**
     * Create grammar from folder
     *
     * @param f
     * @param grammar
     * @throws Exception
     */
    public void createGrammar(File f, Map<String, Vector<String>> grammar) throws Exception {

        File[] subdir = f.listFiles();
        Arrays.sort(subdir);

        for (File fi : subdir) {
            if (fi.isFile() && fi.getName().charAt(0) != '.') {
                System.out.print("reading file ----------------");
                System.out.println(f + "/" + fi.getName());
                //for each file in folder create one tree
                //String fileName = path + "/" + listOfFiles[l].getName();
                File fXmlFile = new File(f + "/" + fi.getName());
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(fXmlFile);
                doc.getDocumentElement().normalize();

                //create grammar
                readGrammarDepthFirstNew(doc.getDocumentElement(), grammar);

            } else if (fi.isDirectory()) {
                //System.out.println("inside if check directory");
                createGrammar(fi, grammar);
            }
        }
    }

    /**
     * Create grammar from folder
     *
     * @param path
     * @param grammar
     * @throws Exception
     */
    public void createGrammar(String path, Map<String, Vector<String>> grammar) throws Exception {
        createGrammar(new File(path), grammar);
    }

}
