package be.intimals.freqt.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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


    public int getConfigID(){
        return Integer.valueOf(prop.getProperty("configID"));
    }

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
        return prop.getProperty("inFiles");
    }

    public String getOutputFile() {
        return prop.getProperty("outFile");
    }

    public int getMinSupport() {
        return Integer.valueOf(prop.getProperty("minSupport"));
    }

    public int getMinPatternSize() {
        return Integer.valueOf(prop.getProperty("minPatternSize"));
    }

    public int getMaxPatternSize() {
        return Integer.valueOf(prop.getProperty("maxPatternSize"));
    }

    public int getMinLeaf() {
        return Integer.valueOf(prop.getProperty("minLeafSize"));
    }

    public int getMaxLeaf() {
        return Integer.valueOf(prop.getProperty("maxLeafSize"));
    }

    public boolean addLocations() {
        return Boolean.valueOf(prop.getProperty("where"));
    }

    public boolean outputAsXML() {
        return Boolean.valueOf(prop.getProperty("xml"));
    }

    public boolean outputAsENC() {
        return Boolean.valueOf(prop.getProperty("enc"));
    }

    public boolean postProcess(){return Boolean.valueOf(prop.getProperty("post"));}

    public  int getMaxRepeatLabel(){
        return Integer.valueOf(prop.getProperty("maxRepeatLabel"));
    }

    public  int getMinLineDistance(){
        return Integer.valueOf(prop.getProperty("minLineDistance"));
    }

    public  int getMaxLineDistance(){
        return Integer.valueOf(prop.getProperty("maxLineDistance"));
    }




}

