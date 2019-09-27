/*
   This package is used to cluster input ASTs by using clustering algorithms
 */
package be.intimals.freqt.cluster;

import java.lang.String;
import java.io.*;

public class Main {

    static public void main(String[] args) throws IOException {

        /*
        //TESTING
        //input directory
        String inputDirectory = "input/jhotdraw-original";
        //output directory = database's name
        String outputDirectory = "output/jhotdraw";
        //creating database options
        String createDataBaseOption = "2"; //1: keep all labels, 2: keep leafs
        //clustering algorithm
        String algorithmName = "1"; //1: hierarchical; 2: kmeans
        //number of clusters output
        String numberClusters = "5";
        */



        if(args.length==0 || args.length != 5){
            System.out.println("---------------");
            System.out.println("USAGE:");
            System.out.println("java -jar clusterAST.jar inputDirectory outputDirectory createDataBaseOption algorithmName numberCluster");
            System.out.println("inputDirectory: a directory containing ASTs");
            System.out.println("outputDirectory: a directory containing output clusters");
            System.out.println("createDataBaseOption: 1 - keep all labels; 2 - keep only leafs");
            System.out.println("algorithmName: 1 - hierarchical; 2 - KMeans");
            System.out.println("numberCluster: number of output clusters");
            System.exit(-1);
        }

        //input directory
        String inputDirectory = args[0];
        String outputDirectory = args[1];
        String createDataBaseOption = args[2];
        String algorithmName = args[3];
        String numberClusters = args[4];

        //check input directory
        File temp = new File(inputDirectory);
        if(!temp.exists()){
            System.out.println("inputDirectory not found ");
            System.exit(2);
        }
        //run cluster algorithm
        Cluster cluster = new Cluster(inputDirectory, outputDirectory, createDataBaseOption, algorithmName, numberClusters);
        cluster.run();
    }
}
