package be.intimals.freqt.util;

import be.intimals.freqt.grammar.CreateGrammar;
import be.intimals.freqt.grammar.ReadGrammar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Initial_Int {

    /**
     * build grammar from a set of ASTs
     */
    public static void initGrammar_Int(String path, Map<Integer, ArrayList<String>> gram,
                                       Map<Integer, String> labelIndex ) throws Exception {
        try{
            //System.out.println("generating grammar ... ");
            Map<String, ArrayList<String>> gramTemp = new HashMap<>();
            CreateGrammar createGrammar = new CreateGrammar();
            //createGrammar.createGrammar(path, gramTemp);
            for (Map.Entry<String,ArrayList<String >> entry : gramTemp.entrySet()){
                int index = findIndex(entry.getKey(),labelIndex);
                for(int i=2; i<entry.getValue().size(); ++i){
                    String[]temp = entry.getValue().get(i).split(Variables.uniChar);
                    if(!temp[0].equals("leaf-node")){
                        int childIndex = findIndex(temp[0],labelIndex);
                        String newChild = String.valueOf(childIndex)+Variables.uniChar+temp[1];
                        entry.getValue().set(i,newChild);
                    }else{
                        String newChild = String.valueOf(0)+Variables.uniChar+temp[1];
                        entry.getValue().set(i,newChild);
                    }
                }
                gram.put(index,entry.getValue());
            }
        }catch (Exception e){ System.out.println("Error: reading grammar "+e);}

    }

    //convert grammar in form of String to Int
    public static void initGrammar_Int(Map<Integer, ArrayList<String>> gramInt,
                                       Map<String, ArrayList<String>> gramStr,
                                       Map<Integer, String> labelIndex ) throws Exception {
        try{
            for (Map.Entry<String,ArrayList<String >> entry : gramStr.entrySet()){
                String nodeLabel = entry.getKey();
                ArrayList<String> nodeChildren = entry.getValue();
                //find index of the current label
                int index = findIndex(nodeLabel,labelIndex);
                //new int children
                ArrayList<String> newChildren = new ArrayList<>();
                newChildren.add(nodeChildren.get(0));
                newChildren.add(nodeChildren.get(1));
                //find new int children
                for(int i=2; i<nodeChildren.size(); ++i){
                    String[]temp = nodeChildren.get(i).split(Variables.uniChar);//entry.getValue().get(i).split(Variables.uniChar);
                    if(!temp[0].equals("leaf-node")){
                        int childIndex = findIndex(temp[0],labelIndex);
                        String newChild = String.valueOf(childIndex)+Variables.uniChar+temp[1];
                        newChildren.add(newChild);
                    }else{
                        String newChild = String.valueOf(0)+Variables.uniChar+temp[1];
                        newChildren.add(newChild);
                    }
                }
                //add current int label and int children into gramInt
                gramInt.put(index, newChildren);
            }
        }catch (Exception e){ System.out.println("Error: reading grammar "+e);}
    }

    /**
     * Load the grammar from a given file or build it from a set of ASTs
     */
    public static void initGrammar_Str(String path, String white, Map<String, ArrayList<String>> gram,
                                   boolean _buildGrammar) throws Exception {
        try{
            if(_buildGrammar) {
                CreateGrammar createGrammar = new CreateGrammar();
                createGrammar.createGrammar(path, white, gram);
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
        }catch (IOException e) {
            System.out.println("Error: reading listRootLabel "+e);
            e.printStackTrace();
        }
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
        }catch (IOException e) {System.out.println("Error: reading XML character error "+e);}
    }


    private static int findIndex(String label, Map<Integer, String> labelIndex){
        int index=-1;
        for(Map.Entry<Integer,String> entry : labelIndex.entrySet()){
            if(entry.getValue().equals(label)) {
                index = entry.getKey();
            }
        }
        return index;
    }

    /**
     * read whitelist and create blacklist
     */
    public static void readWhiteLabel(String path,
                                      Map<Integer,ArrayList<String>> _grammar,
                                      Map<Integer,ArrayList<Integer> > _whiteLabels,
                                      Map<Integer,ArrayList<Integer> > _blackLabels,
                                      Map<Integer,String> labelIndex){
        //all black list labels are removed
        try {
            try (BufferedReader br = new BufferedReader(new FileReader(path))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.isEmpty() && line.charAt(0) != '#') {
                        String[] str_tmp = line.split(" ");
                        String ASTNode = str_tmp[0];
                        int label_int = findIndex(ASTNode, labelIndex);
                        if (label_int != -1) {
                            ArrayList<Integer> whiteChildren_int = new ArrayList<>();
                            ArrayList<String> whiteChildren_str = new ArrayList<>();
                            for (int i = 1; i < str_tmp.length; ++i) {
                                whiteChildren_str.add(str_tmp[i]);
                                int t = findIndex(str_tmp[i], labelIndex);
                                whiteChildren_int.add(t);
                            }
                            _whiteLabels.put(label_int, whiteChildren_int);
                            //System.out.println("white labels: "+whiteChildren_int);
                            //create blacklist
                            if (_grammar.containsKey(label_int)) {
                                //get all children of label_int in grammar
                                ArrayList<String> blackChildren = new ArrayList<>();
                                blackChildren.addAll(_grammar.get(label_int).subList(2, _grammar.get(label_int).size()));
                                //System.out.println("grammar labels: "+blackChildren);
                                //transform string to int
                                ArrayList<Integer> blackChildren_int = new ArrayList<>();
                                for (int i = 0; i < blackChildren.size(); ++i) {
                                    blackChildren.set(i, blackChildren.get(i).split(Variables.uniChar)[0]);
                                    int index = Integer.valueOf(blackChildren.get(i).split(Variables.uniChar)[0]);
                                    blackChildren_int.add(index);
                                }
                                //System.out.println("grammar labels: "+blackChildren);
                                for (int i = 0; i < whiteChildren_int.size(); ++i)
                                    blackChildren_int.remove(whiteChildren_int.get(i));
                                _blackLabels.put(label_int, blackChildren_int);
                                //System.out.println("black labels: "+blackChildren_int);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("read white labels list error " + e);
            }
        }catch (Exception e){
            System.out.println("reading white list " + e);
        }
    }
}
