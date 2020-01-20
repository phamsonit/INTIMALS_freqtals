package be.intimals.freqt.core;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.constraint.Constraint;
import be.intimals.freqt.input.ReadFile;
import be.intimals.freqt.output.AOutputFormatter;
import be.intimals.freqt.output.XMLOutput;
import be.intimals.freqt.structure.*;
import be.intimals.freqt.util.Variables;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

/*
    find a common pattern in each cluster
 */
public class FreqT_common {

    private Config config;
    private Map <String,ArrayList<String> > grammar;
    private Map <String,String> xmlCharacters;

    private Map<String,String> commonOutputPatterns = null;
    private Vector < String > maximalPattern = null;
    private ArrayList < ArrayList<NodeFreqT> > newTransaction = null;
    private int minsup;
    private boolean found;

    ////////////////////////////////////////////////////////////////////////////////

    public FreqT_common(Config _config, Map <String,ArrayList<String> > _grammar, Map <String,String> _xmlCharacters){
        config = _config;
        grammar = _grammar;
        xmlCharacters = _xmlCharacters;
    }

    /**
     * prune candidates based on minimal support
     * @param candidates
     */
    public void prune (Map <String, Projected > candidates, int minSup){

        Iterator < Map.Entry<String,Projected> > iter = candidates.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String,Projected> entry = iter.next();
            int sup = Constraint.getSupport(entry.getValue());
            int wsup = Constraint.getRootSupport(entry.getValue());
            if(sup < minSup){
                iter.remove();
            }
            else {
                entry.getValue().setProjectedSupport(sup);
                entry.getValue().setProjectedRootSupport(wsup);
            }
        }
    }

    /**
     * expand a subtree
     * @param projected
     */
    private void project(Projected projected) {
            if(found) return;
            //find all candidates of the current subtree
            int depth = projected.getProjectedDepth();
            Map <String , Projected > candidate = new LinkedHashMap<>();
            for(int i = 0; i < projected.getProjectLocationSize(); ++i ){
                int id  = projected.getProjectLocation(i).getLocationId();
                int pos = projected.getProjectLocation(i).getLocationPos();
                String prefix = "";
                for(int d = -1; d < depth && pos != -1; ++d) {
                    int start = (d == -1) ?
                            newTransaction.get(id).get(pos).getNodeChild() :
                            newTransaction.get(id).get(pos).getNodeSibling();
                    int newdepth = depth - d;
                    for (int l = start; l != -1;
                         l = newTransaction.get(id).get(l).getNodeSibling()) {
                        String item = prefix + Variables.uniChar + newTransaction.get(id).get(l).getNodeLabel();

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
                    }
                    if (d != -1) pos = newTransaction.get(id).get(pos).getNodeParent();
                    prefix += Variables.uniChar+")";
                }
            }

            prune(candidate,minsup);

            if(candidate.isEmpty()){
                addCommonPattern(maximalPattern,projected);
                //System.out.println("common pattern "+maximalPattern);
                found = true;
                //return;
            }else {
                //expand the current pattern with each candidate
                Iterator<Map.Entry<String, Projected>> iter = candidate.entrySet().iterator();
                while (iter.hasNext()) {
                    int oldSize = maximalPattern.size();
                    Map.Entry<String, Projected> entry = iter.next();
                    // add new candidate to current pattern
                    String[] p = entry.getKey().split(Variables.uniChar);
                    for (int i = 0; i < p.length; ++i) {
                        if (!p[i].isEmpty())
                            maximalPattern.add(p[i]);
                    }
                    project(entry.getValue());
                    maximalPattern.setSize(oldSize);
                }
            }
    }


    //1. for each cluster find a set of patterns
    //2. create a tree data
    //3. find the common pattern
    //4. write cluster-common pattern to file
    public void run(String inPatterns, String inClusters, String outCommonFile){
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
                    newTransaction = new ArrayList<>();
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
                            maximalPattern.add(entry.getKey());
                            project(entry.getValue());
                            maximalPattern.remove(maximalPattern.size() - 1);
                        }
                    }
                }
            }
            //output common pattern in each cluster
            outputMFP(commonOutputPatterns,outCommonFile);
    }


    /**
     * Return all frequent subtrees of size 1
     * @return
     */
    public Map<String, Projected> buildFP1Set(ArrayList < ArrayList<NodeFreqT> > trans) {
        Map<String, Projected> freq1 = new LinkedHashMap<>();
        for(int i = 0; i < trans.size(); ++i) {
            for (int j = 0; j < trans.get(i).size(); ++j) {
                String node_label = trans.get(i).get(j).getNodeLabel();
                String lineNr = trans.get(i).get(j).getLineNr();
                //if node_label in rootLabels and lineNr > lineNr threshold
                //if(rootLabels.contains(node_label) || rootLabels.isEmpty()){
                    //System.out.println(lineNr+"  "+lineNrs.elementAt(i));
                    //if(Integer.valueOf(lineNr) > lineNrs.elementAt(i)){ //using only for Cobol data
                    //find a list of locations then add it to freq1[node_label].locations

                    if (node_label != null) {
                        //System.out.println("Node "+ node_label+" "+lineNr);
                        //if node_label already exists
                        if (freq1.containsKey(node_label)) {
                            freq1.get(node_label).setProjectLocation(i, j);
                            //freq1.get(node_label).setProjectLineNr(Integer.valueOf(lineNr)); //add to keep the line number
                            //freq1.get(node_label).setProjectRootLocation(i, j);
                        } else {
                            Projected projected = new Projected();
                            projected.setProjectLocation(i, j);
                            //projected.setProjectLineNr(Integer.valueOf(lineNr)); //add to keep the line number
                            //projected.setProjectRootLocation(i, j);
                            freq1.put(node_label, projected);
                        }
                    }
                    //}
                //}
            }
        }
        return freq1;
    }

    private void addCommonPattern(Vector<String> pat, Projected projected){

        int support = projected.getProjectedSupport();
        int wsupport = projected.getProjectedRootSupport(); //=> root location
        int size = Pattern.getPatternSize(pat);

        //replace "," in the leafs by uniChar
        String patString = pat.get(0);
        for (int i = 1; i < pat.size(); ++i)
            patString = patString + "," + pat.get(i);
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

    //filter and print maximal patterns
    public void outputMFP(Map<String,String> maximalPatterns,String outFile){
        try{

            FileWriter outputCommonPatterns = new FileWriter(outFile+".txt");
            //output maximal patterns
            AOutputFormatter outputMaximalPatterns =  new XMLOutput(outFile,config, grammar, xmlCharacters);
            Iterator < Map.Entry<String,String> > iter1 = maximalPatterns.entrySet().iterator();
            while(iter1.hasNext()){
                Map.Entry<String,String> entry = iter1.next();
                outputMaximalPatterns.printPattern(entry.getValue());
                //System.out.println(entry.getKey());
                outputCommonPatterns.write(entry.getKey()+"\n");
            }
            outputMaximalPatterns.close();

            outputCommonPatterns.flush();
            outputCommonPatterns.close();

        }
        catch(Exception e){System.out.println("error print maximal patterns");}
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


    private List<String> readPatterns(String inPatterns){
        List<String> patterns = new LinkedList<>();
        try{
            FileReader fr = new FileReader(inPatterns);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                if( ! line.isEmpty() )
                    patterns.add(line);
            }
            br.close();
            fr.close();
        }catch (Exception e){
            e.printStackTrace();
            System.exit(-1);
        }
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
        }catch (Exception e){
            e.printStackTrace();
            System.exit(-1);
        }

        return temp;
    }


}