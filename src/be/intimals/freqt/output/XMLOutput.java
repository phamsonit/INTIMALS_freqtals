package be.intimals.freqt.output;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.core.FreqT;
import be.intimals.freqt.structure.Pattern;
import be.intimals.freqt.structure.Projected;
import be.intimals.freqt.util.Util;

import java.io.IOException;
import java.util.*;

public class XMLOutput extends AOutputFormatter {
    private char uniChar = '\u00a5';

    //boolean abstractLeafs = false;
    private Set<String> reservedLabels = new HashSet<>();

    /////////////////////////////

    public XMLOutput(String _file, Config _config, Map<String, ArrayList<String>> _grammar, Map<String,String> _xmlCharacters) throws IOException {
        super(_file,_config, _grammar, _xmlCharacters);
        reservedLabels = FreqT.getReservedLabels();
    }

    public XMLOutput(String _file, Config _config, Map<String, ArrayList<String>> _grammar, Map<String,String> _xmlCharacters,
                     Map<String,String> _patSupMap) throws IOException {
        super(_file,_config, _grammar, _xmlCharacters);
        patSupMap = _patSupMap;
    }

    @Override
    protected void openOutputFile() throws IOException {
        super.openOutputFile();
        out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n\n");
        out.write("<results>\n\n");
    }

    @Override
    public void close() throws IOException {
        out.write("</results>\n");
        out.flush();
        out.close();
    }

    @Override
    public void printPattern(String _pat){
        ArrayList<String> pat = new ArrayList<>();
        try{

            String[] strTmp = _pat.split("\t");
            String[] supports = strTmp[0].split(",");

            //String[] pattern = strTmp[1].substring(1,strTmp[1].length()-1).split(",");
            String[] pattern = strTmp[1].split(",");

            for(int i=0; i<pattern.length;++i)
                pat.add(pattern[i].trim());//replaceAll(String.valueOf(uniChar),","));

            //remove right-path missed real leafs
            pat = Pattern.removeMissingLeaf(pat);

            Projected projected = new Projected();
            projected.setProjectedSupport(Integer.valueOf(supports[1]));
            projected.setProjectedRootSupport(Integer.valueOf(supports[2]));

            report_Str(pat,projected);

        }
        catch (Exception e){
            System.out.println("print xml error : " + e);
            System.out.println(pat);

        }
    }

    //new report function to print pattern in the form of Integer
    public void report_Int(ArrayList<String> pat, String supports){
        try{
            //increase number of output pattern
            ++nbPattern;

            //keep meta-variables in pattern
            Map<String,Integer> metaVariable = new HashMap<>();

            //print support, wsupport, size
            String[] sup = supports.split(",");
            if(config.get2Class()) {
                out.write("<subtree id=\"" + nbPattern + "\" support=\"" + sup[0] +
                        "\" score=\"" + sup[1] + "\" size=\"" + sup[2] + "\">\n");
            }else
                out.write("<subtree id=\"" + nbPattern + "\" support=\"" + sup[0] +
                        "\" wsupport=\"" + sup[1] + "\" size=\"" + sup[2] + "\">\n");

            //print pattern
            int n = 0;
            ArrayList < String > tmp = new ArrayList<>();
            //number of meta-variable ???
            for ( int i = 0; i < pat.size () - 1; ++i) {
                //open a node
                if (!pat.get(i).equals(")") && !pat.get(i + 1).equals(")") ) {

                    String nodeOrder = grammar.get(pat.get(i)).get(0);
                    String nodeDegree = grammar.get(pat.get(i)).get(1);
                    ArrayList<String> childrenList = Pattern.findChildrenLabels(pat,i);

                    if(nodeOrder.equals("unordered")){
                        switch (nodeDegree){
                            case "1": //output leaf node
                                switch (childrenList.size()){
                                    case 0:
                                        out.write("<" + pat.get(i) + ">\n");
                                        String metaLabel = getMetaLabel(pat, metaVariable, i);
                                        writeMetaVariableLeaf(metaLabel);
                                        break;
                                    default:
                                        out.write("<" + pat.get(i) + ">\n");
                                        break;
                                }
                                break;
                            case "1..*":
                                writeMatchSetDirectivesNode(pat, i);
                                break;

                            default:
                                out.write("<" + pat.get(i)+">\n");
                                break;
                        }
                    }else{
                        switch (nodeDegree){
                            case "1": //output leaf node
                                switch (childrenList.size()){
                                    case 0:
                                        out.write("<" + pat.get(i) + ">\n");
                                        String metaLabel = getMetaLabel(pat, metaVariable, i);
                                        writeMetaVariableLeaf(metaLabel);
                                        break;
                                    default:
                                        out.write("<" + pat.get(i) + ">\n");
                                        break;
                                }
                                break;

                            default: //N children: if this node has full children
                                writeInternalNode(pat, i);
                                break;
                        }
                    }
                    tmp.add(pat.get(i));
                    ++n;
                }else {
                    //print leaf node of subtree
                    if (!pat.get(i).equals(")") && pat.get(i + 1).equals(")")) {
                        if (pat.get(i).charAt(0) == '*') {
                            outputLeaf(pat, i);
                        } else { //leaf of subtree is an internal node in the original tree
                            outputNode(pat, metaVariable, i);
                        }
                    } else {
                        //close a node
                        if (pat.get(i).equals(")") && pat.get(i + 1).equals(")")) {
                            out.write("</" + tmp.get(n - 1) + ">\n");
                            tmp.remove(n-1);
                            --n;
                        }
                    }
                }
            }
            //print the last node of pattern
            if(pat.get(pat.size() - 1).charAt(0) == '*')  {
                outputLeaf(pat,pat.size() - 1);
            }else {
                int i = pat.size() - 1;
                outputNode(pat, metaVariable, i);
            }
            //close nodes
            for (int i = n - 1; i >= 0; --i)
                out.write( "</" + tmp.get(i) + ">\n");
            //close subtree
            out.write("</subtree>\n");
            out.write("\n");
        }
        catch (Exception e){
            System.out.println("report xml error : " + e);
            System.out.println(pat);
        }
    }

    //print pattern in form of string
    public void report_Str(ArrayList<String> pat, Projected projected){
        try{
            //if( checkOutputConstraint(pat) ) return;
            //System.out.print(pat);
            ++nbPattern;

            //keep meta-variables in pattern
            Map<String,Integer> metaVariable = new HashMap<>();
            //print support, wsupport, size
            if(config.postProcess() && !patSupMap.isEmpty()){
                String patTemp = Pattern.getPatternString(pat);
                String[] sup = patSupMap.get(patTemp).split(",");
                int size = Pattern.getPatternSize(pat);
                out.write("<subtree id=\"" + nbPattern + "\" support=\"" + sup[1] +
                        "\" wsupport=\"" + sup[2] + "\" size=\"" + size + "\">\n");
            }
            else{
                int sup = projected.getProjectedSupport();
                int wsup = projected.getProjectedRootSupport();
                int size = Pattern.getPatternSize(pat);
                out.write("<subtree id=\""+ nbPattern+ "\" support=\"" + sup +
                        "\" wsupport=\"" + wsup + "\" size=\"" + size + "\">\n");
            }
            //print pattern
            int n = 0;
            ArrayList < String > tmp = new ArrayList<>();
            //number of meta-variable ???
            for ( int i = 0; i < pat.size () - 1; ++i) {
                //open a node
                if (!pat.get(i).equals(")") && !pat.get(i + 1).equals(")") ) {

                    String nodeOrder = grammar.get(pat.get(i)).get(0);
                    String nodeDegree = grammar.get(pat.get(i)).get(1);
                    ArrayList<String> childrenList = Pattern.findChildrenLabels(pat,i);

                    if(nodeOrder.equals("unordered")){
                        switch (nodeDegree){
                            case "1":
                                switch (childrenList.size()){
                                    case 0:
                                        out.write("<" + pat.get(i) + ">\n");
                                        String metaLabel = getMetaLabel(pat, metaVariable, i);
                                        writeMetaVariableLeaf(metaLabel);
                                        break;

                                    default:
                                        out.write("<" + pat.get(i) + ">\n");
                                        break;
                                }
                                break;

                            case "1..*":
                                writeMatchSetDirectivesNode(pat, i);
                                break;

                            default:
                                out.write("<" + pat.get(i)+">\n");
                                break;
                        }
                    }
                    else{
                        switch (nodeDegree){
                            case "1":
                                switch (childrenList.size()){
                                    case 0:
                                        out.write("<" + pat.get(i) + ">\n");
                                        String metaLabel = getMetaLabel(pat, metaVariable, i);
                                        writeMetaVariableLeaf(metaLabel);
                                        break;
                                    default:
                                        out.write("<" + pat.get(i) + ">\n");
                                        break;
                                }
                                break;
                            default: //N children: if this node has full children
                                writeInternalNode(pat, i);
                                break;
                        }
                    }
                    tmp.add(pat.get(i));
                    ++n;
                }else {
                    //print leaf node of subtree
                    if (!pat.get(i).equals(")") && pat.get(i + 1).equals(")")) {
                        //TODO: abstracting leafs of Cobol data
                        if (pat.get(i).charAt(0) == '*') {
                            outputLeaf(pat, i);
                        } else { //leaf of subtree is an internal node in the original tree
                            outputNode(pat, metaVariable, i);
                        }
                    } else {
                        //close a node
                        if (pat.get(i).equals(")") && pat.get(i + 1).equals(")")) {
                            out.write("</" + tmp.get(n - 1) + ">\n");
                            tmp.remove(n-1);
                            --n;
                        }
                    }
                }
            }
            //print the last node of pattern
            if(pat.get(pat.size() - 1).charAt(0) == '*')  {
                outputLeaf(pat,pat.size() - 1);
            }
            else {
                int i = pat.size() - 1;
                outputNode(pat, metaVariable, i);
            }
            //close nodes
            //System.out.println(tmp);
            for (int i = n - 1; i >= 0; --i)
                out.write( "</" + tmp.get(i) + ">\n");

            out.write("</subtree>\n");
            out.write("\n");

        }
        catch (Exception e){
            System.out.println("report xml error : " + e);
            System.out.println(pat);

        }
    }

    // write a leaf node
    private void outputLeaf(ArrayList<String> pat, int i) throws IOException{
        if (config.getAbstractLeafs() && !reservedLabels.contains(pat.get(i)) ){
            writeMetaVariableLeaf(pat.get(i-1));
        }else{
            writeLeafLabel(pat.get(i));
        }
    }

    // write a real leaf node
    private void writeLeafLabel(String leaf) throws IOException {
        for(int t=1; t<leaf.length(); ++t) {
            if (xmlCharacters.containsKey(String.valueOf(leaf.charAt(t))))
                out.write(xmlCharacters.get(String.valueOf(leaf.charAt(t))));
            else
                out.write(leaf.charAt(t));
        }
    }

    // write a meta variable node
    private void writeMetaVariableLeaf(String metaVariable) throws IOException {
        out.write("<Dummy>\n");
        out.write("<__directives>\n");
        out.write("<optional />\n");
        out.write("<meta-variable>\n");
        out.write("<parameter key=\"name\" value=\"?"+metaVariable+"\"/>\n");
        out.write("</meta-variable>\n");
        out.write("</__directives>\n");
        out.write("</Dummy>\n");
    }

    // write an internal node
    private void outputNode(ArrayList<String> pat, Map<String, Integer> metaVariable, int i) throws IOException {
        String nodeOrder = grammar.get(pat.get(i)).get(0);
        String nodeDegree = grammar.get(pat.get(i)).get(1);
        if(nodeOrder.equals("unordered")){
            switch (nodeDegree){
                case "1":
                    out.write("<" + pat.get(i) + ">\n");
                    String metaLabel = getMetaLabel(pat, metaVariable, i);
                    writeMetaVariableLeaf(metaLabel);
                    out.write("</" + pat.get(i) + ">\n");
                    break;
                case "1..*":
                    writeMatchSetDirectivesNode(pat, i);
                    break;

                default:
                    out.write("<" + pat.get(i)+"/>\n");
                    break;

            }
        }
        else{
            writeCloseNode(pat.get(i));
        }
    }

    // write an internal node which has n ordered children
    private void writeInternalNode(ArrayList<String> pat, int i) throws IOException {
        out.write("<" + pat.get(i) + ">\n");
        out.write("<__directives>");
        out.write("<match-sequence/>");
        out.write("</__directives>\n");
    }

    // write an internal node which has n unordered children
    private void writeMatchSetDirectivesNode(ArrayList<String> pat, int i) throws IOException {
        out.write("<" + pat.get(i)+">\n");
        out.write("<__directives>");

        if(pat.get(i).equals("TheBlocks") && pat.get(i-1).equals("SectionStatementBlock"))
            out.write("<match-succession/>");
        else
            //out.write("<match-set/>");
            out.write("<match-sequence/>");

        out.write("</__directives>\n");
    }

    // write an closed internal node which has n ordered children
    private void writeCloseNode(String nodeName) throws IOException {
        out.write("<" + nodeName + ">\n");
        out.write("<__directives>");
        out.write("<match-sequence/>");
        out.write("</__directives>\n");
        out.write("</" + nodeName + ">\n");
    }

    private String getMetaLabel(ArrayList<String> pat, Map<String, Integer> metaVariable, int i) {
        String metaLabel;
        if(metaVariable.containsKey(pat.get(i))){
            metaVariable.put(pat.get(i), metaVariable.get(pat.get(i))+1);
            metaLabel = pat.get(i)+String.valueOf(metaVariable.get(pat.get(i)));
        }else{
            metaLabel = pat.get(i)+"1";
            metaVariable.put(pat.get(i),1);
        }
        return metaLabel;
    }

}
