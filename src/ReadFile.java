import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.Vector;
import java.io.FileReader;
import java.io.BufferedReader;

public class ReadFile {

    public void createTransaction (String path, Vector < Vector<NodeFreqT> > trans)
    {
        String file = path;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if( ! line.isEmpty() )
                {
                    String[] str_tmp = line.split("\t");
                    String str = str_tmp[1];
                    Vector <NodeFreqT> tmp = new Vector<>();
                    str2node(str,tmp);
                    trans.addElement(tmp);
                }
            }
        }catch (IOException e) {System.out.println("Reading file error ");}

    }

    //create transaction for freqt-post
    public void createTransactionNew(Set< String > inPatterns,
                                     Vector<Vector<NodeFreqT>> trans,
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

                Vector <NodeFreqT> tran_tmp = new Vector<>();
                str2node(str_temp[1],tran_tmp);
                trans.addElement(tran_tmp);
            }
        }
    }


    private void str2node(String str, Vector<NodeFreqT> node){

        try{
            int len = str.length();
            int size = 0;
            String buff = ""; //individual node
            Vector<String> tmp = new Vector<>(); //a list of node

            int ii=0;
            while (ii<len){
            //for(int i = 0; i < len; ++i) //for each char in the str
                //if str.chatAt(i) =='(' open a node
                //if str.chatAt(i) ==')' close a node of a branch
                if(str.charAt(ii) == '(' || str.charAt(ii) == ')'){
                    if(! buff.isEmpty()){
                        if(buff.charAt(0)=='*'){
                            tmp.addElement(buff);
                        }
                        else{
                            String[] label = buff.split("_");
                            tmp.addElement(label[0]);
                        }
                        buff = "";
                        ++size;
                    }
                    if(str.charAt(ii) == ')') tmp.addElement(")");
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
            node.setSize(size);
            Vector<Integer> sibling = new Vector<>(size);
            sibling.setSize(size);
            for(int i = 0; i < size; ++i){
                NodeFreqT n = new NodeFreqT();
                n.setNodeSibling(-1);
                n.setNodeParent(-1);
                n.setNodeChild(-1);
                node.setElementAt(n,i);
                //node.elementAt(i).setNodeChild(-1);
                //node.elementAt(i).setNodeParent(-1);
                //node.elementAt(i).setNodeSibling(-1);
                sibling.setElementAt(-1,i);

            }

            //create tree
            Vector<Integer> sr = new Vector<>();
            int id = 0;
            int top = 0;

            for(int i = 0; i< tmp.size(); ++i){
                if( tmp.elementAt(i).equals(")") ){
                    top = sr.size() - 1;
                    if (top < 1) continue;
                    int child = sr.elementAt(top);
                    int parent = sr.elementAt(top-1);
                    node.elementAt(child).setNodeParent(parent);
                    if(node.elementAt(parent).getNodeChild() == -1)
                        node.elementAt(parent).setNodeChild(child);
                    if(sibling.elementAt(parent) != -1)
                        node.elementAt(sibling.elementAt(parent)).setNodeSibling(child);
                    sibling.setElementAt(child,parent);
                    sr.setSize(top);
                }
                else {
                    node.elementAt(id).setNodeLabel(tmp.elementAt(i));
                    sr.addElement(id);
                    id ++;
                }
            }

        }catch (Exception e) {
            System.out.println("Fatal: parse error << ["+ str +"]\n");
            System.exit(-1);
        }
    }

}
