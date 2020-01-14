package be.intimals.freqt.output;

import be.intimals.freqt.structure.*;
import be.intimals.freqt.config.*;


import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import be.intimals.freqt.config.*;

public class LineOutput extends AOutputFormatter {
    private char uniChar;
    //Map<String,String> patSupMap = new LinkedHashMap<>();;
/*
    public LineOutput(String _file,Config _config, Map<String, Vector<String>> _grammar, Map<String,String> _xmlCharacters, char _uniChar) throws IOException {
        super(_file,_config, _grammar,_xmlCharacters);
        uniChar = _uniChar;
    }*/

    public LineOutput(String _file, Config _config, Map<String, ArrayList<String>> _grammar, Map<String,String> _xmlCharacters, Map<String,String> _patSupMap, char _uniChar) throws IOException {
        super(_file,_config, _grammar,_xmlCharacters);
        uniChar = _uniChar;
        this.patSupMap = _patSupMap;
    }

    /**
     * Output subtrees as string format
     * @param pat
     * @param projected
     */
    //@Override
    private void report(ArrayList<String> pat, Projected projected){
        try{

            if(checkOutputConstraint(pat)) return;
            ++nbPattern;

            if(config.postProcess() && !patSupMap.isEmpty()){
                String patTemp = Pattern.getPatternString(pat);
                String[] sup = patSupMap.get(patTemp).split(",");
                int size = Pattern.getPatternSize(pat);
                //out.write("supp:"+sup[0]+" wsupp:"+sup[1]+" size:"+sup[2]+"\t");
                out.write("rootIDs\t " + sup[0] + "\t supp:" + sup[1] + "\t wsup:" + sup[2] + " size:" + size + "\t");
            }
            else{
                int sup = projected.getProjectedSupport();
                int wsup = projected.getProjectedRootSupport();
                int size = Pattern.getPatternSize(pat);

                //List<Integer> allOccurrences = getSizeAllOccurrences(projected);

                assert(size == projected.getProjectLocation(0).getLocationList().size());
//                assert(size == projected.getProjectLocation(0).getLocationList().size());

                //System.out.println(FreqT.getPatternString(pat));
                //printAllOccurrence(projected);

                //out.write("occurrences:"+allOccurrences.size()+" supp:"+sup + " rsupp:" + rsup +" size:" + size + "\t");
                out.write("supp:"+ sup  + " wsup:" + wsup +" size:" + size + "\t");
            }

/*            if(config.outputAsENC()){
                for(int i = 0; i < pat.size(); ++i){
                    if(i != 0) out.write(" ");
                    //System.out.print(pat.elementAt(i));
                    out.write(pat.elementAt(i).replace(uniChar,'-'));
                }
            }
            else{*/
                int n = 0;
                for(int i = 0; i<pat.size(); ++i){
                    if(pat.get(i).equals(")")) {
                        out.write(pat.get(i));
                        --n;
                    }
                    else{
                        ++n;
                        out.write("(" + pat.get(i));
                    }
                }
                for(int i = 0 ; i < n; ++i) {
                    out.write(")");
                }
                out.write("\n");

            /*}
            //output locations
            if(config.addLocations()){
                out.write("Locations : ");
                out.write(projected.getProjectLocation(0).getLocationId()+" ");
                for(int i=0; i<projected.getProjectLocationSize()-1; ++i){
                    Location tmp1 = projected.getProjectLocation(i);
                    Location tmp2 = projected.getProjectLocation(i+1);
                    if(tmp1.getLocationId() != tmp2.getLocationId())
                        out.write(tmp2.getLocationId()+" ");
                }
                out.write("\n");
            }*/

        }catch (IOException e) {
            System.out.println("report error");
        }
    }

    @Override
    public void printPattern(String _pat){
        try{

            String[] strTmp = _pat.split("\t");
            String[] supports = strTmp[0].split(",");
            String[] pattern = strTmp[1].substring(1,strTmp[1].length()-1).split(",");

            ArrayList<String> pat = new ArrayList<>();
            for(int i=0; i<pattern.length;++i)
                pat.add(pattern[i].trim());

            Projected projected = new Projected();
            projected.setProjectedSupport(Integer.valueOf(supports[0]));
            projected.setProjectedRootSupport(Integer.valueOf(supports[1]));

            report(pat,projected);

        }
        catch (Exception e){
            System.out.println("report String error : " + e);
            System.out.println(_pat);

        }

    }

    @Override
    public void close() throws IOException {
        out.flush();
        out.close();
    }
}
