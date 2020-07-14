package be.intimals.freqt.util;

import org.w3c.dom.Document;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Vector;
import java.io.File;
import java.io.FileWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Utility Class for formatting XML
 *
 * @author Pankaj
 *
 */
public class XmlFormatter {

    private static String sep = "/";//File.separator;

    //format one xml file
    public static void format(String inPath, String outPath){
        try{
            BufferedReader br = new BufferedReader(new FileReader(inPath));
            String fileContent = "";
            String uniCode = "\uFEFF";
            String line = br.readLine();
            if(line.contains(uniCode)){
                line = line.substring(1);
            }
            fileContent += line.trim();
            while ((line = br.readLine()) != null) {
                if( ! line.isEmpty() )
                    fileContent += line.trim();
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StringWriter stringWriter = new StringWriter();
            transformer.transform(
                    new StreamSource(new StringReader(fileContent)),
                    new StreamResult(stringWriter));

            FileWriter file = new FileWriter(outPath);
            file.write(stringWriter.toString().trim());
            file.flush();
            file.close();
        }catch (Exception e){
            System.out.println("Exception while reading file " + inPath);
            e.printStackTrace();
            System.exit(-1);
        }
    }
}