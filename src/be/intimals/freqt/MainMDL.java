package be.intimals.freqt;

import be.intimals.freqt.mdl.input.BasicStrJavaLoader;
import be.intimals.freqt.mdl.input.Database;
import be.intimals.freqt.mdl.input.IDatabaseLoader;
import be.intimals.freqt.mdl.input.IDatabaseNode;
import be.intimals.freqt.mdl.miner.BeamFreqT;
import be.intimals.freqt.mdl.tsg.*;

import java.io.IOException;
import java.util.Arrays;

public class MainMDL {
    public static void main(String[] args) {
        try {
            String configPath = args.length == 0 ? "conf/mdl/config.properties" : args[0];

            Config config = new Config(configPath);

            long start = System.currentTimeMillis();

            IDatabaseLoader<String> loader = new BasicStrJavaLoader();
            Database<String> db = loader.loadDirectory(config.getInputFiles());
            //Database<String> db = loader.loadFile(".\\out\\FD_debug_dataset.xml");
            ATSG<String> tsg = new BasicTSG();
            tsg.loadDatabase(db);

            double modelLength = tsg.getModelCodingLength();
            double dataLength = tsg.getDataCodingLength();
            System.out.println("Model: " + tsg.getModelCodingLength());
            System.out.println("Data : " + tsg.getDataCodingLength());
            System.out.println("Sum : " + (modelLength + dataLength));

            BeamFreqT miner = BeamFreqT.create(db, tsg, Integer.MAX_VALUE);
            miner.run(config);

            // TODO debug

            ITSGNode<String> fakeRoot = TSGNode.buildFromList(new String[]{"SourceFile", "FieldDeclaration", "type", "INTEGER_TYPE", "$", "$", "name"}, "$");
            fakeRoot = fakeRoot.getChildAt(0); // FieldDeclaration
            TSGRule<String> rule = TSGRule.create(tsg.getDelimiter());
            rule.setRoot(fakeRoot);
            rule.addOccurrence(0, Arrays.asList(1, 3, 8, 7));
            tsg.addRule(rule);

            tsg.removeRule(rule);

            ITSGNode<String> fakeRoot2 = TSGNode.buildFromList(new String[]{"SourceFile", "FieldDeclaration", "annotation", "$", "modifiers"}, "$");
            fakeRoot2 = fakeRoot2.getChildAt(0);
            TSGRule<String> rule2 = TSGRule.create(tsg.getDelimiter());
            rule2.setRoot(fakeRoot2);
            rule2.addOccurrence(0, Arrays.asList(1, 4, 6));
            rule2.addOccurrence(0, Arrays.asList(2, 13, 14));
            //tsg.addRule(rule2);

            //tsg.removeRule(rule2);

            System.out.println("Model: " + tsg.getModelCodingLength());
            System.out.println("Data : " + tsg.getDataCodingLength());


            /*
            PCFG pcfg = new PCFG();
            pcfg.loadGrammar(config.getGrammarFile());
            Parser parser = new Parser(pcfg,"ClassBodyDeclaration");
            parser.parseDirectory(".\\out\\test_temp");
            System.out.println(pcfg.getDataCodingLength());
            */

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
