/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bg.srebrinb.pdfsign_helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;

/**
 *
 * @author sbalabanov
 */
public class FixForm {

    public static void main(String[] args) throws IOException {
        File file = new File("dogovorUPF.pdf");
        PDDocument document = PDDocument.load(file);
        PDFont font = PDType0Font.load(document, new FileInputStream("fonts/ariblk.ttf"), false); // check that the font has what you need; ARIALUNI.TTF is good but huge
        PDResources resources = new PDResources();
        resources.put(COSName.getPDFName("arialbd"), font);
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();        
        acroForm.setDefaultResources(resources);
        
        String defaultAppearanceString = String.format("/arialbd %d Tf 0 g", 7);
        Iterator<PDField> fields = acroForm.getFieldIterator();
        while (fields.hasNext()) {
          //  System.out.println("name:" + fields.next().getPartialName());
            PDField field = fields.next();
            if (field instanceof PDTextField) {
                
                PDTextField textBox = (PDTextField) field;

                // adjust to replace existing font name
                textBox.setDefaultAppearance(defaultAppearanceString);
                
            }
        }
        //acroForm.setNeedAppearances(true);
        document.save("fix_dogovorUPF.pdf");

    }
}
