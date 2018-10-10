package be.intimals.freqt.input;

import be.intimals.freqt.core.NodeFreqT;

import java.io.IOException;
import java.util.Vector;
import java.io.FileReader;
import java.io.BufferedReader;

public class ReadFile {

    public void createTransaction(String path, Vector<Vector<NodeFreqT>> trans) {
        String file = path;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty()) {
                    String[] str_tmp = line.split("\t");
                    String str = str_tmp[1];
                    Vector<NodeFreqT> tmp = new Vector<>();
                    str2node(str, tmp);
                    trans.addElement(tmp);
                }
            }
        } catch (IOException e) {
            System.out.println("Reading file error ");
        }

    }

    private void str2node(String str, Vector<NodeFreqT> node) {

        try {
            int len = str.length();
            int size = 0;
            String buff = "";
            Vector<String> tmp = new Vector<>();

            for (int i = 0; i < len; ++i) {
                if (str.charAt(i) == '(' || str.charAt(i) == ')') {
                    if (!buff.isEmpty()) {
                        if (buff.charAt(0) == '*') {
                            tmp.addElement(buff);
                        } else {
                            String[] label = buff.split("_");
                            tmp.addElement(label[0]);
                        }
                        buff = "";
                        ++size;
                    }
                    if (str.charAt(i) == ')') tmp.addElement(")");
                } else if (str.charAt(i) == '\t' || str.charAt(i) == ' ') {
                    buff += "_";
                } else {
                    buff += str.charAt(i);
                }
            }

            if (!buff.isEmpty()) throw new ArithmeticException("error !");

            //init a list of node
            node.setSize(size);
            Vector<Integer> sibling = new Vector<>(size);
            sibling.setSize(size);
            for (int i = 0; i < size; ++i) {
                NodeFreqT n = new NodeFreqT();
                n.setNodeSibling(-1);
                n.setNodeParent(-1);
                n.setNodeChild(-1);
                node.setElementAt(n, i);
                //node.elementAt(i).setNodeChild(-1);
                //node.elementAt(i).setNodeParent(-1);
                //node.elementAt(i).setNodeSibling(-1);
                sibling.setElementAt(-1, i);

            }

            //create tree
            Vector<Integer> sr = new Vector<>();
            int id = 0;
            int top = 0;

            for (int i = 0; i < tmp.size(); ++i) {
                if (tmp.elementAt(i).equals(")")) {
                    top = sr.size() - 1;
                    if (top < 1) continue;
                    int child = sr.elementAt(top);
                    int parent = sr.elementAt(top - 1);
                    node.elementAt(child).setNodeParent(parent);
                    if (node.elementAt(parent).getNodeChild() == -1)
                        node.elementAt(parent).setNodeChild(child);
                    if (sibling.elementAt(parent) != -1)
                        node.elementAt(sibling.elementAt(parent)).setNodeSibling(child);
                    sibling.setElementAt(child, parent);
                    sr.setSize(top);
                } else {
                    node.elementAt(id).setNodeLabel(tmp.elementAt(i));
                    sr.addElement(id);
                    id++;
                }
            }

        } catch (Exception e) {
            System.out.println("Fatal: parse error << [" + str + "]\n");
            System.exit(-1);
        }
    }

}
