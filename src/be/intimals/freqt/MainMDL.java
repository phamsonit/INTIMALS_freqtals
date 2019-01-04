package be.intimals.freqt;

import be.intimals.freqt.mdl.input.BasicStrJavaLoader;
import be.intimals.freqt.mdl.input.Database;
import be.intimals.freqt.mdl.input.IDatabaseLoader;
import be.intimals.freqt.mdl.input.IDatabaseNode;
import be.intimals.freqt.mdl.miner.BeamFreqT;
import be.intimals.freqt.mdl.tsg.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

public class MainMDL {
    public static void main(String[] args) {
        try {
            //LogManager.getLogManager().reset();
            String configPath = args.length == 0 ? "conf/mdl/config.properties" : args[0];

            Config config = new Config(configPath);

            long start = System.currentTimeMillis();

            IDatabaseLoader<String> loader = new BasicStrJavaLoader();
            Database<String> db = loader.loadDirectory(config.getInputFiles());
            System.out.println(db.getSize() + " transactions");
            ATSG<String> tsg = new BasicTSG();
            tsg.loadDatabase(db);

            //debugPrintCodingLength(tsg);
            //ITSGNode<String> fakeRoot = TSGNode.buildFromList(new String[]{"s", "a", "a0", "$", "$", "b"}, "$");
            //TSGRule<String> rule = TSGRule.create(tsg.getDelimiter());
            //rule.setRoot(fakeRoot);
            //rule.addOccurrence(0, Arrays.asList(0, 1, 6, 5));
            //tsg.addRule(rule);

            debugPrintCodingLength(tsg);

            BeamFreqT miner = BeamFreqT.create(db, tsg);
            miner.run(config);

            System.out.println("Best coding length: " + miner.getBestLength());
            System.out.println("Best rules: " + miner.getBestRules().stream()
                    .map(e -> e.getRule()).collect(Collectors.toList()));
            debugPrintCodingLength(tsg);
            miner.getBestRules().forEach(best -> tsg.addRule(best.getRule()));
            debugPrintCodingLength(tsg);

            /*
            // TODO debug
            ITSGNode<String> fakeRoot = TSGNode.buildFromList(new String[]{"FieldDeclaration", "type", "INTEGER_TYPE", "$", "$", "name"}, "$");
            TSGRule<String> rule = TSGRule.create(tsg.getDelimiter());
            rule.setRoot(fakeRoot);
            rule.addOccurrence(0, Arrays.asList(1, 3, 8, 7));
            tsg.addRule(rule);

            tsg.removeRule(rule);

            ITSGNode<String> fakeRoot2 = TSGNode.buildFromList(new String[]{"FieldDeclaration", "annotation", "$", "modifiers"}, "$");
            TSGRule<String> rule2 = TSGRule.create(tsg.getDelimiter());
            rule2.setRoot(fakeRoot2);
            rule2.addOccurrence(0, Arrays.asList(1, 4, 6));
            rule2.addOccurrence(0, Arrays.asList(2, 13, 14));
            tsg.addRule(rule2);

            //tsg.removeRule(rule2);

            System.out.println("Model: " + tsg.getModelCodingLength());
            System.out.println("Data : " + tsg.getDataCodingLength());
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

    private static void debugPrintCodingLength(ATSG<String> tsg) {
        double modelLength = tsg.getModelCodingLength();
        double dataLength = tsg.getDataCodingLength();
        System.out.println("Model: " + tsg.getModelCodingLength());
        System.out.println("Data : " + tsg.getDataCodingLength());
        System.out.println("Sum : " + (modelLength + dataLength));
    }
}
