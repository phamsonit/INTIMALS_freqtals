import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public abstract class AOutputFormatter {
    Config config;
    Map<String, Vector<String>> grammar;
    FileWriter out;
    int nbPattern;

    Map<String,String> xmlCharacters;

    public AOutputFormatter(Config _config, Map<String, Vector<String>> _grammar, Map<String,String> _xmlCharacters)
            throws IOException {
        config = _config;
        grammar = _grammar;
        openOutputFile();
        nbPattern = 0;
        xmlCharacters = _xmlCharacters;
    }

    protected void openOutputFile() throws IOException {
        out = new FileWriter(config.getOutputFile());
    }


    public int getNbPattern(){
        return this.nbPattern;
    }

    /**
     * check if a pattern satisfies output constraints
     * @param pat
     * @return
     */
    public boolean checkOutputConstraint(Vector<String> pat){


        if(Pattern.checkMissedLeafNode(pat) || (Pattern.countLeafNode(pat) < config.getMinLeaf()) )
            return true;
        else
            return false;


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
     * calculate size of union all occurrences
     * @param projected
     * @return
     */
    public List<Integer> getSizeAllOccurrences(Projected projected){
        //print union of all occurrences
        List<Integer> tmp = new ArrayList<>(projected.getProjectLocation(0).getLocationList());
        for(int i=1; i<projected.getProjectLocationSize(); ++i) {
            tmp = union(tmp,projected.getProjectLocation(i).getLocationList());
        }
        Collections.sort(tmp);
        //////

        return tmp;

    }

    public void printAllOccurrence(Projected projected){
        for(int i=0; i<projected.getProjectLocationSize(); ++i) {
            System.out.print(projected.getProjectLocation(i).getLocationId()+" ");
            System.out.println(projected.getProjectLocation(i).getLocationList());
        }

    }

    /**
     * check a node having all children ?
     * @param pat
     * @param nodeName
     * @return
     */
    private boolean checkMandatoryChild(Vector<String> pat, String nodeName){

        boolean result = false;
        for(int i=0; i<pat.size(); ++i)
            if(pat.elementAt(i).equals(nodeName))
            {
                Vector<String> listOfChild = Pattern.findChildren(pat,i);
                String degree = grammar.get(nodeName).elementAt(1);
                if(!degree.equals(String.valueOf(listOfChild.size())))
                    return true;//result = true;
                else
                    result = false;
            }
        return result;
    }

    public abstract void report(Vector<String> pat, Projected projected);
    public abstract void close() throws IOException;
}
