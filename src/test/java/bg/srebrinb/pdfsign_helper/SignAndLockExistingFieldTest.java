/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bg.srebrinb.pdfsign_helper;

import static bg.srebrinb.pdfsign_helper.FillFormTest.document;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author sbalabanov
 */
public class SignAndLockExistingFieldTest {

    public SignAndLockExistingFieldTest() {
    }

    @BeforeClass
    public static void setUpClass() {

    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws IOException {
        File file = new File("generated.pdf");
        document = PDDocument.load(file);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of signAndLock method, of class SignAndLockExistingField.
     */
    @Test
    public void testSignAndLock() throws Exception {
        System.out.println("signAndLock");
        int signatureInx = 0;
        String reason = "reason Text";        
        SignAndLockExistingField instance = new SignAndLockExistingField(document);
        instance.setKeyStore("test.pfx", "1234".toCharArray());
        OutputStream output = new FileOutputStream("sign_person.pdf");
        instance.signAndLock(signatureInx, reason, output);
        output.close();
        File file = new File("sign_person1.pdf");
        document = PDDocument.load(file);        
        instance = new SignAndLockExistingField(document);
        instance.setKeyStore("test.pfx", "1234".toCharArray());
        OutputStream output2 = new FileOutputStream("sign_second.pdf");
        instance.signAndLock(1, "Second", output2);
        output2.close();
        
    }

    
}
