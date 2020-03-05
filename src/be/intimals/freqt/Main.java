/*
   $Id: freqt.cpp,v 1.5 2003/01/22 08:37:19 taku-ku Exp $;

   Copyright (C) 2003 Taku Kudo, All rights reserved.
   This is free software with ABSOLUTELY NO WARRANTY.

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA
   ===============================
   java implementation: 16 May 2018
   by PHAM Hoang Son
*/

package be.intimals.freqt;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.core.FreqT_Int;
import be.intimals.freqt.core.FreqT_common;
import be.intimals.freqt.core.Freqt_Int_2class;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Main {

    static public void main(String[] args) throws IOException {
        Main m = new Main();

        //String[] agg = {"-multi", "conf/java/config.properties", "--class"};
        //args = agg;

        if (args.length==0) {
            System.out.println("Single-run Freq-T usage:\n" +
                    "java -jar freqt_java.jar CONFIG_FILE [MIN_SUPPORT] [INPUT_FOLDER] (--class) (--memory [VALUE]) (--debug-file)\n" +
                    "\n" +
                    "Multi-run Freq-T usage:\n" +
                    "java -jar freqt_java.jar -multi CONFIG_FILE (--class) (--memory [VALUE]) (--debug-file)");
        } else {
            if (args[0].equals("-multi")) {
                m.multiRun(args);
            } else {
                m.singleRun(args);
            }
        }
        System.exit(1);
    }

    private void singleRun(String[] args) {
        try{
            String memory = null; //args[4]
            String debugFile = null; //args[5]
            String classData = null;
            String finalConfig = null;

            finalConfig = parseConfig(args);
            if(args.length > 3){
                finalConfig = parseConfig(args);
                for(int i=3; i<args.length; ++i) {
                    if (args[i].equals("--class"))
                        classData = "--class";
                    if (args[i].equals("--memory")) {
                        memory = "-Xmx" + args[i + 1];
                        i++;
                    }
                    if (args[i].equals("--debug-file"))
                        debugFile = args[i];
                }
            }

            //load final configuration as new configuration;
            Config config = new Config(finalConfig);
            if(classData != null && classData.equals("--class")){
                Freqt_Int_2class freqt_int_2class = new Freqt_Int_2class(config);
                freqt_int_2class.run();
                runForestMatcher(config, memory);
                findCommonPattern(config, freqt_int_2class.getGrammar(), freqt_int_2class.getXmlCharacters());
                cleanUp(config);
            }else{
                //run Freqt to find maximal patterns
                FreqT_Int freqt = new FreqT_Int(config);
                freqt.run();
                runForestMatcher(config, memory);
                findCommonPattern(config, freqt.getGrammar(), freqt.getXmlCharacters());
                cleanUp(config);
            }

            System.out.println("Finished ...");

        }
        catch (Exception e){
            System.out.println("!!! Error: main "+e);
            e.printStackTrace();
        }
    }

    private String parseConfig(String[] args){
        String finalConfig = "";
        try{

            String configPathBasic = args[0];
            Config configBasic = new Config(configPathBasic);
            String inputMinSup = args[1];
            String inputFold = args[2];

            String sep = "/";
            //create final configuration as used by FreqT
            Properties prop = configBasic.getProp();
            //input data
            String inputPath = configBasic.getInputFiles().replace("\"", "") + sep + inputFold;
            System.out.println("Reading input from: " + inputPath);

            String inputPath1 = inputPath+"/"+configBasic.getInputFiles1();
            String inputPath2 = inputPath+"/"+configBasic.getInputFiles2();

            File outputDir = new File(configBasic.getOutputFile());
            if(!outputDir.exists()) outputDir.mkdir();

            String outputPrefix = configBasic.getOutputFile().replace("\"", "") +
                    sep + inputFold.replaceAll(sep, "-") + "-" + inputMinSup;

            //output patterns
            String outputPatterns = outputPrefix + "-patterns.xml";
            Files.deleteIfExists(Paths.get(outputPatterns));

            //final configuration as used by FreqT
            finalConfig =  outputPrefix + "-config.properties";
            Files.deleteIfExists(Paths.get(finalConfig));

            //create parameters for forest matcher
            String outputMatches = outputPrefix + "-matches.xml";
            Files.deleteIfExists(Paths.get(outputPrefix));

            String outputClusters = outputPrefix + "-clusters.xml";
            Files.deleteIfExists(Paths.get(outputClusters));

            String outputClustersTemp = outputPrefix + "-matches_clusters.xml";
            Files.deleteIfExists(Paths.get(outputClustersTemp));

            String outputCommonPatterns = outputPrefix + "-patterns_common.xml";
            Files.deleteIfExists(Paths.get(outputCommonPatterns));

            String outputCommonMatches = outputPrefix + "-matches_common.xml";
            Files.deleteIfExists(Paths.get(outputCommonMatches));

            String outputCommonClusters = outputPrefix + "-common_clusters.xml";
            Files.deleteIfExists(Paths.get(outputCommonClusters));

            String outputCommonClustersMatches = outputPrefix + "-matches_common_clusters.xml";
            Files.deleteIfExists(Paths.get(outputCommonClustersMatches));

            //update properties
            prop.setProperty("minSupport",inputMinSup);
            prop.replace("inputPath", inputPath);
            prop.replace("inputPath1", inputPath1);
            prop.replace("inputPath2", inputPath2);
            prop.replace("outputPath", outputPatterns);
            prop.remove("minSupportList");
            prop.remove("inFilesList");

            prop.setProperty("outputMatches", outputMatches);
            prop.setProperty("outputClusters", outputClusters);
            prop.setProperty("outputClustersTemp", outputClustersTemp);
            prop.setProperty("outputCommonPatterns", outputCommonPatterns);
            prop.setProperty("outputCommonMatches", outputCommonMatches);
            prop.setProperty("outputCommonClusters", outputCommonClusters);

            prop.setProperty("outputClustersTemp", outputClustersTemp);
            prop.setProperty("outputCommonPatterns", outputCommonPatterns);
            prop.setProperty("outputCommonMatches", outputCommonMatches);
            prop.setProperty("outputCommonClusters", outputCommonClusters);

            //save new properties in the final configuration
            OutputStream output = new FileOutputStream(finalConfig);
            prop.store(output, null);
            output.close();

        }catch (Exception e){
            System.out.println("parse args error: "+e);
        }
        return finalConfig;
    }

    private void runForestMatcher(Config config, String memory )
            throws IOException, InterruptedException {
        //run forestmatcher to create matches.xml and clusters.xml
        System.out.println("Running forestmatcher ...");
        String command = "";
        if(memory != null)
            command = "java -jar " + memory + " forestmatcher.jar " +
                    config.getInputFiles() + " " + config.getOutputFile() +" "
                    + config.getOutputMatches() + " " + config.getOutputClusters();
        else
            command = "java -jar forestmatcher.jar " +
                    config.getInputFiles() + " " + config.getOutputFile() +" " +
                    config.getOutputMatches() + " " + config.getOutputClusters();

        System.out.println("With command: "+command);
        Process proc = Runtime.getRuntime().exec(command);
        proc.waitFor();
    }

    private void findCommonPattern(Config config, Map<String, ArrayList<String>> grammar, Map<String, String> xmlCharacters)
                                throws IOException, InterruptedException {
        String command;
        File pattern = new File(config.getOutputClustersTemp());
        if(pattern.exists()){
            //find common patterns in each cluster
            System.out.println("Mining common patterns in clusters ...");
            String outputPatternsTemp = config.getOutputFile()+".txt";
            FreqT_common common = new FreqT_common(config, grammar, xmlCharacters);
            common.run(outputPatternsTemp, config.getOutputClustersTemp(), config.getOutputCommonPatterns());

            //find matches for common_patterns
            command = "java -jar forestmatcher.jar " +
                   config.getInputFiles() + " " + config.getOutputCommonPatterns()+" " +
                   config.getOutputCommonMatches() + " " + config.getOutputCommonClusters();
            System.out.println("With command: "+command);
            Process proc = Runtime.getRuntime().exec(command);
            proc.waitFor();
        }
    }

    private void cleanUp(Config config) throws IOException {
        System.out.println("Cleaning up ...");
        Files.deleteIfExists(Paths.get(config.getOutputFile()+".txt"));
        Files.deleteIfExists(Paths.get(config.getOutputCommonPatterns()+".txt"));
    }

    private class MultiRunConfig{
        public Integer minSupport;
        public String inFolder;
        public String classData = null;
        public String memory = null;
        public String debugFile = null;
    }

    private void multiRun(String[] args) throws IOException {
        String configPathBasic = args[1];
        String classData = null;
        String memory = null;
        String debug = null;

        if(args.length>2)
            for(int i=2; i<args.length; ++i) {
                if (args[i].equals("--class"))
                    classData = "--class";
                if (args[i].equals("--memory"))
                    memory = "-Xmx"+args[i+1];
                if (args[i].equals("--debug-file"))
                    debug = args[i];
            }

        Config conf = new Config(configPathBasic);
        List<Integer> minSupports = conf.getMinSupportList();
        List<String> folders = conf.getInputFilesList();

        // Create the list of all runs
        List<MultiRunConfig> runs = new ArrayList<>();
        for (Integer minSupport: minSupports) {
            for(String folder: folders) {
                MultiRunConfig run = new MultiRunConfig();
                run.minSupport = minSupport;
                run.inFolder = folder;
                if(classData != null)
                    run.classData = classData;
                if(memory != null)
                	run.memory = memory;
                if(debug != null)
                	run.debugFile = debug;
                runs.add(run);
            }
        }

        runs.parallelStream().forEach((run) -> {
            String runDescr = "(minimum support:" + run.minSupport + " ; input:" + run.inFolder + ")";
            System.out.println("Starting run " + runDescr);
            List<String> runArgs = new ArrayList<String>();
            runArgs.add(args[1]);
            runArgs.add(run.minSupport.toString());
            runArgs.add(run.inFolder);
            if(run.classData != null)
                runArgs.add(run.classData);
            if(run.memory != null)
            	runArgs.add(run.memory);
            if(run.debugFile != null)
            	runArgs.add(run.debugFile);
            String[] params = new String[runArgs.size()];
            singleRun(runArgs.toArray(params));
            System.out.println("Finished run " + runDescr);
        });
    }


}
