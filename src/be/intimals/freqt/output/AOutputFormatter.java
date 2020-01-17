package be.intimals.freqt.output;

import be.intimals.freqt.structure.*;
import be.intimals.freqt.config.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public abstract class AOutputFormatter {
    int nbPattern;
    String fileName;
    FileWriter out;
    Config config;
    Map<String, ArrayList<String>> grammar;
    Map<String,String> xmlCharacters;
    Map<String,String> patSupMap = new LinkedHashMap<>();


    public AOutputFormatter(String _fileName, Config _config, Map<String, ArrayList<String>> _grammar, Map<String,String> _xmlCharacters)
            throws IOException {
        nbPattern = 0;
        fileName = _fileName;
        config = _config;
        grammar = _grammar;
        xmlCharacters = _xmlCharacters;
        openOutputFile();
    }

    protected void openOutputFile() throws IOException {
        //out = new FileWriter(config.getOutputFile());
        out = new FileWriter(fileName);
    }

    public int getNbPattern(){
        return this.nbPattern;
    }

    /**
     * check if a pattern satisfies output constraints
     * @param pat
     * @return
     */
    public boolean checkOutputConstraint(ArrayList<String> pat){

        /*if(Pattern.isMissedLeafNode(pat) ||
                (Pattern.countLeafNode(pat) < config.getMinLeaf()) )
            return true;
        else
            return false;*/

        return true;
    }

    /**
     * union two lists
     * @param list1
     * @param list2
     * @param <T>
     * @return
     */
    public <T> List<T> union(List<T> list1, List<T> list2) {
        Set<T> set = new HashSet<T>();
        set.addAll(list1);
        set.addAll(list2);
        return new ArrayList<T>(set);
    }

    /**
     * check a node having all children ?
     * @param pat
     * @param nodeName
     * @return
     */
    /*
    private boolean checkMandatoryChild(ArrayList<String> pat, String nodeName){

        boolean result = false;
        for(int i=0; i<pat.size(); ++i)
            if(pat.get(i).equals(nodeName))
            {
                ArrayList<String> listOfChild = Pattern.findChildrenLabels(pat,i);
                String degree = grammar.get(nodeName).get(1);
                if(!degree.equals(String.valueOf(listOfChild.size())))
                    return true;//result = true;
                else
                    result = false;
            }
        return result;
    }*/

    //public abstract void report(Vector<String> pat, Projected projected);
    public abstract void printPattern(String pat);
    public abstract void close() throws IOException;
}
