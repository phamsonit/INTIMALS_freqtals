package be.intimals.freqt.output;

import be.intimals.freqt.Config;
import be.intimals.freqt.core.Projected;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public abstract class AOutputFormatter {
    Config config;
    Map<String, Vector<String>> grammar;
    FileWriter out;
    int nbPattern;

    /**
     * Abstract class used for formatting the patterns as output.
     * @param newConfig
     * @param newGrammar
     * @throws IOException
     */
    public AOutputFormatter(Config newConfig, Map<String, Vector<String>> newGrammar) throws IOException {
        config = newConfig;
        grammar = newGrammar;
        openOutputFile();
        nbPattern = 0;
    }

    protected void openOutputFile() throws IOException {
        out = new FileWriter(config.getOutputFile());
    }

    public void flush() throws IOException {
        out.flush();
    }

    public abstract void report(Vector<String> pat, Projected projected);

    public abstract void close() throws IOException;
}
