package be.intimals.freqt.input;

import be.intimals.freqt.structure.*;

import java.io.IOException;
import java.util.*;
import java.io.FileReader;
import java.io.BufferedReader;

public class ReadFile {
/*
    //create transaction data for a XML file, each line is a input tree
    public void createTransaction (String path, ArrayList< ArrayList<NodeFreqT> > trans){
        String file = path;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if( ! line.isEmpty() )
                {
                    String[] str_tmp = line.split("\t");
                    String str = str_tmp[1];
                    ArrayList <NodeFreqT> tmp = new ArrayList<>();
                    str2node(str,tmp);
                    trans.add(tmp);
                }
            }
        }catch (IOException e) {System.out.println("Reading file error ");}
    }*/

    //create transaction from Map < pattern, supports>
    public void createTransactionFromMap(Map<String, String > inPatterns,
                                         ArrayList<ArrayList<NodeFreqT>> trans){

        Iterator <Map.Entry<String,String> > iterMap = inPatterns.entrySet().iterator();
        while(iterMap.hasNext()){
            for(int i=0; i<inPatterns.size(); ++i){
                Map.Entry<String,String> temp = iterMap.next();
                String str_pattern = temp.getKey();
                ArrayList <NodeFreqT> tran_tmp = new ArrayList<>();
                str2node(str_pattern,tran_tmp);
                trans.add(tran_tmp);
            }
        }
    }

/*
    //create transaction from Map < pattern, supports>
    public void createTransactionFromMap(Map<String, String > inPatterns,
                                         ArrayList<ArrayList<NodeFreqT>> trans,
                                     Map<String,String> patSup,
                                     Set<String> rootLabel){

        Iterator <Map.Entry<String,String> > iterMap = inPatterns.entrySet().iterator();
        while(iterMap.hasNext()){
            for(int i=0; i<inPatterns.size(); ++i){
                Map.Entry<String,String> temp = iterMap.next();

                String str_pattern = temp.getKey();
                String str_sup = temp.getValue();

                patSup.put(str_pattern,str_sup);

                String root = "";
                for(int j=1;j<str_pattern.length();++j) {
                    if (str_pattern.charAt(j)=='(')
                        break;
                    else
                        root += str_pattern.charAt(j);
                }
                rootLabel.add(root);

                ArrayList <NodeFreqT> tran_tmp = new ArrayList<>();
                str2node(str_pattern,tran_tmp);
                trans.add(tran_tmp);
            }
        }
    }*/
/*
    //create transaction for freqt-post
    public void createTransactionFromSet(Set< String > inPatterns,
                                         ArrayList<ArrayList<NodeFreqT>> trans,
                                     Map<String,String> patSup,
                                     Set<String> rootLabel){

        Iterator<String> iterSet = inPatterns.iterator();
        while(iterSet.hasNext()){
            for(int i=0; i<inPatterns.size(); ++i){
                String temp = iterSet.next();
                String[] str_temp = temp.split("\t");
                patSup.put(str_temp[1],str_temp[0]);

                String root = "";
                for(int j=1;j<str_temp[1].length();++j) {
                    if (str_temp[1].charAt(j)=='(')
                        break;
                    else
                        root += str_temp[1].charAt(j);
                }
                rootLabel.add(root);

                ArrayList <NodeFreqT> tran_tmp = new ArrayList<>();
                str2node(str_temp[1],tran_tmp);
                trans.add(tran_tmp);
            }
        }
    }
*/

    private void str2node(String str, ArrayList<NodeFreqT> trans){

        //try{
            int len = str.length();
            int size = 0;
            String buff = ""; //individual node
            ArrayList<String> tmp = new ArrayList<>(); //a list of node

            int ii=0;
            while (ii<len){
            //for(int i = 0; i < len; ++i) //for each char in the str
                //if str.chatAt(i) =='(' open a node
                //if str.chatAt(i) ==')' close a node of a branch
                if(str.charAt(ii) == '(' || str.charAt(ii) == ')'){
                    if(! buff.isEmpty()){
                        if(buff.charAt(0)=='*'){
                            tmp.add(buff);
                        }
                        else{
                            String[] label = buff.split("_");
                            tmp.add(label[0]);
                        }
                        buff = "";
                        ++size;
                    }
                    if(str.charAt(ii) == ')') tmp.add(")");
                }
                else
                    if(str.charAt(ii) == '\t' || str.charAt(ii) == ' ') {buff += "_";}
                    else {
                    //adding to find leaf node i.e. *X(120)
                        if (str.charAt(ii)=='*'){
                            int bracket=0;
                            while(bracket >= 0){

                                if(str.charAt(ii)=='(')
                                    bracket++;
                                else
                                    if(str.charAt(ii)==')')
                                        bracket--;

                                if(bracket==-1)
                                    break;
                                else {
                                    buff += str.charAt(ii);
                                    ++ii;
                                }
                            }
                            //System.out.println(buff);
                            --ii;
                        }else buff += str.charAt(ii);
                    }
                ++ii;
            }

            if (! buff.isEmpty()) throw new ArithmeticException("error !");

            //init a list of node
            //trans.setSize(size);
            ArrayList<Integer> sibling = new ArrayList<>(size);
            //sibling.setSize(size);
            for(int i = 0; i < size; ++i){
                NodeFreqT nodeTemp = new NodeFreqT();
                nodeTemp.setNodeSibling(-1);
                nodeTemp.setNodeParent(-1);
                nodeTemp.setNodeChild(-1);
                trans.add(nodeTemp);
                //node.elementAt(i).setNodeChild(-1);
                //node.elementAt(i).setNodeParent(-1);
                //node.elementAt(i).setNodeSibling(-1);
                sibling.add(-1);

            }
            //create tree
            ArrayList<Integer> sr = new ArrayList<>();
            int id = 0;
            int top = 0;

            for(int i = 0; i< tmp.size(); ++i){
                if( tmp.get(i).equals(")") ){
                    top = sr.size() - 1;
                    if (top < 1) continue;
                    int child = sr.get(top);
                    int parent = sr.get(top-1);
                    trans.get(child).setNodeParent(parent);
                    if(trans.get(parent).getNodeChild() == -1)
                        trans.get(parent).setNodeChild(child);
                    if(sibling.get(parent) != -1)
                        trans.get(sibling.get(parent)).setNodeSibling(child);
                    sibling.set(parent,child);
                    sr.remove(top);
                }
                else {
                    trans.get(id).setNodeLabel(tmp.get(i));
                    sr.add(id);
                    id ++;
                }
            }

        /*}catch (IOException e) {
            System.out.println("Fatal: parse error << ["+ str +"]\n");
            e.printStackTrace();
            System.exit(-1);
        }*/
    }

}
