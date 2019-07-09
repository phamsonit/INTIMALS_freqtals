package be.intimals.freqt.core;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.input.ReadFile;
import be.intimals.freqt.output.AOutputFormatter;
import be.intimals.freqt.output.XMLOutput;
import be.intimals.freqt.structure.Location;
import be.intimals.freqt.structure.NodeFreqT;
import be.intimals.freqt.structure.Pattern;
import be.intimals.freqt.structure.Projected;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;


public class FreqT_common extends FreqT {

    private Map<String,String> commonOutputPatterns = null;
    private Vector < String > maximalPattern = null;
    private Vector < Vector<NodeFreqT> > newTransaction = null;
    private int minsup;
    private boolean found;

    ////////////////////////////////////////////////////////////////////////////////

    public FreqT_common(Config config,
                           Map<String,Vector<String>> grammar,
                           Map<String,String> xmlCharacters) {
        super(config);
        this.grammar = grammar;
        this.xmlCharacters = xmlCharacters;

    }

    /**
     * expand a subtree
     * @param projected
     */
    private void project(Projected projected) {
        try{
            if(found) return;
            //find all candidates of the current subtree
            int depth = projected.getProjectedDepth();
            Map <String , Projected > candidate = new LinkedHashMap<>();
            for(int i = 0; i < projected.getProjectLocationSize(); ++i ){
                int id  = Location.getLocationId(projected.getProjectLocation(i));
                int pos = Location.getLocationPos(projected.getProjectLocation(i));
                String prefix = "";
                for(int d = -1; d < depth && pos != -1; ++d) {
                    int start = (d == -1) ?
                            newTransaction.elementAt(id).elementAt(pos).getNodeChild() :
                            newTransaction.elementAt(id).elementAt(pos).getNodeSibling();
                    int newdepth = depth - d;
                    for (int l = start; l != -1;
                         l = newTransaction.elementAt(id).elementAt(l).getNodeSibling()) {
                        String item = prefix + uniChar + newTransaction.elementAt(id).elementAt(l).getNodeLabel();

                        Projected tmp;// = new Projected();
                        if(candidate.containsKey(item)) {
                            candidate.get(item).setProjectLocation(id,l); //store right most positions
                        }
                        else {
                            tmp = new Projected();
                            tmp.setProjectedDepth(newdepth);
                            tmp.setProjectLocation(id,l); //store right most positions
                            candidate.put(item, tmp);
                        }
                        //////////

                    }
                    if (d != -1) pos = newTransaction.elementAt(id).elementAt(pos).getNodeParent();
                    prefix += uniChar+")";
                }
            }

            prune(candidate,minsup);

            if(candidate.isEmpty()){
                //outputPattern = Pattern.getPatternString1(maximalPattern);
                //String pattern = maximalPattern.toString(); //Pattern.getPatternString(maximalPattern);
//                String patString = maximalPattern.elementAt(0);
//                for(int i=1; i< maximalPattern.size(); ++i)
//                    patString = patString+"," + maximalPattern.elementAt(i);
//                commonOutputPatterns.put(patString,patString);

                addCommonPattern(maximalPattern,projected);

                //System.out.println("common pattern "+maximalPattern);
                found = true;
                return;
            }else {
                //expand the current pattern with each candidate
                Iterator<Map.Entry<String, Projected>> iter = candidate.entrySet().iterator();
                while (iter.hasNext()) {
                    int oldSize = maximalPattern.size();
                    Map.Entry<String, Projected> entry = iter.next();
                    // add new candidate to current pattern
                    String[] p = entry.getKey().split(String.valueOf(uniChar));
                    for (int i = 0; i < p.length; ++i) {
                        if (!p[i].isEmpty())
                            maximalPattern.addElement(p[i]);
                    }
                    project(entry.getValue());
                    maximalPattern.setSize(oldSize);
                }
            }
        }catch (Exception e){
            System.out.println("ERROR: FREQT_common project " + e);
        }
    }

    private void addCommonPattern(Vector<String> pat, Projected projected){

        int support = projected.getProjectedSupport();
        int wsupport = projected.getProjectedRootSupport(); //=> root location
        int size = Pattern.getPatternSize(pat);

        //replace "," in the leafs by uniChar
        String patString = pat.elementAt(0);
        for (int i = 1; i < pat.size(); ++i)
            patString = patString + "," + pat.elementAt(i);
        //patString = patString + uniChar + pat.elementAt(i);
        String patternSupport =
                "rootOccurrences" + "," +
                        String.valueOf(support) + "," +
                        String.valueOf(wsupport) + "," +
                        String.valueOf(size) + "\t" +
                        patString;//pat.toString(); //keeping for XML output
        commonOutputPatterns.put(pat.toString(), patternSupport);
        //System.out.println(patString);
        //System.out.println(Pattern.getPatternString(pat));
    }

    //1. for each cluster find a set of patterns
    //2. create a tree data
    //3. find the common pattern
    //4. write cluster-common pattern to file
    public void run(String inPatterns, String inClusters, String outCommonFile){
        try{
            commonOutputPatterns = new LinkedHashMap<>();
            List<List<Integer>> clusters = readClusters(inClusters);
            List<String> patterns = readPatterns(inPatterns);
            //System.out.println("nb clusters "+clusters.size());
            //System.out.println("nb patterns "+patterns.size());

            for(int i=0; i< clusters.size(); ++i){
                //System.out.println("cluster: "+ i + " #patterns: "+ clusters.get(i).size());
                if(clusters.get(i).size() < 2){
                    String ttt = "1,1,1,1\t"+Pattern.covert(patterns.get(clusters.get(i).get(0)-1));
                    commonOutputPatterns.put(patterns.get(clusters.get(i).get(0)-1),ttt);
                    //System.out.println(ttt);
                    //System.out.println("common pattern "+ patterns.get(clusters.get(i).get(0)-1));
                    //System.out.println("pattern ID "+clusters.get(i).get(0));

                }else{
                    Map<String,String> tempDatabase = new HashMap<>();
                    for(int j=0; j<clusters.get(i).size(); ++j){
                        //System.out.println(patterns.get(clusters.get(i).get(j)-1));
                        tempDatabase.put(patterns.get(clusters.get(i).get(j)-1),"nothing");
                    }
                    found = false;
                    newTransaction = new Vector<>();
                    initDatabase(tempDatabase);
                    minsup = tempDatabase.size();
                    Map<String,Projected > FP1 = buildFP1Set(newTransaction);
                    prune(FP1, minsup);
                    maximalPattern = new Vector<>();
                    Iterator < Map.Entry<String,Projected> > iter = FP1.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry<String,Projected> entry = iter.next();
                        if(entry.getKey() != null && entry.getKey().charAt(0) != '*'){
                            entry.getValue().setProjectedDepth(0);
                            maximalPattern.addElement(entry.getKey());
                            project(entry.getValue());
                            maximalPattern.setSize(maximalPattern.size() - 1);
                        }
                    }
                }
            }
            //output common pattern in each cluster
            outputMFP(commonOutputPatterns,outCommonFile);

        }catch (Exception e){  }



    }

    /**
     * create transaction from list of patterns
     * @param patterns
     */
    private void initDatabase(Map<String,String> patterns) {
        //System.out.println("reading input subtrees ...");
        ReadFile readFile = new ReadFile();
        readFile.createTransactionFromMap(patterns,newTransaction);
    }


    private static void visitNode(Node node){

        if(node.getNodeName().equals("subtree")) {
            System.out.print("(");
        }
        else {
            System.out.print(node.getNodeName() + "(");
        }
        if(node.hasChildNodes()){
            NodeList nodeList = node.getChildNodes();

            if(nodeList.getLength()==1){//leaf
                if(node.getNodeType() == Node.ELEMENT_NODE){
                    System.out.print(node.getTextContent().trim()+")");
                    //add how many )
                }
            }else{
                for(int i=0; i<nodeList.getLength();++i)
                {
                    if(nodeList.item(i).getNodeType() == Node.ELEMENT_NODE &&
                            nodeList.item(i).getNodeName().charAt(0)!='_'){
                        visitNode(nodeList.item(i));
                    }
                }
            }
        }

    }


    private List<String> readPatterns(String inPatterns){
        List<String> patterns = new LinkedList<>();
        try{
            BufferedReader br = new BufferedReader(new FileReader(inPatterns));
            String line;
            while ((line = br.readLine()) != null) {
                if( ! line.isEmpty() )
                    patterns.add(line);
            }
        }catch (Exception e){System.out.println(e);}
        return  patterns;
    }

    private List<List<Integer>> readClusters(String inClusters){
        List<List<Integer> > temp = new LinkedList<>();

        try{
            File fXmlFile = new File(inClusters);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();
            //for each cluster ID, collect a list of pattern ID
            NodeList clusters = doc.getDocumentElement().getChildNodes();
            for(int i=0; i < clusters.getLength(); ++i){
                if(clusters.item(i).getNodeType() == Node.ELEMENT_NODE){
                    NodeList patterns = clusters.item(i).getChildNodes();
                    //for each patterns get the pattern ID
                    List<Integer> t = new LinkedList<>();
                    for(int j=0; j<patterns.getLength(); ++j){
                        if(patterns.item(j).getNodeType()==Node.ELEMENT_NODE){
                            NamedNodeMap nodeMap = patterns.item(j).getAttributes();
                            int ID = Integer.valueOf(nodeMap.getNamedItem("ID").getNodeValue());
                            t.add(ID);
                        }
                    }
                    temp.add(t);
                }
            }

        }catch (Exception e){System.out.println("file cluster not found "+e); }

        return temp;
    }




}