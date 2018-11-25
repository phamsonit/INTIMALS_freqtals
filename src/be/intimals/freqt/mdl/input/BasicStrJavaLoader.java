package be.intimals.freqt.mdl.input;

import org.w3c.dom.Node;
import java.util.logging.Logger;

/**
 * Loads a Database from some ASTs in a given XML format. This loader works with Java
 * and loads extracts everything as-is.
 */
public class BasicStrJavaLoader extends ABasicJavaLoader<String> {
    private static final Logger LOGGER = Logger.getLogger(BasicStrJavaLoader.class.getName());

    @Override
    protected String getKeyForNode(Node current) {
        String key = current.getNodeType() == Node.ELEMENT_NODE ? current.getNodeName() : current.getNodeValue();
        assert (key != null);
        return key;
    }
}
