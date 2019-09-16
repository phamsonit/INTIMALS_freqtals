package be.intimals.freqt.cluster;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;

public abstract class Cluster {

    //private String sep = "/";//File.separator;
    private static String uniChar = "\u00a5";

    public Map<Integer, String> labelIndex = new HashMap<>();
    public List<String> labels = new LinkedList<>();
    public ArrayList<String> fileNames = new ArrayList<>();
    public ArrayList < ArrayList<Integer> > database = new ArrayList<>();

    public int maxSize=0;
    public static boolean keepInternalLabels = true;
    public static boolean DEBUG = false;

    public String inputDir;
    public String outputDir;
    public String numberCluster;

    public String algorithmName;
    public String distanceMethod;
    public String maxDistance;
    public String minSizeCluster;



    public Cluster(String _inputDir, String _outputDir, String _numCluster){
        inputDir = _inputDir;
        outputDir = _outputDir;
        numberCluster = _numCluster;
    }

    //public abstract void Hierarchical();
    //public abstract void Spmf();


    public static void deleteDirectoryRecursion(Path path) throws IOException {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectoryRecursion(entry);
                }
            }
        }
        Files.delete(path);
    }

    //count number children of a node
    private int countNBChildren(Node node){
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
    private int countNBNodes(Node root) {
        NodeList childrenNodes = root.getChildNodes();
        int result = countNBChildren(root);
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

    //read tree by breadth first traversal
    private void readTreeDepthFirst(Node node , ArrayList <Integer> trans) {
        try {
            //TODO: exclude node label occurred in the black list
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if(keepInternalLabels){//update labelIndex for internal labels
                    if(labelIndex.isEmpty() && labels.isEmpty()) {
                        trans.add(0);
                        labelIndex.put(0, node.getNodeName());
                        labels.add(node.getNodeName());
                    }
                    else{
                        if(!labels.contains(node.getNodeName())) {
                            trans.add(labelIndex.size());
                            labelIndex.put(labelIndex.size(), node.getNodeName());
                            labels.add(node.getNodeName());
                        }else{
                            trans.add(labels.indexOf(node.getNodeName()));
                        }
                    }
                }

                if (node.hasChildNodes()) {
                    //get list of children
                    NodeList nodeList = node.getChildNodes();
                    //if node is a parent of a leaf node
                    if (node.getChildNodes().getLength() == 1) {
                        //update labelIndex for leaf labels
                        String leafLabel = node.getTextContent().replace(",",uniChar).trim();
                        if(!labels.contains(leafLabel)) {
                            //trans.get(id).setNode_label_int(labelIndex.size()*(-1));
                            trans.add(labelIndex.size());
                            labelIndex.put(labelIndex.size(), leafLabel);
                            labels.add(leafLabel);
                        }else {
                            //trans.get(id).setNode_label_int(labels.indexOf(leafLabel)*(-1));
                            trans.add(labels.indexOf(leafLabel));
                        }
                        //System.out.println("foo");
                        ///////////////
                    } else {//internal node
                        for (int i = 0; i < nodeList.getLength(); ++i) {
                            if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                                readTreeDepthFirst(nodeList.item(i), trans);
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            System.exit(-1);
        }
    }

    //create transaction from ASTs in multiple folders
    public void readDatabase(File rootDirectory) {

        //for each file, create a Array of integer
        //ArrayList < ArrayList<Double> > database = new ArrayList<>();
        //collect all input files
        ArrayList<String> files = new ArrayList<>();
        populateFileListNew(rootDirectory,files);
        //create database
        System.out.print("Reading " + files.size() +" files ... ");
        //XmlFormatter formatter = new XmlFormatter();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            for (String fi : files) {
                //keep the file's name
                fileNames.add(fi);
                //format XML file before create tree
                //String inFileTemp = rootDirectory+sep+"temp.xml";
                //Files.deleteIfExists(Paths.get(inFileTemp));
                //formatter.format(fi,inFileTemp);

                //create vector for each file
                File fXmlFile = new File(fi);
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(fXmlFile);
                doc.getDocumentElement().normalize();

                //get total number of nodes
                int size = countNBNodes(doc.getDocumentElement())+1;
                //init array to contain tree
                ArrayList<Integer> trans = new ArrayList<>(size);
                //createVector
                readTreeDepthFirst(doc.getDocumentElement(),trans);
                //
                if(maxSize < trans.size() ) maxSize = trans.size();
                //add tree to database
                database.add(trans);
                //delete temporary input file
                //Files.deleteIfExists(Paths.get(inFileTemp));
                //System.out.print(".");
            }
            System.out.println(" end.");
        } catch (Exception e) {
            System.out.println(" read error.");
            e.printStackTrace();
            System.exit(-1);
        }
        //return database;
    }


}
