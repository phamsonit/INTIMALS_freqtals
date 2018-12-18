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

    public boolean buildGrammar() {
        return Boolean.valueOf(prop.getProperty("buildGrammar"));
    }

    public String getGrammarFile() {
        return prop.getProperty("grammarFile");
    }

    public String getRootLabelFile() {
        return prop.getProperty("rootLabelFile");
    }

    public String getBlackLabelFile() {
        return prop.getProperty("blackLabelFile");
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
        return Integer.valueOf(prop.getProperty("minsup"));
    }

    public int getMinPatternSize() {
        return Integer.valueOf(prop.getProperty("minpat"));
    }

    public int getMaxPatternSize() {
        return Integer.valueOf(prop.getProperty("maxpat"));
    }

    public int getMinLeaf() {
        return Integer.valueOf(prop.getProperty("minleaf"));
    }

    public int getMaxLeaf() {
        return Integer.valueOf(prop.getProperty("maxleaf"));
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

    public  int getMaxTimeLabel(){
        return Integer.valueOf(prop.getProperty("maxtimelabel"));
    }

    public  int getMaxRepeatLabel(){
        return Integer.valueOf(prop.getProperty("maxrepeatlabel"));
    }

    public  int getLineDistance(){
        return Integer.valueOf(prop.getProperty("linedistance"));
    }


}

