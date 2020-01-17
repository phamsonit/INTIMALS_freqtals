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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Main {

    static public void main(String[] args) throws IOException {
        Main m = new Main();

        if (args.length==0) {

//            String[] argsss = {"conf/java/config.properties","5","fold4"};
//            m.singleRun(argsss);
//            System.exit(-1);

            System.out.println("Single-run Freq-T usage:\n" +
                    "java -jar freqt_java.jar CONFIG_FILE [MIN_SUPPORT] [INPUT_FOLDER] (--memory [VALUE]) (--debug-file)\n" +
                    "\n" +
                    "Multi-run Freq-T usage:\n" +
                    "java -jar freqt_java.jar -multi CONFIG_FILE (--memory [VALUE]) (--debug-file)");
        } else {
            if (args[0].equals("-multi")) {
                m.multiRun(args);
            } else {
                m.singleRun(args);
            }
        }
        System.exit(2);

    }

    private void singleRun(String[] args) {
        try{
            //load basic configuration
            String configPathBasic = args[0];
            Config configBasic = new Config(configPathBasic);
            String inputMinSup = args[1];
            String inputFold = args[2];
            String memory = null;
            if(args.length == 4) //memory flag or debug is set
            	if(args[3].contains("file")) { //debug file
            		PrintStream o = new PrintStream(new File(inputFold+"-debug-log.txt")); 
                    // Assign o to output stream to redirect to file
                    System.setOut(o);
                    System.setErr(o);
            	}else //memory value
            		memory = args[3];
            if(args.length == 5) { //memory and debug file
            	memory = args[3];
            	PrintStream o = new PrintStream(new File(inputFold+"-debug-log.txt")); 
            	// Assign o to output stream to redirect to file
            	System.setOut(o);
                System.setErr(o);
            }
            String sep = "/";

            //create final configuration as used by FreqT
            Properties prop = configBasic.getProp();
            //input data
            String inputPath = configBasic.getInputFiles().replace("\"", "") + sep + inputFold;
            System.out.println("Reading input from: " + inputPath);

            File outputDir = new File(configBasic.getOutputFile());
            if(!outputDir.exists()) outputDir.mkdir();

            String outputPrefix = configBasic.getOutputFile().replace("\"", "") +
                    sep + inputFold.replaceAll(sep, "-") + "-" + inputMinSup;

            //output patterns
            String outputPatterns = outputPrefix + "-patterns.xml";
            Files.deleteIfExists(Paths.get(outputPatterns));

            //final configuration as used by FreqT
            String finalConfig =  outputPrefix + "-config.properties";
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
            //prop.replace("minSupport", inputMinSup);
            prop.setProperty("minSupport",inputMinSup);
            prop.replace("inputPath", inputPath);
            prop.replace("outputPath", outputPatterns);
            prop.remove("minSupportList");
            prop.remove("inFilesList");

            //save new properties in the final configuration
            OutputStream output = new FileOutputStream(finalConfig);
            prop.store(output, null);
            output.close();

            //load final configuration as new configuration;
            Config config = new Config(finalConfig);
            //run Freqt to find maximal patterns
            FreqT_Int freqt = new FreqT_Int(config);
            //FreqT freqt = new FreqT(config);
            freqt.run();

            //run forestmatcher to create matches.xml and clusters.xml
            System.out.println("Running forestmatcher ...");
            String command = "";
            if(memory != null)
            	command = "java -jar " + memory + " forestmatcher.jar " +
                        inputPath + " " + outputPatterns +" " + outputMatches + " " + outputClusters;
            else
            	command = "java -jar forestmatcher.jar " +
                    inputPath + " " + outputPatterns +" " + outputMatches + " " + outputClusters;
            System.out.println("With command: "+command);
            Process proc = Runtime.getRuntime().exec(command);
            //Get output of forestMatcher
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

            // Read the output from the command
            System.out.println("Here is the standard output of the command:\n");
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }
            // Read any errors from the attempted command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
            proc.waitFor();

            File pattern = new File(outputClustersTemp);
            if(pattern.exists()){
                //find common patterns in each cluster
                System.out.println("Mining common patterns in clusters ...");
                String outputPatternsTemp = outputPatterns+".txt";
                FreqT_common inCluster = new FreqT_common(config,freqt.getGrammar(),freqt.getXmlCharacters());
                inCluster.run(outputPatternsTemp, outputClustersTemp, outputCommonPatterns);

                //find matches of common_patterns
                command = "java -jar forestmatcher.jar " +
                        inputPath + " " + outputCommonPatterns +" " + outputCommonMatches + " " + outputCommonClusters;
                System.out.println("With command: "+command);
                 proc = Runtime.getRuntime().exec(command);
                //Get output of forestMatcher
                stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

                // Read the output from the command
                System.out.println("Here is the standard output of the command:\n");
                s = null;
                while ((s = stdInput.readLine()) != null) {
                    System.out.println(s);
                }
                // Read any errors from the attempted command
                System.out.println("Here is the standard error of the command (if any):\n");
                while ((s = stdError.readLine()) != null) {
                    System.out.println(s);
                }
                proc.waitFor();
                System.out.println("Cleaning up ...");
                //Files.deleteIfExists(Paths.get(outputPatternsTemp));
                //Files.deleteIfExists(Paths.get(outputCommonPatterns+".txt"));
            }

            System.out.println("Finished ...");

            //System.out.println("===========================================================");
            //return;
        }
        catch (Exception e){
            System.out.println("!!! Error: main "+e);
            e.printStackTrace();
        }
    }

    //memory consumption test
    private static final long MEGABYTE = 1024L * 1024L;

    private static long bytesToMegabytes(long bytes) {
        return bytes / MEGABYTE;
    }


    //
    private class MultiRunConfig{
        public Integer minSupport;
        public String inFolder;
        public String memory = null;
        public String debugFile = null;
    }

    private void multiRun(String[] args) throws IOException {
        String configPathBasic = args[1];
        //String timeOut = args[2];
        String memory = null;
        if(args.length > 2 && args[2].contentEquals("--memory"))//Try and set memory arg
        	memory = "-Xmx"+args[3];
        String debug = null;
        if(args.length > 2 && args[2].contentEquals("--debug-file")) {//Try and set debug arg
        	debug = args[2];
        }else if(args.length > 4 && args[4].contentEquals("--debug-file"))//Try and set memory and debug arg
        	debug = args[4];
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
