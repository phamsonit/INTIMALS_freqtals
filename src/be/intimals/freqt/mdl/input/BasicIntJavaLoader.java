package be.intimals.freqt.mdl.input;

import org.w3c.dom.Node;
import java.util.*;
import java.util.logging.Logger;

// TODO WIP, Use Str loader for now
/**
 * Loads a Database from some ASTs in a given XML format. This loader works with Java, extracts everything
 * and maps them to integer keys.
 */
public class BasicIntJavaLoader extends ABasicJavaLoader<Integer> {
    private static final Logger LOGGER = Logger.getLogger(BasicIntJavaLoader.class.getName());
    private static Integer keyCounter = 0;

    private Map<String, Integer> vocabMapToInt = new HashMap<>();

    @Override
    protected Integer getKeyForNode(Node current) {
        String key = current.getNodeType() == Node.ELEMENT_NODE ? current.getNodeName() : current.getNodeValue();
        assert (key != null);
        if (vocabMapToInt.containsKey(key)) {
            return vocabMapToInt.get(key);
        } else {
            Integer newIntKey = keyCounter;
            vocabMapToInt.put(key, newIntKey);
            keyCounter++;
            return newIntKey;
        }
    }

    protected void init() {
        super.init();
        keyCounter = 0;
        vocabMapToInt.clear();
    }
}
