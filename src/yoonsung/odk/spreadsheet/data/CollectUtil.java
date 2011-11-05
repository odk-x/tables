package yoonsung.odk.spreadsheet.data;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Utility methods for using ODK Collect.
 */
public class CollectUtil {
    
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
