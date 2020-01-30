package bg.srebrinb.pdfsign_helper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDPushButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;

/**
 * <a href="https://stackoverflow.com/questions/46799087/how-to-insert-image-programmatically-in-to-acroform-field-using-java-pdfbox">
 * How to insert image programmatically in to AcroForm field using java PDFBox?
 * </a>
 * <p>
 * This utility class is from Renat Gatin's answer to the question linked above.
 * </p>
 * <p>
 * A flag parameter <code>flatten</code> was added to <code>populateAndCopy</code> to allow deciding whether or not to flatten the
 * form.
 * </p>
 *
 * @author Renat Gatin
 */
public class AcroFormPopulator {

    public static void main(String[] args) {
        AcroFormPopulator abd = new AcroFormPopulator();
        try {
            /* Map<String, String> data = new HashMap<>();
            data.put("firstName", "Mike");
            data.put("lastName", "Taylor");
            data.put("dateTime", (new Date()).toString());
            data.put("photo_af_image", "photo1.jpg");
            data.put("photo2_af_image", "photo2.jpg");
            data.put("photo3_af_image", "photo3.jpg");
             */
            
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> data = new HashMap<String, String>();
            String json="{'person_firstName':''}";
// convert JSON string to Map
            data = mapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });            
            abd.populateAndCopy("test.pdf", "generated.pdf", data, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void populateAndCopy(String originalPdf, String targetPdf, Map<String, String> data, boolean flatten) throws IOException {
        File file = new File(originalPdf);
        PDDocument document = PDDocument.load(file);
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

        for (Map.Entry<String, String> item : data.entrySet()) {
            String key = item.getKey();
            PDField field = acroForm.getField(key);
            if (field != null) {
                System.out.print("Form field with placeholder name: '" + key + "' found");

                if (field instanceof PDTextField) {
                    System.out.println("(type: " + field.getClass().getSimpleName() + ")");
                    field.setValue(item.getValue());
                    System.out.println("value is set to: '" + item.getValue() + "'");

                } else if (field instanceof PDPushButton) {
                    System.out.println("(type: " + field.getClass().getSimpleName() + ")");
                    PDPushButton pdPushButton = (PDPushButton) field;

                    List<PDAnnotationWidget> widgets = pdPushButton.getWidgets();
                    if (widgets != null && widgets.size() > 0) {
                        PDAnnotationWidget annotationWidget = widgets.get(0); // just need one widget

                        String filePath = item.getValue();
                        File imageFile = new File(filePath);

                        if (imageFile.exists()) {
                            /*
                             * BufferedImage bufferedImage = ImageIO.read(imageFile); 
                             * PDImageXObject pdImageXObject = LosslessFactory.createFromImage(document, bufferedImage);
                             */
                            PDImageXObject pdImageXObject = PDImageXObject.createFromFile(filePath, document);
                            float imageScaleRatio = (float) pdImageXObject.getHeight() / (float) pdImageXObject.getWidth();

                            PDRectangle buttonPosition = getFieldArea(pdPushButton);
                            float height = buttonPosition.getHeight();
                            float width = height / imageScaleRatio;
                            float x = buttonPosition.getLowerLeftX();
                            float y = buttonPosition.getLowerLeftY();

                            PDAppearanceStream pdAppearanceStream = new PDAppearanceStream(document);
                            pdAppearanceStream.setResources(new PDResources());
                            try (PDPageContentStream pdPageContentStream = new PDPageContentStream(document, pdAppearanceStream)) {
                                pdPageContentStream.drawImage(pdImageXObject, x, y, width, height);
                            }
                            pdAppearanceStream.setBBox(new PDRectangle(x, y, width, height));

                            PDAppearanceDictionary pdAppearanceDictionary = annotationWidget.getAppearance();
                            if (pdAppearanceDictionary == null) {
                                pdAppearanceDictionary = new PDAppearanceDictionary();
                                annotationWidget.setAppearance(pdAppearanceDictionary);
                            }

                            pdAppearanceDictionary.setNormalAppearance(pdAppearanceStream);
                            System.out.println("Image '" + filePath + "' inserted");

                        } else {
                            System.err.println("File " + filePath + " not found");
                        }
                    } else {
                        System.err.println("Missconfiguration of placeholder: '" + key + "' - no widgets(actions) found");
                    }
                } else {
                    System.err.print("Unexpected form field type found with placeholder name: '" + key + "'");
                }
            } else {
                System.err.println("No field found with name:" + key);
            }
        }

        // you can optionally flatten the document to merge acroform lay to main one
        if (flatten) {
            acroForm.flatten();
        }

        document.save(targetPdf);
        document.close();
        System.out.println("Done");
    }

    private PDRectangle getFieldArea(PDField field) {
        COSDictionary fieldDict = field.getCOSObject();
        COSArray fieldAreaArray = (COSArray) fieldDict.getDictionaryObject(COSName.RECT);
        return new PDRectangle(fieldAreaArray);
    }
}
