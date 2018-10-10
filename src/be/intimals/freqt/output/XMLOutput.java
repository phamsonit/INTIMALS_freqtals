package be.intimals.freqt.output;

import be.intimals.freqt.Config;
import be.intimals.freqt.core.Pattern;
import be.intimals.freqt.core.Projected;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class XMLOutput extends AOutputFormatter {

    public XMLOutput(Config newConfig, Map<String, Vector<String>> newGrammar) throws IOException {
        super(newConfig, newGrammar);
    }

    @Override
    protected void openOutputFile() throws IOException {
        super.openOutputFile();
        out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n\n");
        out.write("<results>\n\n");
    }

    /**
     * Represent subtrees with XML format + Ekeko.
     *
     * @param pat
     * @param projected
     */
    @Override
    public void report(Vector<String> pat, Projected projected) {
        try {
            int size = Pattern.getPatternSize(pat);
            int sup = projected.getProjectedSupport();
            int wsup = projected.getProjectLocationSize();

            assert (Pattern.checkConstraints(config, pat));

            ++nbPattern;

            //track meta-variables
            Map<String, Integer> metaVariable = new HashMap<>();

            out.write("<subtree id=\"" + nbPattern + "\" support=\"" + sup
                    + "\" wsupport=\"" + wsup + "\" size=\"" + size + "\">\n");
            //System.out.println(nbPattern);
            int n = 0;
            Vector<String> tmp = new Vector<>();
            //number of meta-variable ???
            for (int i = 0; i < pat.size() - 1; ++i) {
                //open a node
                if (!pat.elementAt(i).equals(")") && !pat.elementAt(i + 1).equals(")")) {
                    String nodeOrder = grammar.get(pat.elementAt(i)).elementAt(0);
                    String nodeDegree = grammar.get(pat.elementAt(i)).elementAt(1);
                    Vector<String> childrenList = Pattern.findChildren(pat, i);

                    if (nodeOrder.equals("unordered")) {
                        switch (nodeDegree) {
                            case "1":
                                switch (childrenList.size()) {
                                    case 0:
                                        String metaLabel = getMetaLabel(pat, metaVariable, i);

                                        out.write("<" + pat.elementAt(i) + ">\n");
                                        out.write("<Dummy>\n");
                                        out.write("<__directives>\n");
                                        out.write("<optional />\n");
                                        out.write("<meta-variable>\n");
                                        out.write("<parameter key=\"name\" value=\"?" + metaLabel + "\"/>\n");
                                        out.write("</meta-variable>\n");
                                        out.write("</__directives>\n");
                                        out.write("</Dummy>\n");

                                        break;
                                    default:
                                        out.write("<" + pat.elementAt(i) + ">\n");
                                        break;
                                }
                                break;

                            case "1..*":
                                out.write("<" + pat.elementAt(i) + ">\n");
                                out.write("<__directives>");
                                out.write("<match-set/>");
                                out.write("</__directives>\n");
                                break;
                            default:
                                out.write("<" + pat.elementAt(i) + ">\n");
                                //out.write("<__directives>");
                                //out.write("<match-set/>");
                                //out.write("</__directives>\n");
                                break;
                        }
                    } else {
                        out.write("<" + pat.elementAt(i) + ">\n");
                        out.write("<__directives>");
                        out.write("<match-sequence/>");
                        out.write("</__directives>\n");
                    }

                    //track open node label
                    //System.out.println(tmp);
                    tmp.addElement(pat.elementAt(i));
                    ++n;
                } else {
                    //print leaf node of subtree
                    if (!pat.elementAt(i).equals(")") && pat.elementAt(i + 1).equals(")")) {
                        if (pat.elementAt(i).charAt(0) == '*') {
                            //if(pat.elementAt(i).equals("*") )  {
                            out.write(pat.elementAt(i).substring(1) + "\n");
                        } else { //leaf of subtree is an internal node in the original tree
                            outputNode(pat, metaVariable, i);
                        }
                    } else {
                        //close a node
                        if (pat.elementAt(i).equals(")") && pat.elementAt(i + 1).equals(")")) {
                            out.write("</" + tmp.elementAt(n - 1) + ">\n");
                            //tmp.remove(tmp.lastElement());
                            tmp.remove(n - 1);
                            --n;
                        }
                    }
                }
            }

            //print the last node of pattern
            if (pat.elementAt(pat.size() - 1).charAt(0) == '*') {
                out.write(pat.elementAt(pat.size() - 1).substring(1) + "\n");
            } else {
                int i = pat.size() - 1;
                outputNode(pat, metaVariable, i);
            }

            //close nodes
            //System.out.println(tmp);
            for (int i = n - 1; i >= 0; --i) {
                out.write("</" + tmp.elementAt(i) + ">\n");
            }

            out.write("</subtree>\n");
            out.write("\n");

        } catch (Exception e) {
            System.out.println("report xml error : " + e);
            System.out.println(pat);

        }

    }

    @Override
    public void close() throws IOException {
        out.write("</results>\n");
        out.flush();
        out.close();
    }

    private void outputNode(Vector<String> pat, Map<String, Integer> metaVariable, int i) throws IOException {
        String nodeOrder = grammar.get(pat.elementAt(i)).elementAt(0);
        String nodeDegree = grammar.get(pat.elementAt(i)).elementAt(1);
        if (nodeOrder.equals("unordered")) {
            switch (nodeDegree) {
                case "1":
                    String metaLabel = getMetaLabel(pat, metaVariable, i);

                    out.write("<" + pat.elementAt(i) + ">\n");
                    out.write("<Dummy>\n");
                    out.write("<__directives>\n");
                    //out.write("<wildcard />");
                    out.write("<optional />\n");
                    out.write("<meta-variable>\n");
                    out.write("<parameter key=\"name\" value=\"?" + metaLabel + "\"/>\n");
                    out.write("</meta-variable>\n");
                    out.write("</__directives>\n");
                    out.write("</Dummy>\n");
                    out.write("</" + pat.elementAt(i) + ">\n");
                    break;

                case "1..*":
                    out.write("<" + pat.elementAt(i) + ">\n");
                    out.write("<__directives>");
                    out.write("<match-set/>");
                    out.write("</__directives>\n");
                    out.write("</" + pat.elementAt(i) + ">\n");
                    break;
                default:
                    out.write("<" + pat.elementAt(i) + "/>\n");
                    //out.write("<__directives>");
                    //out.write("<match-set/>");
                    //out.write("</__directives>\n");
                    //out.write("</" + pat.elementAt(i)+">\n");
                    break;
            }
        } else {
            out.write("<" + pat.elementAt(i) + ">\n");
            out.write("<__directives>");
            out.write("<match-sequence/>");
            out.write("</__directives>\n");
            out.write("</" + pat.elementAt(i) + ">\n");
        }
    }

    private String getMetaLabel(Vector<String> pat, Map<String, Integer> metaVariable, int i) {
        String metaLabel;
        if (metaVariable.containsKey(pat.elementAt(i))) {
            metaVariable.put(pat.elementAt(i), metaVariable.get(pat.elementAt(i)) + 1);
            metaLabel = pat.elementAt(i) + String.valueOf(metaVariable.get(pat.elementAt(i)));
        } else {
            metaLabel = pat.elementAt(i) + "1";
            metaVariable.put(pat.elementAt(i), 1);
        }
        return metaLabel;
    }
}
