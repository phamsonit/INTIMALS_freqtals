package be.intimals.freqt.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Util {

    static  Vector<String> pattern = new Vector<>();
    // TODO (The method that uses this field is currently unused.) This field is error-prone though because it's static and we're running Freq-T in parallel!

    private static Vector<String> trans = new Vector<>();
    // TODO (The method that uses this field is currently unused.) This field is error-prone though because it's static and we're running Freq-T in parallel!


    //create transaction for item set mining algorithm
    public static void createTransaction(String inputPath, String outputPath){

        String file = inputPath;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if( ! line.isEmpty() )
                {
                    String[] str_tmp = line.split("\t");
                    String fileIDs = str_tmp[1];
                    //System.out.println(fileIDs);
                    trans.add(fileIDs);
                }
            }
        }catch (IOException e) {System.out.println("Reading file error ");}

        int rows = 450;
        int columns = trans.size();
        List<List<Integer>> matrix = new LinkedList<>();
        for(int i=0; i<rows; ++i){
            List<Integer> tmpList = new LinkedList<>();
            for(int j=0; j<columns; ++j){
                tmpList.add(-1);
            }
            matrix.add(tmpList);
        }

        for(int i=0; i<trans.size(); ++i){
            //System.out.println(trans.elementAt(i));
            String[] tmpStr = trans.elementAt(i).split(",");
            for(int j=0; j<tmpStr.length; ++j){
                matrix.get(Integer.valueOf(tmpStr[j].trim())).set(i,Integer.valueOf(i));
            }
        }

        try{
            FileWriter out = new FileWriter(outputPath);
            for(int i=0; i< rows; ++i){
                String tmp="";
                for(int j=0;j<columns;++j){
                    if(matrix.get(i).get(j) != -1)
                        tmp += matrix.get(i).get(j)+" ";
                }
                if(!tmp.isEmpty()) {
                    //System.out.print("transaction: " + i +"\t");
                    //System.out.println(tmp);
                    out.write(tmp+"\n");
                }

            }
            out.close();
        }
        catch (Exception e){}

    }


    //group patterns by leaf labels
    public void groupPattern(String inputFile, String outputFile) {

        //readPattern(path);
        //String file = inputFile;
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty())
                    pattern.add(line);
            }
        } catch (IOException e) {System.out.println("Error: reading patterns");}

        //find leaf set
        Map<String, String> leafMap = new LinkedHashMap();
        Set<String> leafSet = new LinkedHashSet<>();

        for (int i = 0; i < pattern.size(); ++i) {
            int j = 0;
            String leafStringTmp = "";
            while (j < pattern.elementAt(i).length()) {
                String leafTmp = "";
                int start;
                int end;
                if (pattern.elementAt(i).charAt(j) == '*') {
                    start = j;
                    int bracket = 0;
                    //while (pattern.elementAt(i).charAt(j) != ')') {
                    while(bracket >= 0){
                        if(pattern.elementAt(i).charAt(j)=='(') ++bracket;
                        else if(pattern.elementAt(i).charAt(j)==')') --bracket;
                        ++j;
                    }
                    end = j-1;
                    leafTmp = pattern.elementAt(i).substring(start, end);
                    leafStringTmp += leafTmp;
                } else
                    ++j;
            }
            leafMap.put(pattern.elementAt(i), leafStringTmp);
            leafSet.add(leafStringTmp);
        }


        //output
        try{
            FileWriter out = new FileWriter(outputFile);

            Iterator<String> iterSet = leafSet.iterator();
            while (iterSet.hasNext()) {
                String tmp = iterSet.next();
                out.write(tmp+"\n");
                //System.out.println(tmp);
                Iterator<Map.Entry<String, String>> iter = leafMap.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, String> entry = iter.next();
                    if (entry.getValue().equals(tmp)) {
                        //System.out.println(entry.getKey());
                        out.write(entry.getKey()+"\n");
                    }
                }
            }

            out.write("# leaf sets / patterns " + leafSet.size() + " / " + leafMap.size());
            out.close();
        }
        catch (Exception e){}

        System.out.println("# leaf sets / patterns " + leafSet.size() + " / " + leafMap.size());
    }


    public static Object makeObject(String inputString) {
        StringBuffer buf = new StringBuffer(12);
        buf.append(inputString);
        return buf.toString();
    }


}
