/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bg.srebrinb.pdfsign_helper;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author sbalabanov
 */
public class App {
    private PDDocument document;
    public static void main(String[] args) throws IOException {
        String dataFile="data.json";
        App app=new App();
        File file = new File("form.pdf");
        app.document = PDDocument.load(file);
        Map<String, String> data = app.getData(dataFile);
        boolean flatten = false;
        FillForm fillForm = new FillForm(app.document);
        String fontFile = "fonts/ariblk.ttf";
        int sizeFont = 7;

        fillForm.addFontDefaultResources(fontFile, sizeFont);        
        PDDocument result = fillForm.populate(data, flatten);
        ByteArrayOutputStream tmp=new ByteArrayOutputStream();
        result.save(tmp);
        PDDocument tmpPDD=PDDocument.load(tmp.toByteArray());
        Long signatureInx=app.getSignIdx(dataFile);
        String reason = "reason Text";        
        SignAndLockExistingField signAndLockExistingField = new SignAndLockExistingField(tmpPDD);
        try {
            signAndLockExistingField.setKeyStore("test.pfx", "1234".toCharArray());
        } catch (KeyStoreException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        } catch (CertificateException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnrecoverableKeyException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
        OutputStream output = new FileOutputStream("sign_1.pdf");
        signAndLockExistingField.signAndLock(signatureInx.intValue(), reason, output);
        output.close();
        
        dataFile="data2.json";
        data = app.getData(dataFile);
        signatureInx=app.getSignIdx(dataFile);
        
        fillForm.setDocument(PDDocument.load(new File("sign_1.pdf")));
        result = fillForm.populate(data, flatten);
        tmp=new ByteArrayOutputStream();
        result.save(tmp);
        result.save("tmp2.pdf");
        tmpPDD=PDDocument.load(tmp.toByteArray());
        signAndLockExistingField.setDocument(tmpPDD);
        output = new FileOutputStream("sign_2.pdf");
        signAndLockExistingField.signAndLock(signatureInx.intValue(), reason, output);
        output.close();
        
    }
    Long getSignIdx(String file) {
        JSONParser parser = new JSONParser();
        Long signatureInx =null;
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            Object obj = parser.parse(bufferedReader);

            JSONObject jsonObject = (JSONObject) obj;
            signatureInx = (Long) jsonObject.get("signatureInx");            

        } catch (Exception e) {
            e.printStackTrace();
        }
        return signatureInx;
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
}
