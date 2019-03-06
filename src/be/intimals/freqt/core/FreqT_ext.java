package be.intimals.freqt.core;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.structure.*;

import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;


import be.intimals.freqt.output.*;

public class FreqT_ext extends FreqT {

    private AOutputFormatter outputLargest;
    private AOutputFormatter outputMaximalPatterns;

    private Vector <String> largestPattern;
    private Vector <Vector<NodeFreqT> >  transaction = new Vector<>();
    private Map<String,String> outputLargestPatternsMap = new HashMap<>();


    private int nbOutputLargestPatterns;
    private int largestMinSup;

    private Map<String,String> rootIDs = new HashMap<>();

    //test check maximality
    private Map<String,String> outputMaximalPatternsMap1 = new HashMap<>();

    //test: don't store patterns in variable
    //add patterns to file -->
    FileWriter output;

    ////////////////////////////////////////////////////////////////////////////////

    public FreqT_ext(Config config,
                     Map<String,Vector<String>> grammar,
                     Map<String,Vector<String>> blackLabels,
                     Map<String,Vector<String>> whiteLabels,
                     Map<String,String> xmlCharacters) {
        super(config);
        this.grammar = grammar;
        this.blackLabels = blackLabels;
        this.whiteLabels = whiteLabels;
        this.xmlCharacters = xmlCharacters;
    }

    public int getNbOutputLargestPatterns(){
        return this.nbOutputLargestPatterns;
    }

    public Map<String,String> getOutputLargestPatterns(){ return this.outputLargestPatternsMap; }


    private void chooseOutput(Vector<String> pat, Projected projected){

        if (checkOutput(pat)) return;
        //keep one largest pattern per group root occurrences
        //addRootIDs(pat, projected,rootIDs);
        //keep all largest patterns
        //addPattern(pat, projected, outputLargestPatternsMap);
        addMaximality (pat,projected,outputMaximalPatternsMap1);
    }

    //return 1 : pat1 is subset of 2; 2 : pat2 is subset of pat1; otherwise return 0
    private int checkSubTree(String pat1, String pat2){

        //System.out.println("===================");
        FreqT_max1 post = new FreqT_max1(this.config);
        post.run(pat1, pat2);

        if (post.getOutputPattern() == null)
        {
            return 0;
        }else
            {
                if( pat1.length() <= pat2.length() ) {
                    return 1;
                }
                else {
                    return 2;
                }
            }

    }

    //input pat, patSet
    //for each element in patSet
    //if pat is a subtree of element return
    //else if element is a subset of pat then replace element by pat
    //else add pat to patSet
    private void addMaximality(Vector<String> pat, Projected projected, Map<String,String> patSet){
        boolean found = false;
        Iterator < Map.Entry<String,String> > p = patSet.entrySet().iterator();
        while(p.hasNext()){
            Map.Entry<String, String> entry = p.next();

            switch (checkSubTree(Pattern.getPatternString1(pat),entry.getKey())){
                case 1:
                    found = true;
                    break;
                case 2:
                    //found = true;
                    p.remove();
                    break;
            }
        }
        if(! found) {
            int support = projected.getProjectedSupport();
            int wsupport = projected.getProjectedRootSupport(); //=> root location
            int size = Pattern.getPatternSize(pat);
            //find root occurrences of pattern
            /*String rootOccurrences = "";
            for (int i = 0; i < projected.getProjectRootLocationSize(); ++i) {
                rootOccurrences = rootOccurrences +
                        projected.getProjectRootLocation(i).getLocationId() + ("-") +
                        projected.getProjectRootLocation(i).getLocationPos() + ";";
            }*/
            String patternSupport =
                    "rootOccurrences" + "," +
                            String.valueOf(support) + "," +
                            String.valueOf(wsupport) + "," +
                            String.valueOf(size)+"\t"+
                            pat.toString(); //keeping for XML output

            String patternString = Pattern.getPatternString1(pat); //filter out the right part of pattern which misses leaf nodes
            patSet.put(patternString, patternSupport);
        }
    }

    //expand candidate based on grammar
    private void grammarExpand(Map.Entry<String, Projected> entry){

        //get the current candidate label
        String potentialCandidate = Pattern.getPotentialCandidateLabel(entry.getKey());
        if ( potentialCandidate.charAt(0) == '*' ) { //potentialCandidate is a leaf node
            project( entry.getValue() );
        }else {//internal node
            //find grammar of parent of potentialCandidate
            int parentPos = Pattern.findParentPosition(largestPattern, entry.getKey());
            String parentLabel = largestPattern.elementAt(parentPos).split(String.valueOf(uniChar))[0];

            if ( grammar.containsKey(parentLabel) ) {
                String parentOrdered = grammar.get(parentLabel).elementAt(0);
                String parentDegree  = grammar.get(parentLabel).elementAt(1);
                switch (parentDegree) {
                    case "1": //node has one non-terminal child or one leaf
                        project(entry.getValue());
                        break;
                    case "1..*"://node-list
                        //project(entry.getValue());
                        if(parentOrdered.equals("unordered")) {
                            /*check previous sibling
                            Set<String> previousSiblings = getPreviousSibling(entry.getValue(), transaction);
                            Set<String> currentChildren = new HashSet<>(Pattern.findChildren(largestPattern,parentPos));
                            previousSiblings.retainAll(currentChildren);
                            if(previousSiblings.size()>0) {
                                if (config.postProcess()) {
                                    addPattern(largestPattern, entry.getValue(), outputLargestPatternsMap);
                                    outputLargest.report(largestPattern, entry.getValue());
                                } else
                                    outputLargest.report(largestPattern, entry.getValue());
                                return;
                            }*/
                            //grammar constraint: don't allow N children of an unordered node to have the same label
                            if (Pattern.isRepeatedLabel(largestPattern, entry.getKey(), config.getMaxRepeatLabel()))
                                //check line distance of 2 nodes which have the same label
                                //if(Pattern.checkLineDistance(pattern, entry.getKey(), entry.getValue(), config.getMinLineDistance(), config.getMaxLineDistance()))
                                project(entry.getValue());
                                else{//output the current pattern
                                    chooseOutput(largestPattern,entry.getValue());
                                    return;
                                }
                        }else
                            if(parentOrdered.equals("ordered")){
                                project(entry.getValue());
                            }
                        break;

                    default: //AST node has fixed N children
                        //project(entry.getValue());
                        //find all children of parentPos in the grammar
                        Vector<String> listOfChildrenGrammar = new Vector<>();
                        listOfChildrenGrammar.addAll(grammar.get(parentLabel).subList(2, grammar.get(parentLabel).size()));
                        //find all children of parentPos in the pattern
                        Vector<String> listOfChildrenPattern = Pattern.findChildren(largestPattern, parentPos);
                        //find white labels and black labels
                        Set<String> blackLabelChildren = new LinkedHashSet<>();
                        Set<String> whiteLabelChildren = new LinkedHashSet<>();
                        if(whiteLabels.containsKey(parentLabel))
                            whiteLabelChildren.addAll(whiteLabels.get(parentLabel));
                        if(blackLabels.containsKey(parentLabel))
                            blackLabelChildren.addAll(blackLabels.get(parentLabel));
                        //expand this candidate if it doesn't miss the previous mandatory sibling
                        if (! Pattern.isMissedMandatoryChild(listOfChildrenPattern, listOfChildrenGrammar, blackLabelChildren, whiteLabelChildren)) {
                            project(entry.getValue());
                        }else {
                            chooseOutput(largestPattern, entry.getValue());
                            return;
                        }

                        break;
                }
            }
        }
        //garbage collector
        //System.gc();

    }

    /**
     * expand a pattern without size constraint
     * @param entry
     */
    private void expandCandidate(Map.Entry<String, Projected> entry) {
        try{
            // add a candidate to the current pattern
            String[] p = entry.getKey().split(String.valueOf(uniChar));
            for (int i = 0; i < p.length; ++i) {
                if (!p[i].isEmpty())
                    largestPattern.addElement(p[i]);
            }
            //don't check size constraints
            //don't check full leaf constraint ==> result: time consuming!!!
            //check full leaf node
            if(Pattern.isMissedLeafNode(largestPattern)) {
                chooseOutput(largestPattern,entry.getValue());
                return;
            }
            else
                grammarExpand(entry);
                //project(entry.getValue());

        }catch (Exception e){System.out.println("Error: Freqt_ext expand candidate " + e);}
    }


    /**
     * expand a subtree
     * @param projected
     */
    private void project(Projected projected) {
        try{

            //don't check size constraints
            //if(Pattern.countLeafNode(pattern) >= config.getMaxLeaf()) return;


            //find candidates
            Map<String, Projected> candidates = generateCandidates(projected,transaction);
            //System.out.println("all candidates     " + candidates.keySet());
            prune(candidates,largestMinSup);
            //System.out.println("after support pruning " + candidates.keySet());
            //pruning based on blacklist: for each candidate if it occurs in the blacklist --> remove
            pruneBlackList(largestPattern,candidates,blackLabels);
            //System.out.println("after blacklist pruning " + candidates.keySet());
            //if there is no candidate then report pattern --> stop
            if( candidates.isEmpty() ){
                chooseOutput(largestPattern,projected);
                return;
            }

            //expand the current pattern with each candidate
            Iterator < Map.Entry<String,Projected> > iter = candidates.entrySet().iterator();
            while (iter.hasNext()) {
                int oldSize = largestPattern.size();

                Map.Entry<String, Projected> entry = iter.next();

                expandCandidate(entry);

                largestPattern.setSize(oldSize);
            }
            //garbage collector
            //System.gc();
        }catch (Exception e){
            System.out.println("Error: Freqt_ext projected " + e);
            e.printStackTrace();
        }
    }


    public void run(Map <String, String > _rootIDs,
                              Vector <Vector<NodeFreqT>  > _transaction){

        try{
            transaction = _transaction;

            //store result of the second step ???
            /*String fileName = "phase2-"+config.getOutputFile().replaceAll("\\/","-");
            outputLargest = config.outputAsXML() ? new XMLOutput(fileName,config, grammar, xmlCharacters) :
                    new LineOutput(fileName,config, grammar, xmlCharacters, uniChar);*/

            //for each group of root occurrences find patterns without size constraints
            largestPattern = new Vector<>();
            Iterator < Map.Entry<String,String> > rootId = _rootIDs.entrySet().iterator();
            while(rootId.hasNext()){

                //long start = System.currentTimeMillis( );
                Map.Entry<String,String> entry = rootId.next();
                //System.out.println(entry.getValue());
                String tmp[] = entry.getValue().split("\t");
                //String rootLabel = entry.getValue().split(String.valueOf("\\("))[1];
                String rootLabel = tmp[1].split(String.valueOf(","))[0].substring(1);
                //String rootLabel = entry.getValue();
                //System.out.println("=============================================");
                //System.out.println(entry.getKey());

                largestPattern.add(rootLabel);
                Projected projected = new Projected();
                projected.setProjectedDepth(0);
                //set locations for projected
                String[] temp = entry.getKey().split(";");
                for(int i=0; i<temp.length; ++i){
                    String[] pos = temp[i].split("-");
                    projected.setProjectLocation(Integer.valueOf(pos[0]),Integer.valueOf(pos[1]));
                    projected.setProjectRootLocation(Integer.valueOf(pos[0]),Integer.valueOf(pos[1]));
                }
                largestMinSup = support(projected);
                project(projected);
                largestPattern.setSize(largestPattern.size() - 1);
                //System.out.println(outputLargestPatternsMap2.size());

                //garbage collector
                //System.gc();
                /*
                //Test: do the maximality check for each group of largest patterns
                //System.out.println("#largest patterns: "+outputLargestPatternsMap.size());
                //test result: don't improve running time; generate larger number of maximal subtrees

                System.out.println("#largest patterns: "+outputLargestPatternsMap.size());
                FreqT_max post = new FreqT_max(this.config, this.grammar, this.blackLabels, this.whiteLabels, this.xmlCharacters);
                post.run(outputLargestPatternsMap);
                System.out.println("#maximal patterns: "+post.getNbMaximalPattern());
                outputLargestPatternsMap.clear();
                ///////////////////

                long end1 = System.currentTimeMillis( );
                long diff1 = end1 - start;
                System.out.println("#running time = "+ diff1);*/
            }
            //outputLargest.close();
            //garbage collector
            //System.gc();

            nbOutputLargestPatterns = outputMaximalPatternsMap1.size();
            outputMaximalPatterns =  new XMLOutput(config.getOutputFile(),config, grammar, xmlCharacters);
            //System.out.println("outputLargestPatternsMap1 :" + outputLargestPatternsMap1.size()+" ==>");
            Iterator < Map.Entry<String,String> > iter1 = outputMaximalPatternsMap1.entrySet().iterator();
            while(iter1.hasNext()){
                Map.Entry<String,String> entry = iter1.next();
                //print to screen
                //System.out.println(entry.getValue());
                //XML output
                outputMaximalPatterns.printPattern(entry.getValue());
            }
            outputMaximalPatterns.close();

            //test: keep rootIDs to compare with maximal patterns in the third step
            //for each group of patterns keep only the largest
            //test result: improve the running time, but risk of losing maximal subtrees

            /*//FileWriter fileWriter = new FileWriter(config.getOutputFile()+".xml");
            Iterator < Map.Entry<String,String> > rootIdNew = rootIDs.entrySet().iterator();
            while(rootIdNew.hasNext()){
                Map.Entry<String,String> entry = rootIdNew.next();
                //add the largest patterns to the outputLargestPatternsMap
                String[] strTmp = entry.getValue().split("\t");
                //adding fileIds to have the same format of supports of outputLargestPatternsMap
                String supports = strTmp[0];
                //create pattern string
                Vector<String> pat = Pattern.formatPattern(strTmp[1].substring(1,strTmp[1].length()-1).split(","));
                //put the pattern,support into the outputLargestPatternsMap
                //keeping the largest patterns for maximality check
                outputLargestPatternsMap.put(Pattern.getPatternString1(pat),supports);
                //System.out.println(entry.getKey()+" : "+entry.getValue());
                //log(fileWriter,entry.getKey()+"\t"+Pattern.getPatternString1(pat));
            }
            nbOutputLargestPatterns = outputLargestPatternsMap.size();*/
            //fileWriter.close();
            //System.out.print("number of largest patterns "+outputLargestPatternsMap.size());

        }catch (Exception e){}


    }

}