/*
   This package is used to cluster input ASTs by using clustering algorithms
 */
package be.intimals.freqt.cluster;

import java.lang.String;
import java.io.*;

public class Main {

    static public void main(String[] args) throws IOException {

//        String file1 = "output/jhotdraw-original_1_1_svd/outputCluster.txt";
//        String file2 = "output/jhotdraw-original_2_1_svd/outputCluster.txt";
//        ComputeClusterOverlap comp = new ComputeClusterOverlap(file1,file2);
//        comp.run();
//        System.exit(-1);

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

        if(args.length==0){
            System.out.println("---------------");
            System.out.println("USAGE:");
            System.out.println("java -jar clusterAST.jar --input [inputDirectory] --output [outputDirectory] --database [n] --algorithm [n] --numberCluster [n] --svd");
            System.out.println("Parameters:");
            System.out.println("--input [inputDirectory]   : a directory containing ASTs");
            System.out.println("--output [outputDirectory] : a directory containing output clusters");
            System.out.println("--database [n]             : n=1 : keep all labels; n=2 : keep only leafs. default n=1");
            System.out.println("--algorithm [n]            : n=1 : hierarchical; n=2 : KMeans. Default n=1");
            System.out.println("--numberCluster [n]        : n = number of output clusters");
            System.out.println("--svd                      : if using svd to compress input data. Default 'non' ");
            System.out.println("---------------");
            System.exit(-1);
        }

        //input directory
        String inputDirectory = "";// = args[0];
        String outputDirectory = "output";// = args[1];
        String createDataBaseOption = "1";// = args[2];
        String algorithmName = "1";// = args[3];
        String numberClusters = "10";// = args[4];
        String svd = "none";// = args[5];

        for(int i=0; i<args.length; ++i){
            if(args[i].toLowerCase().equals("--input")){
                inputDirectory = args[i+1];
                i++;
            }
            if(args[i].toLowerCase().equals("--output")){
                outputDirectory = args[i+1];
                i++;
            }
            if(args[i].toLowerCase().equals("--database")){
                createDataBaseOption = args[i+1];
                i++;
            }
            if(args[i].toLowerCase().equals("--algorithm")){
                algorithmName = args[i+1];
                i++;
            }
            if(args[i].toLowerCase().equals("--numbercluster")){
                numberClusters = args[i+1];
                i++;
            }
            if(args[i].equals("--svd")){
                svd = "svd";
            }
        }


        //check input directory
        File temp = new File(inputDirectory);
        if(!temp.exists()){
            System.out.println("inputDirectory not found ");
            System.exit(2);
        }
        //create directory to store clusters
        String databaseName = inputDirectory.split("/")[inputDirectory.split("/").length-1];
        outputDirectory +=  "/" + databaseName + "_" + createDataBaseOption + "_" + algorithmName + "_" + svd;

        //run cluster algorithm
        Cluster cluster = new Cluster(inputDirectory, outputDirectory, createDataBaseOption, algorithmName, numberClusters, svd);
        cluster.run();


    }
}
