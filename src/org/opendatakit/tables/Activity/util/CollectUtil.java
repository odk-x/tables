package org.opendatakit.tables.Activity.util;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Utility methods for using ODK Collect.
 */
public class CollectUtil {
    
    public static void buildForm(String filename, String[] keys, String title,
            String id) {
        try {
            FileWriter writer = new FileWriter(filename);
            writer.write("<h:html xmlns=\"http://www.w3.org/2002/xforms\" " +
                    "xmlns:h=\"http://www.w3.org/1999/xhtml\" " +
                    "xmlns:ev=\"http://www.w3.org/2001/xml-events\" " +
                    "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                    "xmlns:jr=\"http://openrosa.org/javarosa\">");
            writer.write("<h:head>");
            writer.write("<h:title>" + title + "</h:title>");
            writer.write("<model>");
            writer.write("<instance>");
            writer.write("<data id=\"" + id + "\">");
            for (String key : keys) {
                writer.write("<" + key + "/>");
            }
            writer.write("</data>");
            writer.write("</instance>");
            writer.write("<itext>");
            writer.write("<translation lang=\"eng\">");
            for (String key : keys) {
                writer.write("<text id=\"/data/" + key + ":label\">");
                writer.write("<value>" + key + "</value>");
                writer.write("</text>");
            }
            writer.write("</translation>");
            writer.write("</itext>");
            writer.write("</model>");
            writer.write("</h:head>");
            writer.write("<h:body>");
            for (String key : keys) {
                writer.write("<input ref=\"/data/" + key + "\">");
                writer.write("<label ref=\"jr:itext('/data/" + key +
                        ":label')\"/>");
                writer.write("</input>");
            }
            writer.write("</h:body>");
            writer.write("</h:html>");
            writer.close();
        } catch(IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public static String getJrFormId(String filepath) {
        Document formDoc = parseForm(filepath);
        String namespace = formDoc.getRootElement().getNamespace();
        Element hhtmlEl = formDoc.getElement(namespace, "h:html");
        Element hheadEl = hhtmlEl.getElement(namespace, "h:head");
        Element modelEl = hheadEl.getElement(namespace, "model");
        Element instanceEl = modelEl.getElement(namespace, "instance");
        Element dataEl = instanceEl.getElement(1);
        return dataEl.getAttributeValue(namespace, "id");
    }
    
    private static Document parseForm(String filepath) {
        Document formDoc = new Document();
        KXmlParser formParser = new KXmlParser();
        try {
            formParser.setInput(new FileReader(filepath));
            formDoc.parse(formParser);
        } catch(FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch(XmlPullParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch(IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return formDoc;
    }
}
