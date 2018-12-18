import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class Initial {
    private  static  char uniChar = '\u00a5';// Japanese Yen symbol
    /**
     * Loads data from folders
     */
    public static void initDatabase(String path, Map<String,Vector<String>> gram, Vector < Vector<NodeFreqT> > trans) {

        System.out.println("reading tree data ...");
        ReadXML readXML = new ReadXML();
        readXML.createTransaction(new File(path),gram,trans);
        //System.out.println("total files: "+transaction.size());

        //create transaction from single input file
        //ReadFile r = new ReadFile();
        //r.createTransaction(inFile,transaction);
    }

    /**
     * Load the grammar from a given file or build it from a set of ASTs
     */
    public static void initGrammar(String path, Map<String, Vector<String>> gram, boolean _buildGrammar) throws Exception {
        //read grammar from grammarFile
        try{
            System.out.println("reading grammar ... ");
            if(_buildGrammar) {
                CreateGrammar createGrammar = new CreateGrammar();
                createGrammar.createGrammar(path, gram);
            } else {
                ReadGrammar read = new ReadGrammar();
                read.readGrammar(path, gram);
            }
        }catch (Exception e){ System.out.println("Error: reading grammar "+e);}

    }

    /**
     * read list of root labels
     */
    public static void readRootLabel(String path, Set<String> rootLabels){

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if( ! line.isEmpty() && line.charAt(0) != '#' ){
                    String[] str_tmp = line.split(" ");
                    rootLabels.add(str_tmp[0]);
                }
            }
        }catch (IOException e) {System.out.println("Error: reading listRootLabel "+e);}

    }

    /**
     * read list of special XML characters
     */
    public static void readXMLCharacter(String path, Map<String,String> listCharacters){

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if( ! line.isEmpty() && line.charAt(0) != '#' ){
                    String[] str_tmp = line.split("\t");
                    listCharacters.put(str_tmp[0],str_tmp[1]);
                }
            }
        }catch (IOException e) {System.out.println("Error: reading XMLCharater "+e);}
    }

    /**
     * read whitelist and create blacklist
     */
    public static void readWhiteLabel(String path,
                                Map<String,Vector<String>> _grammar,
                                Map <String,Vector<String> > _whiteLabels,
                                Map <String,Vector<String> > _blackLabels){

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if( ! line.isEmpty() && line.charAt(0) != '#' ) {
                    String[] str_tmp = line.split(" ");
                    String ASTNode = str_tmp[0];
                    Vector<String> children = new Vector<>();
                    for(int i=1; i< str_tmp.length; ++i)
                        children.addElement(str_tmp[i]);

                    _whiteLabels.put(ASTNode,children);

                    //create blacklist
                    if(_grammar.containsKey(ASTNode))
                    {
                        Vector<String> blackChildren = new Vector<>();
                        blackChildren.addAll(_grammar.get(ASTNode).subList(2, _grammar.get(ASTNode).size()));

                        for(int i=0; i<blackChildren.size();++i)
                            blackChildren.set(i,blackChildren.elementAt(i).split(String.valueOf(uniChar))[0]);

                        for(int i=0; i<children.size();++i)
                            for(int j=0; j<blackChildren.size();++j)
                                if(children.elementAt(i).equals(blackChildren.elementAt(j)))
                                    blackChildren.remove(j);

                        _blackLabels.put(ASTNode,blackChildren);
                    }
                }
            }
        }catch (IOException e) {System.out.println("Error: reading white list "+e);}
    }
}
