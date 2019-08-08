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

    private static String sep = File.separator;

//    public String format(String input) {
//        return prettyFormat(input, "2");
//    }

    public static String prettyFormat(String input, String indent) {
        Source xmlInput = new StreamSource(new StringReader(input));
        StringWriter stringWriter = new StringWriter();
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", indent);
            transformer.transform(xmlInput, new StreamResult(stringWriter));
            return stringWriter.toString().trim();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //format one xml file
    public void format(String inPath, String outPath){
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
            FileWriter file = new FileWriter(outPath);
            file.write(prettyFormat(fileContent, "2"));
            file.flush();
            file.close();

        }catch (Exception e){System.out.println(e);}


    }

    //format all xml in the inPath directory
    public void formatXML(String inPath, String outPath) {
        try {
            //System.out.print("create tree data: ");
            //String inPath = args[0];
            //String outPath = args[1];

            File directory = new File(outPath);
            if(!directory.exists()) {
                directory.mkdir();
            }

            File f = new File(inPath);
            File[] subdir = f.listFiles();
            Arrays.sort(subdir);

            for (File fi : subdir) {
                if (fi.isFile() && fi.getName().charAt(0)!='.' ) {
                    String[] fullName = fi.getName().split("\\.");
                    String ext = fullName[fullName.length - 1];
                    String fileName = fullName[0];
                    if(ext.toLowerCase().equals("xml")){
                        System.out.print("reading file ---------------- ");
                        System.out.println(f+sep+fi.getName());
                        String inFile = f+sep+fi.getName();
                        format(inFile,outPath+sep+fileName+".xml");
                    }
                }else
                if (fi.isDirectory()) {

                }
            }

        } catch (Exception e) { System.out.println("input error");}


    }
}