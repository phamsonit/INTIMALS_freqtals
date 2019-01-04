package be.intimals.freqt.util;

import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

// TODO you could refactor nicely but no time
public class DatabaseStatistics {
    public int nbNodes;
    public int nbLeaf; // nbNonTerminals = nbNodes - nbLeaf
    public Set<String> labelsNT = new HashSet<>(); // Non-terminals
    public Set<String> labelsT = new HashSet<>(); // Terminals
    public int nbTrees;

    // Per vertex of tree
    public int currentNonTerminals;
    public double currentFanoutMean;
    public double currentFanoutVar;

    // Per tree
    //public int nbTrees;
    public double currentMaxFanoutMean;
    public double currentMaxFanoutVar;
    public int maxFanout = Integer.MIN_VALUE;

    public double currentVerticesMean;
    public double currentVerticesVar;

    public List<Integer> fanoutList = new ArrayList<>();
    public List<Integer> maxFanoutList = new ArrayList<>();
    public List<Integer> verticesList = new ArrayList<>();

    private DatabaseStatistics() {
    }

    public static DatabaseStatistics create() {
        return new DatabaseStatistics();
    }

    public void addFanout(int vertexFanout) {
        //currentNonTerminals++;
        double oldMean = currentFanoutMean;
        currentFanoutMean = getNewMean(oldMean, vertexFanout, currentNonTerminals);

        double oldVar = currentFanoutVar;
        currentFanoutVar = getNewVar(oldVar, oldMean, currentFanoutMean, vertexFanout);
        fanoutList.add(vertexFanout);
        int debug = 0;
    }

    public void addVertices(int nbVertices) {
        //nbTrees++;
        double oldMean = currentVerticesMean;
        currentVerticesMean = getNewMean(oldMean, nbVertices, nbTrees);

        double oldVar = currentVerticesVar;
        currentVerticesVar = getNewVar(oldVar, oldMean, currentVerticesMean, nbVertices);
        verticesList.add(nbVertices);
    }

    public void addMaxFanoutVertices(int maxFanout) {
        //nbTrees++;
        double oldMean = currentMaxFanoutMean;
        currentMaxFanoutMean = getNewMean(oldMean, maxFanout, nbTrees);

        double oldVar = currentMaxFanoutVar;
        currentMaxFanoutVar = getNewVar(oldVar, oldMean, currentMaxFanoutMean, maxFanout);
        maxFanoutList.add(maxFanout);
    }

    public void updateMaxFanout(int currentFanout) {
        if (maxFanout < currentFanout) maxFanout = currentFanout;
    }

    private double getNewMean(double oldMean, double newObs, long n) {
        return oldMean + ((newObs - oldMean) / n);
    }

    private double getNewVar(double oldVar, double oldMean, double newMean, double newObs) {
        return oldVar + (newObs - oldMean) * (newObs - newMean);
    }

    public void merge(DatabaseStatistics other) {
        int sumTrees = this.nbTrees + other.nbTrees;
        double maxFanoutMean = (this.currentMaxFanoutMean * this.nbTrees + other.currentMaxFanoutMean * other.nbTrees) / sumTrees;
        double diffMaxFanout1 = this.currentMaxFanoutMean - maxFanoutMean;
        double diffMaxFanout2 = other.currentMaxFanoutMean - maxFanoutMean;
        double maxFanoutVar = (this.nbTrees * (this.currentMaxFanoutVar + Math.pow(diffMaxFanout1,2))
                + other.nbTrees * (other.currentMaxFanoutVar + Math.pow(diffMaxFanout2, 2))) / sumTrees;

        double verticesMean = (this.currentVerticesMean * this.nbTrees + other.currentVerticesMean * other.nbTrees) / sumTrees;
        double diffVertices1 = this.currentVerticesMean - verticesMean;
        double diffVertices2 = other.currentVerticesMean - verticesMean;
        double verticesVar = (this.nbTrees * (this.currentVerticesVar + Math.pow(diffVertices1, 2))
                + other.nbTrees * (other.currentVerticesVar + Math.pow(diffVertices2, 2))) / sumTrees;

        int sumNonT = this.currentNonTerminals + other.currentNonTerminals;
        double fanoutMean = (this.currentFanoutMean * this.currentNonTerminals + other.currentFanoutMean * other.currentNonTerminals) / sumNonT;
        double diffFanout1 = this.currentFanoutMean - fanoutMean;
        double diffFanout2 = other.currentFanoutMean - fanoutMean;
        double fanoutVar = (this.currentNonTerminals * (this.currentFanoutVar + Math.pow(diffFanout1, 2))
                + other.currentNonTerminals * (other.currentFanoutVar + Math.pow(diffFanout2, 2))) / sumNonT;
        

        this.nbTrees = sumTrees;
        this.currentMaxFanoutMean = maxFanoutMean;
        this.currentMaxFanoutVar = maxFanoutVar;
        this.currentVerticesMean = verticesMean;
        this.currentVerticesVar = verticesVar;

        this.currentNonTerminals = sumNonT;
        this.currentFanoutMean = fanoutMean;
        this.currentFanoutVar = fanoutVar;

        this.nbNodes += other.nbNodes;
        this.nbLeaf += other.nbLeaf;
        this.labelsT.addAll(other.labelsT);
        this.labelsNT.addAll(other.labelsNT);
        this.maxFanout = Integer.MIN_VALUE;
        assert (this.currentNonTerminals == (this.nbNodes - this.nbLeaf));

        this.fanoutList.addAll(other.fanoutList);
        this.maxFanoutList.addAll(other.maxFanoutList);
        this.verticesList.addAll(other.verticesList);
    }

    public void write() {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter("out/data.csv"));
            String[] vertices = this.verticesList.stream().map(e -> e.toString()).toArray(String[]::new);
            String[] maxFanout = this.maxFanoutList.stream().map(e -> e.toString()).toArray(String[]::new);
            String[] fanout = this.fanoutList.stream().map(e -> e.toString()).toArray(String[]::new);
            //while ((entries = reader.readNext()) != null) {
            //    ArrayList list = new ArrayList(Arrays.asList(entries));
            //    list.add(xxx); // Add the new element here
            //    writer.writeNext(list);
            //}
            writer.writeNext(new String[]{"Vertices", "MaxFanout", "Fanout"});
            for (int i = 0; i < Math.max(Math.max(vertices.length, maxFanout.length), fanout.length); i++) {
                String e1 = i < vertices.length ? vertices[i] : "";
                String e2 = i < maxFanout.length ? maxFanout[i] : "";
                String e3 = i < fanout.length ? fanout[i] : "";
                writer.writeNext(new String[]{e1, e2, e3});

            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
