package be.intimals.freqt;

import be.intimals.freqt.grammar.PCFG;
import be.intimals.freqt.grammar.Parser;
import be.intimals.freqt.mdl.input.BasicStrJavaLoader;
import be.intimals.freqt.mdl.input.Database;
import be.intimals.freqt.mdl.input.IDatabaseLoader;
import be.intimals.freqt.mdl.tsg.*;

import java.io.IOException;
import java.util.Arrays;

public class MainMDL {
    public static void main(String[] args) {
        try {
            String configPath = args.length == 0 ? "conf/config.properties" : args[0];

            Config config = new Config(configPath);

            long start = System.currentTimeMillis();

            IDatabaseLoader<String> loader = new BasicStrJavaLoader();
            //Database<String> db = loader.loadDirectory(".\\data\\test");
            Database<String> db = loader.loadFile(".\\out\\FD_dataset.xml");
            ATSG<String> tsg = new BasicTSG();
            tsg.loadDatabase(db);

            System.out.println(tsg.getDataCodingLength());
            System.out.println(tsg.getModelCodingLength());

            ITSGNode<String> fakeRoot = TSGNode.debugFromString(new String[]{"FieldDeclaration", "type", "INTEGER_TYPE", "|", "|", "name"});
            fakeRoot.setParent(TSGNode.create("SourceFile"));
            TSGRule<String> rule = TSGRule.create(tsg.getDelimiter());
            rule.setRoot(fakeRoot);
            rule.addOccurrence(0, Arrays.asList(2, 13, 18, 14));
            rule.addOccurrence(0, Arrays.asList(7, 39, 42, 40));
            rule.addOccurrence(0, Arrays.asList(6, 34, 36, 35));
            rule.addOccurrence(0, Arrays.asList(8, 46, 50, 47));
            rule.setCount(4);
            rule.setInitialCount(4);
            tsg.addRule(rule);

            System.out.println(tsg.getModelCodingLength());
            System.out.println(tsg.getDataCodingLength());

            PCFG pcfg = new PCFG();
            pcfg.loadGrammar(config.getGrammarFile());
            Parser parser = new Parser(pcfg,"ClassBodyDeclaration");
            parser.parseDirectory(".\\out\\test_temp");

            System.out.println(pcfg.getDataCodingLength());

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
