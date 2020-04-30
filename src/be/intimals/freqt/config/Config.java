package be.intimals.freqt.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Config {
    private String path;
    private Properties prop;

    public Config(String configPath) throws IOException {
        path = configPath;
        prop = new Properties();
        InputStream input = new FileInputStream(configPath);
        prop.load(input);
        input.close();
    }



    public boolean get2Class(){return Boolean.valueOf(prop.getProperty("2Class"));}

    public double getDSScore(){return Double.valueOf(prop.getProperty("minDSScore"));}

    public boolean keepHighestScore(){return Boolean.valueOf(prop.getProperty("keepHighestScore"));}

    public int getNumPatterns(){return Integer.valueOf(prop.getProperty("numPatterns"));}

    public String getInputFiles1() {return prop.getProperty("inputPath1"); }

    public String getInputFiles2() {
        return prop.getProperty("inputPath2");
    }

    public String getOutputMatches() {return prop.getProperty("outputMatches");}

    public String getOutputClusters() {return prop.getProperty("outputClusters");}

    public String getOutputClustersTemp() {return prop.getProperty("outputClustersTemp");}

    public String getOutputCommonPatterns() {return prop.getProperty("outputCommonPatterns");}

    public String getOutputCommonMatches() {return prop.getProperty("outputCommonMatches");}

    public String getOutputCommonClusters() {return prop.getProperty("outputCommonClusters");}

    public String getOutputMatches1() {return prop.getProperty("outputMatches1");}

    public String getOutputClusters1() {return prop.getProperty("outputClusters1");}

    public String getOutputMatches2() {return prop.getProperty("outputMatches2");}

    public String getOutputClusters2() {return prop.getProperty("outputClusters2");}

    public boolean getWeighted(){return Boolean.valueOf(prop.getProperty("weighted"));}


    ///////////////


    public boolean getTwoStep(){return Boolean.valueOf(prop.getProperty("twoStep"));}

    public boolean getFilter(){return Boolean.valueOf(prop.getProperty("filter"));}

    public boolean getAbstractLeafs(){return Boolean.valueOf(prop.getProperty("abstractLeafs"));}

    public int getTimeout(){return Integer.valueOf(prop.getProperty("timeout"));}

    public Properties getProp(){
        return this.prop;
    }

    public boolean buildGrammar() {
        return Boolean.valueOf(prop.getProperty("buildGrammar"));
    }

    public String getGrammarFile() {
        return prop.getProperty("grammarFile");
    }

    public String getRootLabelFile() {
        return prop.getProperty("rootLabelFile");
    }

    public String getWhiteLabelFile() {
        return prop.getProperty("whiteLabelFile");
    }

    public String getXmlCharacterFile() {
        return prop.getProperty("xmlCharacterFile");
    }

    public String getInputFiles() {
        return prop.getProperty("inputPath");
    }

    public String getOutputFile() {
        return prop.getProperty("outputPath");
    }

    public int getMinSupport() {
        return Integer.valueOf(prop.getProperty("minSupport"));
    }

    public int getMinNode() {
        return Integer.valueOf(prop.getProperty("minNode"));
    }

    public int getMaxNode() {
        return Integer.valueOf(prop.getProperty("maxNode"));
    }

    public int getMinLeaf() {
        return Integer.valueOf(prop.getProperty("minLeaf"));
    }

    public int getMaxLeaf() {
        return Integer.valueOf(prop.getProperty("maxLeaf"));
    }

    public boolean postProcess(){return Boolean.valueOf(prop.getProperty("pos"));}

    /**
     * Returns a list of minimum-support values (only used when executing multiple Freq-T runs in parallel)
     * @return
     */
    public List<Integer> getMinSupportList() {
        String msList = prop.getProperty("minSupportList");
        List<Integer> result = new ArrayList<>();
        for(String ms: msList.split(",")) {
            result.add(Integer.valueOf(ms));
        }
        return result;
    }

    /**
     * Returns a list of input folders (only used when executing multiple Freq-T runs in parallel)
     * @return
     */
    public List<String> getInputFilesList() {
        String ifList = prop.getProperty("inFilesList");
        return Arrays.asList(ifList.split(","));
    }

}

