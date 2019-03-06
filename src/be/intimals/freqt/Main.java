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

import be.intimals.freqt.util.*;
import be.intimals.freqt.core.*;
import be.intimals.freqt.config.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import java.lang.String;

public class Main {

    static public void main(String[] args) throws IOException {
        Main m = new Main();

        if (args.length==0) {

            System.out.println("Single-run Freq-T usage:\n" +
                    "java -jar freqt_java.jar CONFIG_FILE [MIN_SUPPORT] [INPUT_FOLDER]\n" +
                    "\n" +
                    "Multi-run Freq-T usage:\n" +
                    "java -jar freqt_java.jar -multi CONFIG_FILE");

            m.singleRun(args);

        } else if (args[0].equals("-multi")) {
            m.multiRun(args);
        } else {
            m.singleRun(args);
        }
        System.exit(3);

    }

    private void singleRun(String[] args) {

        try{
                //load basic configuration
                String configPathBasic = args.length == 0 ? "conf/java/config-jhotdraw.properties" : args[0];
                Config configBasic = new Config(configPathBasic);
                //update minSup and sub folder name
                String inputMinSup = args.length == 0 ? String.valueOf(configBasic.getMinSupport()) : args[1];
                String inputFold = args.length == 0 ? "fold4" : args[2];
                //create temporary configuration
                Properties prop;
                OutputStream output = null;
                String configPathTemp = "";

                String inputPath = "";
                String outputPath = "";

                String sourceMatcher = "";
                String inputPatterns = "";
                String outputMatches = "";

                try {
                    prop = configBasic.getProp();
                    //update input dir path
                    inputPath = configBasic.getInputFiles().replace("\"", "") + "/"+inputFold;
                    //update output file path
                    File directory = new File(configBasic.getOutputFile());
                    if(!directory.exists()) directory.mkdir();

                    outputPath = configBasic.getOutputFile().replace("\"","") +
                            "/"+inputFold.replaceAll("\\/","-")+"-" + inputMinSup +"-patterns.xml";
                    //delete output file if if exists
                    Files.deleteIfExists(Paths.get(outputPath));

                    //create parameters for forest matcher
                    sourceMatcher = inputPath;
                    inputPatterns = outputPath;
                    outputMatches = configBasic.getOutputFile().replace("\"","") +
                            "/"+inputFold.replaceAll("\\/","-")+"-" +inputMinSup + "-matches.xml";
                    Files.deleteIfExists(Paths.get(outputMatches));

                    //update path of temporary configuration
                    configPathTemp = configBasic.getOutputFile().replace("\"","")
                            + "/"+ inputFold.replaceAll("\\/","-")+"-"+ inputMinSup + "-config.properties";
                    Files.deleteIfExists(Paths.get(configPathTemp));

                    //update properties
                    //prop.replace("minSupport", inputMinSup);
                    prop.setProperty("minSupport",inputMinSup);
                    prop.replace("inFiles", inputPath);
                    prop.replace("outFile", outputPath);
                    //save new properties
                    output = new FileOutputStream(configPathTemp);
                    prop.store(output, null);

                } catch (IOException io) {
                    io.printStackTrace();
                } finally {
                    if (output != null) {
                        try {
                            output.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                //load new configuration;
                Config config = new Config(configPathTemp);
                //run Freqt
                FreqT freqt = new FreqT(config);
                freqt.run();

                //run forestmatcher
                if(config.outputAsXML()){
                    String command = "java -jar forestmatcher.jar " +
                            sourceMatcher + " " + inputPatterns +" " + outputMatches;
                    Process proc = Runtime.getRuntime().exec(command);

                }
            //System.out.println("===========================================================");
            //how to safety stop this single task
            return;
        }
        catch (Exception e){
            System.out.println("!!! Error: main "+e);
            e.printStackTrace();
        }
    }

    private class MultiRunConfig{
        public Integer minSupport;
        public String inFolder;
    }

    private void multiRun(String[] args) throws IOException {
        String configPathBasic = args[1];
        //String timeOut = args[2];

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
                runs.add(run);
            }
        }

        runs.parallelStream().forEach((run) -> {
            String runDescr = "(minimum support:" + run.minSupport + " ; input:" + run.inFolder + ")";
            System.out.println("Starting run " + runDescr);
            String[] runArgs = {args[1], run.minSupport.toString(), run.inFolder};
            singleRun(runArgs);
            System.out.println("Finished run " + runDescr);
        });
    }


}
