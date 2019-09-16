package be.intimals.freqt.cluster;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class SPMF extends Cluster {

    public SPMF(String _inputDir, String _outputDir, String _algorithmName, String _distanceMethod, String _maxDistance, String _minSizeCluster){
        super(_inputDir,_outputDir,_maxDistance);
        algorithmName = _algorithmName;
        distanceMethod = _distanceMethod;
        minSizeCluster = _minSizeCluster;
    }
    ///////////////////////////////////////////
    //run clustering algorithms from spmf.jar
    //create database for clustering algorithm
    private void createDatabase(String outputFileName){
        try{
            System.out.print("Creating database");
            FileWriter fr = new FileWriter(outputFileName);

            //create values for an instance (AST)
            ArrayList<Integer> allLabels = new ArrayList<>(labelIndex.keySet());
            Collections.sort(allLabels);
            System.out.print(" (instances: "+database.size()+ ", attributes: "+allLabels.size()+") ... ");

            if(DEBUG) {
                System.out.println("\nall labels:");
                for (int i = 0; i < allLabels.size(); ++i) {
                    System.out.print(allLabels.get(i) + ",");
                }
            }

            //write attribute
            for(int i=1; i<= allLabels.size(); ++i)
                fr.write("@ATTRIBUTEDEF="+i+"\n");

            for(int i=0; i<database.size(); ++i) {
                fr.write("@NAME="+fileNames.get(i)+"\n");

                ArrayList<Integer> currentInstance = database.get(i);
                Collections.sort(currentInstance);

                if(DEBUG) {
                    System.out.println("\ncurrent instance :");
                    for (int j = 0; j < currentInstance.size(); ++j) {
                        System.out.print(currentInstance.get(j) + ",");
                    }
                }

                //init tempInstance
                Integer[] tempInstance = new Integer[allLabels.size()];
                for(int j=0; j<tempInstance.length; ++j) tempInstance[j] = 0;

                //create instance
                for (int j=0; j<currentInstance.size(); ++j) {
                    int index = allLabels.indexOf(currentInstance.get(j));
                    tempInstance[index] = tempInstance[index]+1;
                }
                if(DEBUG) {
                    System.out.println("\ntemporary instance:");
                    for (int j = 0; j < tempInstance.length; ++j) {
                        System.out.print(tempInstance[j] + ",");
                    }
                }

                //write instance to file
                String str="";
                for(int j=0; j<tempInstance.length-1; ++j) {
                    //System.out.print(tempInstance[j] + " ");
                    str += Double.valueOf(tempInstance[j])+" ";
                }
                str += Double.valueOf(tempInstance[tempInstance.length-1]);
                fr.write(str+"\n");
            }
            fr.flush();
            fr.close();
            System.out.println(" end.");

        }catch (Exception e){
            System.out.println("error: creating database "+e);
        }
    }
    //run a clustering algorithm
    //more information can be found here: http://www.philippe-fournier-viger.com/spmf/index.php
    private void runClusterAlg(String algorithmName, String inputData, String outputCluster, String distanceMethod, int maxDistance){
        try{
            //run clustering algorithms
            System.out.print("Running clustering algorithm ...");
            String commandStr = "";
            switch (algorithmName){
                case "KMeans":
                    commandStr = "java -jar spmf.jar run " + algorithmName + " " + inputData +" "+outputCluster+" "+
                            maxDistance + " " + distanceMethod;
                    break;
                case "BisectingKMeans":
                    commandStr = "java -jar spmf.jar run " + algorithmName + " " + inputData +" "+outputCluster+" "+
                            maxDistance + " " + distanceMethod +" 20";
                    break;
                case "Hierarchical_clustering":
                    commandStr = "java -jar spmf.jar run " + algorithmName + " " + inputData +" "+outputCluster+" "+
                            maxDistance + " " + distanceMethod;
                    break;
            }
            Process proc = Runtime.getRuntime().exec(commandStr);
            proc.waitFor();
            System.out.println(" end.");

        }catch (Exception e){
            System.out.println("Error: running clustering algorithm "+e);
        }
    }

    private void filterCluster(String inputCluster, String outputCluster, int minClusterSize){
        try{
            System.out.print("Filtering clusters ...");
            BufferedReader br = new BufferedReader(new FileReader(inputCluster));
            FileWriter fw = new FileWriter(outputCluster);

            int nbCluster = 0;
            String line;
            while ((line = br.readLine()) != null) {
                if( (! line.isEmpty()) && (line.charAt(0) !='@') ){
                    String[]lineTemp = line.split("]");
                    if(lineTemp.length >= minClusterSize){
                        fw.write("#cluster_" + ++nbCluster + "\n");
                        for(int i=0; i<lineTemp.length; ++i){
                            String[] instanceNames = lineTemp[i].split(" ");
                            fw.write(instanceNames[0].substring(1)+"\n");
                        }
                    }
                }
            }
            br.close();
            fw.flush();
            fw.close();
            System.out.println(" end.");

        }catch (Exception e){
            System.out.println("Error: extracting clusters "+e);
        }
    }

    private void createClusterDir(String outputDir, String outputFile){
        try{
            //create sub-directories to store results of each cluster found
            File c = new File(outputFile);
            if(c.length() == 0){
                System.out.println("No cluster found");
                System.out.println("Finished.");
            }else {
                System.out.print("Creating clusters' directories ...");
                //String dataName = outputFile.split("/")[0];
                Map<String, Vector<String>> foundClusters = new HashMap<>();
                Vector<String> filesOfCluster = new Vector<>();
                String clusterName = "";
                BufferedReader br = new BufferedReader(new FileReader(outputFile));
                String line;
                while ((line = br.readLine()) != null) {
                    if ((line.charAt(0) == '#')) {
                        if (!filesOfCluster.isEmpty()) {
                            foundClusters.put(clusterName, filesOfCluster);
                            filesOfCluster = new Vector<>();
                            clusterName = line;
                        } else {
                            clusterName = line;
                        }
                    } else {
                        filesOfCluster.add(line);
                    }
                }
                //put the last cluster to clustersFiles
                foundClusters.put(clusterName, filesOfCluster);

                //copy all files in a cluster to folder "cluster_i", i is the id of cluster
                for (Map.Entry<String, Vector<String>> entry : foundClusters.entrySet()) {
                    String folderName = outputDir + "/" + entry.getKey().substring(1);
                    File folder = new File(folderName);
                    if (!folder.exists()) folder.mkdir();
                    //copy source to target using Files Class
                    for (int j = 0; j < entry.getValue().size(); ++j) {
                        String sourceFileName = entry.getValue().get(j);
                        String targetFileName = folder + "/" + entry.getValue().get(j).replace('/', '_');
                        Path sourceDirectory = Paths.get(sourceFileName);
                        Path targetDirectory = Paths.get(targetFileName);
                        Files.copy(sourceDirectory, targetDirectory, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                System.out.println(" end.");
            }

        }catch (Exception e){
            System.out.println("error: create directories for clusters "+e);
        }
    }

    public void run(){
        try{
            //read database
            readDatabase(new File(this.inputDir));
            //write database for clustering algorithms
            String inputData = "clusterInputData.txt";
            createDatabase(inputData);
            //run clustering algorithms
            String outputCluster = "clusterOutput.txt";
            runClusterAlg(this.algorithmName, inputData, outputCluster, this.distanceMethod, Integer.valueOf(this.numberCluster));
            //extract cluster to find groups of files
            String clusterFiltered = "clusterFiltered.txt";
            filterCluster(outputCluster, clusterFiltered, Integer.valueOf(this.minSizeCluster));
            //create directories for found clusters
            createClusterDir(this.outputDir,clusterFiltered);
            //delete temporary files
            //Files.deleteIfExists(Paths.get(inputData));
            //Files.deleteIfExists(Paths.get(outputCluster));
            //Files.deleteIfExists(Paths.get(clusterFiltered));


        }catch (Exception e){
            System.out.println("error: call cluster algorithm "+e);
        }
    }

}
