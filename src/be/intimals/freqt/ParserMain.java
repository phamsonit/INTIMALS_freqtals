package be.intimals.freqt;

import be.intimals.freqt.core.FreqT;
import be.intimals.freqt.grammar.PCFG;
import be.intimals.freqt.grammar.Parser;

import java.io.IOException;

public class ParserMain {
    /**
     * Build a PCFG and run the parser on the data (Java ASTs)
     * @param args
     */
    public static void main(String[] args) {
        try {
            String configPath = args.length == 0 ? "conf/config.properties" : args[0];

            Config config = new Config(configPath);

            long start = System.currentTimeMillis();

            PCFG pcfg = new PCFG();
            pcfg.loadGrammar(config.getGrammarFile());
            Parser parser = new Parser(pcfg,"CompilationUnit");
            parser.parseDirectory(".\\data\\java_AST\\jhotdrawParser");

            long end = System.currentTimeMillis();
            long diff = end - start;
            System.out.println("running time : " + diff + " ms");

        } catch (IOException e) {
            System.out.println("Config file not found");
        } catch (Exception e) {
            System.err.println("Exception: ");
            e.printStackTrace();
        }
    }
}
