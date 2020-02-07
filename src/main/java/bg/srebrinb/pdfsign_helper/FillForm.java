/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bg.srebrinb.pdfsign_helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;

/**
 *
 * @author srebr
 */
public class FillForm {

    private PDDocument document;
    String defaultAppearanceString = null;
    PDAcroForm acroForm = null;
    public FillForm(PDDocument document) {
        this.document = document;

    }

    public void addFontDefaultResources(String fontFile, int sizeFont) throws FileNotFoundException, IOException {
        PDFont font = PDType0Font.load(getDocument(), new FileInputStream(fontFile), false); // check that the font has what you need; ARIALUNI.TTF is good but huge
        PDResources resources = new PDResources();
        resources.put(COSName.getPDFName("arialbd"), font);
        acroForm = getDocument().getDocumentCatalog().getAcroForm();
        acroForm.setDefaultResources(resources);
        defaultAppearanceString = String.format("/arialbd %d Tf 0 g", sizeFont);
    }
    public static PDDocument fastPopulate(PDDocument document,Map<String, String> data) throws IOException{
        FillForm form=new FillForm(document);
        return form.populate(data, false);
    }
    public PDDocument populate(Map<String, String> data, boolean flatten) throws IOException {
        if (null == acroForm) {
            acroForm = getDocument().getDocumentCatalog().getAcroForm();
        }
        Iterator<PDField> fields = acroForm.getFieldIterator();
        while (fields.hasNext()) {
            System.out.println("name:" + fields.next().getPartialName());
        }
        if (defaultAppearanceString != null) {
            acroForm.setDefaultAppearance(defaultAppearanceString);
        }
        for (Map.Entry<String, String> item : data.entrySet()) {
            String key = item.getKey();            
            PDField field = acroForm.getField(key);
            if (field != null) {
                if (field instanceof PDTextField) {                    
                    PDTextField textBox = (PDTextField) field;
                    if (null != defaultAppearanceString) {
                        textBox.setDefaultAppearance(defaultAppearanceString);
                    }
                    try{
                    setField(key, item.getValue());                    
                    }catch(IllegalArgumentException iae){
                        System.out.println("field:"+key);
                        iae.printStackTrace();
                    }
                } else if (field instanceof PDCheckBox) {
                    if (item.getValue().endsWith("true")) {                     
                        setField(key, item.getValue());
                    }                    
                } else {
                    System.err.println("Unexpected form field type found with placeholder name: '" + key + "'"
                            + field.getFieldType());
                }
            } else {
                System.err.println("No field found with name:" + key);
            }
        }

        // you can optionally flatten the document to merge acroform lay to main one
        if (flatten) {
            acroForm.flatten();
        }

        return getDocument();
    }

    /**
     * @return the document
     */
    public PDDocument getDocument() {
        return document;
    }

    /**
     * @param document the document to set
     */
    public void setDocument(PDDocument document) {
        this.document = document;
    }

    public void setField(String name, String Value) throws IOException {
        // PDDocumentCatalog docCatalog = this.document.getDocumentCatalog();
        // PDAcroForm acroForm = docCatalog.getAcroForm();
        PDField field = acroForm.getField(name);
        if (field==null) return;
        if (field instanceof PDCheckBox) {
            ((PDCheckBox) field).check();
        } else if (field instanceof PDTextField) {            
            field.setValue(Value);            
        } 
        try{        
        COSDictionary fieldDictionary = field.getCOSObject();
        COSDictionary dictionary = (COSDictionary) fieldDictionary.getDictionaryObject(COSName.AP);
        dictionary.setNeedToBeUpdated(true);
        if (field instanceof PDTextField) {
            COSStream stream = (COSStream) dictionary.getDictionaryObject(COSName.N);
            stream.setNeedToBeUpdated(true);
            while (fieldDictionary != null) {
                fieldDictionary.setNeedToBeUpdated(true);
                fieldDictionary = (COSDictionary) fieldDictionary.getDictionaryObject(COSName.PARENT);
            }
        }
        }catch(Exception ex){
            Logger.getLogger(FillForm.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }
}
