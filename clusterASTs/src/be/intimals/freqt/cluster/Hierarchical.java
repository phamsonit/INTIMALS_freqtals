package be.intimals.freqt.cluster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Pattern;

public class Hierarchical extends Cluster {
    public Hierarchical(String _inputDir, String _outputDir, String _num){
        super(_inputDir,_outputDir,_num);
    }

    //create database for clustering algorithm
    private void createCSV(String outputFileName){
        try{
            System.out.print("Creating database");
            FileWriter fr = new FileWriter(outputFileName);

            //create values for an instance (AST)
            ArrayList<Integer> allLabels = new ArrayList<>(labelIndex.keySet());
            Collections.sort(allLabels);
            System.out.print(" (instances: "+database.size()+ ", attributes: "+allLabels.size()+") ... ");


            for(int i=0; i<database.size(); ++i) {
                ArrayList<Integer> currentInstance = database.get(i);
                Collections.sort(currentInstance);
                //init tempInstance
                Integer[] tempInstance = new Integer[allLabels.size()];
                for(int j=0; j<tempInstance.length; ++j) tempInstance[j] = 0;
                //create instance
                for (int j=0; j<currentInstance.size(); ++j) {
                    int index = allLabels.indexOf(currentInstance.get(j));
                    tempInstance[index] = tempInstance[index]+1;
                }
                //write instance to file
                String str="";
                for(int j=0; j<tempInstance.length-1; ++j) {
                    //System.out.print(tempInstance[j] + " ");
                    str += Double.valueOf(tempInstance[j])+",";
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

    private void createClusterDirPy(String outputDir,String inputFile){
        try{
            //create sub-directories to store ASTs of each clusters
            File c = new File(inputFile);
            if(c.length() == 0){
                System.out.println("No cluster found");
                System.out.println("Finished.");
            }else {
                System.out.print("Creating clusters' directories ...");
                Map<String, Vector<String>> foundClusters = new HashMap<>();
                BufferedReader br = new BufferedReader(new FileReader(inputFile));
                String line;
                int count=0;
                while ((line = br.readLine()) != null) {
                    String[] row = line.split(" ");
                    for(int i=0; i< row.length; ++i){
                        if(!row[i].isEmpty()){
                            String clusterID = row[i].trim();
                            String sampleID = String.valueOf(count);
                            if(foundClusters.containsKey(clusterID)){
                                Vector<String> rowTemp = new Vector<>(foundClusters.get(clusterID));
                                rowTemp.add(sampleID);
                                foundClusters.replace(clusterID,rowTemp);

                            }else{
                                Vector<String> rowTemp = new Vector<>();
                                rowTemp.add(sampleID);
                                foundClusters.put(clusterID,rowTemp);
                            }
                            count++;
                        }
                    }
                }
                //put the last cluster to clustersFiles
                System.out.println("\nnumber clusters: "+foundClusters.size());
                //copy all files in a cluster to folder "cluster_i", i is the id of cluster
                for (Map.Entry<String, Vector<String>> entry : foundClusters.entrySet()) {
                    String folderName = outputDir+"/cluster_" + entry.getKey();
                    System.out.println("cluster " + folderName + ", #file: " + entry.getValue().size());
                    File folder = new File(folderName);
                    if (!folder.exists()) folder.mkdir();

                    //copy source to target using Files Class
                    for (int j = 0; j < entry.getValue().size(); ++j) {
                        String sourceFilePath = fileNames.get(Integer.valueOf(entry.getValue().get(j)));
                        String[] splittedPath = sourceFilePath.split(Pattern.quote("\\"));
                        String sourceFileName = splittedPath[splittedPath.length-1];
                        String targetFilePath = folder.getAbsolutePath()+ "\\" + sourceFileName;
                        Path sourceDirectory = Paths.get(sourceFilePath);
                        Path targetDirectory = Paths.get(targetFilePath);
                        Files.copy(sourceDirectory, targetDirectory, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                System.out.println(" end.");
            }

        }catch (Exception e){
            System.out.println("\n error: createClusterPy "+e);
        }
    }

    //public void clusterNew(String inputPath, String outputDir, String numberCluster){
    public void run(){
        try{
            //read ASTs database
            readDatabase(new File(this.inputDir));
            //create csv file for clustering algorithm
            String inputDataCSV = "clusterInputData.csv";
            createCSV(inputDataCSV);

            //create output directory
            File outputDirectory = new File(this.outputDir);
            if(outputDirectory.exists()) {
                deleteDirectoryRecursion(Paths.get(this.outputDir));
                outputDirectory.mkdirs();
            }
            else
                outputDirectory.mkdirs();
            //run cluster algorithm
            String outputCluster = this.outputDir+"/outputCluster.txt";
            String python3 = "python3 ";
            String python = "python ";
            String commandStr = "hierarchical.py "+inputDataCSV+" "+outputCluster+" "+this.numberCluster;
            try{
            	Process proc = Runtime.getRuntime().exec(python3+commandStr); //Run on Mac/Linux
            	proc.waitFor();
            }catch(IOException e) {
            	try {
            		Process proc = Runtime.getRuntime().exec(python+commandStr); //Run on Windows
            		proc.waitFor();
            	}catch(IOException f) {
            		System.out.println("Couldn't run the python script to create clusters. Are you sure that python is installed ?\nIt should run with \"python\" or \"python3\"");
            		System.out.println("You should also have the python packages numpy, pandas and scipy installed (pip install packageName)");
            		System.exit(-1);
            	}
            }

            //create sub-directories
            createClusterDirPy(this.outputDir,outputCluster);

            //delete temporary files
            Files.deleteIfExists(Paths.get(inputDataCSV));
            //Files.deleteIfExists(Paths.get(outputCluster));

        }catch (Exception e){
            System.out.println("error: call cluster algorithm "+e);
        }
    }

}
