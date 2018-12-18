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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

public class Main {

    static public void main(String[] args) {

        try{

            String configPath = args.length == 0 ? "conf/synthetic/config.properties" : args[0];

            Config config = new Config(configPath);

            //run Freqt
            long start = System.currentTimeMillis( );

            FreqT t = new FreqT();

            t.run(config); //-> outPatterns
            //System.out.println("number of subtrees: " + t.getOutputPatterns().size());
            //System.out.println("frequent patterns: "+t.getOutputPatterns().size());
            long end = System.currentTimeMillis( );
            long diff = end - start;
            System.out.println("mining time : " + diff +" ms");

            if(config.postProcess()){

                start = System.currentTimeMillis( );

                FreqT_post post = new FreqT_post();
                post.run(config,t.getOutputPatterns(),t.getGrammar(),t.getXmlCharacters());

                end = System.currentTimeMillis( );
                diff = end - start;
                System.out.println("post-processing time : " + diff +" ms");
            }

            /*
            //call forestmatcher
            System.out.println("Running matcher ...");
            String patternsFile = config.getOutputFile();
            String sourcesFile = "cobol_output/asts";
            String matchesFile = "cobol_output/matches.xml";
            String command = "java -jar forestmatcher.jar " +
                              sourcesFile + " " + patternsFile +" " + matchesFile;

            Process proc = Runtime.getRuntime().exec(command);
            */


            //group patterns by leaf label sets
            System.out.println("finding pattern groups ...");
            String patternsInput = config.getOutputFile();
            String patternGroupOutput = "cobol_output/pattern-group-filter.txt";
            Analyse analyse = new Analyse();
            analyse.analysePattern(patternsInput,patternGroupOutput);

        }
        catch (Exception e){System.out.println(e);}
    }
}
