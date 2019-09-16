/*
   This package is used to cluster input ASTs by using clustering algorithms
 */
package be.intimals.freqt.cluster;

import java.lang.String;
import java.io.*;

public class Main {

    static public void main(String[] args) throws IOException {

/*
        //input directory
        String inputDirectory = "input/jhotdraw-original";
        //
        String numberClusters = "10";
        //output directory = database's name
        String dataName = inputDirectory.split("/")[inputDirectory.split("/").length-1];
        //get parameters for clustering algorithms
        String[] clusterParameters = new String[4];
        clusterParameters[0] = "Hierarchical_clustering";
        clusterParameters[1] = "euclidian";
        clusterParameters[2] = "10";
        clusterParameters[3] = "0";
*/

        if(args.length==0 || args.length != 3){
            System.out.println("---------------");
            System.out.println("USAGE:");
            System.out.println("java -jar clusterAST.jar inputDirectory outputDirectory numberCluster");
            System.out.println();
            System.exit(1);
        }

        //input directory
        String inputDirectory = args[0];
        String outputDirectory = args[1];
        String numberClusters = args[2];

        //check input directory
        File temp = new File(inputDirectory);
        if(!temp.exists()){
            System.out.println("inputDirectory not found ");
            System.exit(2);
        }

        //run cluster algorithm
        Cluster cluster = new Hierarchical(inputDirectory,outputDirectory,numberClusters);
        ((Hierarchical) cluster).run();

        //Cluster cluster = new SPMF(inputDirectory,outputDirectory,"Hierarchical_clustering","euclidian",numberClusters,"5");
        //((SPMF) cluster).run();

        //public SPMF(String _inputDir, String _outputDir, String _algorithmName, String _distanceMethod, String _maxDistance, String _minSizeCluster){




    }
}
