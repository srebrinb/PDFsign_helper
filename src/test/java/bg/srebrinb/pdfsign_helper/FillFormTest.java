/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bg.srebrinb.pdfsign_helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author srebr
 */
public class FillFormTest {

    static PDDocument document;

    public FillFormTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws IOException, Exception {
        File file = new File("form.pdf");
        document = PDDocument.load(file);
        testAddFontDefaultResources();
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of addFontDefaultResources method, of class FillForm.
     */
    @Test
    public void testAddFontDefaultResources() throws Exception {
        System.out.println("addFontDefaultResources");
        String fontFile = "fonts/ariblk.ttf";
        int sizeFont = 7;

        FillForm instance = new FillForm(document);
        instance.addFontDefaultResources(fontFile, sizeFont);
    }

    Map<String, String> getData(String file) {
        JSONParser parser = new JSONParser();
        Map<String, String> data = new HashMap<>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            Object obj = parser.parse(bufferedReader);

            JSONObject jsonObject = (JSONObject) obj;
            JSONObject formDataObj = (JSONObject) jsonObject.get("data");

            formDataObj.keySet().forEach(new Consumer() {
                @Override
                public void accept(Object keySet) {
                    data.put((String) keySet, (String) formDataObj.get(keySet));
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    /**
     * Test of populate method, of class FillForm.
     */
    @Test
    public void testPopulate() throws Exception {
        System.out.println("populate");
        Map<String, String> data = getData("data.json");

        boolean flatten = false;
        FillForm instance = new FillForm(document);
        String fontFile = "fonts/ariblk.ttf";
        int sizeFont = 7;

        instance.addFontDefaultResources(fontFile, sizeFont);
        PDDocument expResult = null;
        PDDocument result = instance.populate(data, flatten);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}
