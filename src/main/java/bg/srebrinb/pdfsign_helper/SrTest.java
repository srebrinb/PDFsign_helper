/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bg.srebrinb.pdfsign_helper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTerminalField;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cms.CMSAbsentContent;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle.cms.SignerInfoGeneratorBuilder;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

/**
 *
 * @author srebrin
 */
public class SrTest {

    public static final String KEYSTORE = "keystores/demo-rsa2048.ks";
    public static final char[] PASSWORD = "demo-rsa2048".toCharArray();

    public static KeyStore ks = null;
    public static PrivateKey pk = null;
    public static Certificate[] chain = null;
    public static final COSName COS_NAME_LOCK = COSName.getPDFName("Lock");
    public static final COSName COS_NAME_ACTION = COSName.getPDFName("Action");
    public static final COSName COS_NAME_ALL = COSName.getPDFName("All");
    public static final COSName COS_NAME_SIG_FIELD_LOCK = COSName.getPDFName("SigFieldLock");

    public static void main(String[] args) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        SrTest t = new SrTest();
        BouncyCastleProvider bcp = new BouncyCastleProvider();
        Security.addProvider(bcp);
        //Security.insertProviderAt(bcp, 1);

        ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(new FileInputStream(KEYSTORE), PASSWORD);
        String alias = (String) ks.aliases().nextElement();
        pk = (PrivateKey) ks.getKey(alias, PASSWORD);
        chain = ks.getCertificateChain(alias);
        try (InputStream resource = new FileInputStream("/home/srebrin/Documents/junk/2_sign.pdf");
                OutputStream result = new FileOutputStream(new File("/home/srebrin/Documents/junk/", "3_sign.pdf"));
                PDDocument pdDocument = PDDocument.load(resource)) {
            List<PDField> field = pdDocument.getDocumentCatalog().getAcroForm().getFields();
            for (PDField pDField : field) {
                System.out.println("pDField = " + pDField);
            }

            pdDocument.getDocumentCatalog().getAcroForm().getField("Text3").setValue("Text3");
            pdDocument.getDocumentCatalog().getAcroForm().getField("Text2").setValue("Text2");
            t.signAndLockExistingFieldWithLock(pdDocument, result, data -> t.signWithSeparatedHashing(data));
        }
    }

    void signAndLockExistingFieldWithLock(PDDocument document, OutputStream output, SignatureInterface signatureInterface) throws IOException {
        PDSignatureField signatureField = document.getSignatureFields().get(2);
        PDSignature signature = new PDSignature();
        signatureField.setValue(signature);

        COSBase lock = signatureField.getCOSObject().getDictionaryObject(COS_NAME_LOCK);
        if (lock instanceof COSDictionary) {
            COSDictionary lockDict = (COSDictionary) lock;
            COSDictionary transformParams = new COSDictionary(lockDict);
            transformParams.setItem(COSName.TYPE, COSName.getPDFName("TransformParams"));
            transformParams.setItem(COSName.V, COSName.getPDFName("1.2"));
            transformParams.setDirect(true);
            COSDictionary sigRef = new COSDictionary();
            sigRef.setItem(COSName.TYPE, COSName.getPDFName("SigRef"));
            sigRef.setItem(COSName.getPDFName("TransformParams"), transformParams);
            sigRef.setItem(COSName.getPDFName("TransformMethod"), COSName.getPDFName("FieldMDP"));
            sigRef.setItem(COSName.getPDFName("Data"), document.getDocumentCatalog());
            sigRef.setDirect(true);
            COSArray referenceArray = new COSArray();
            referenceArray.add(sigRef);
            signature.getCOSObject().setItem(COSName.getPDFName("Reference"), referenceArray);

            final Predicate<PDField> shallBeLocked;
            final COSArray fields = lockDict.getCOSArray(COSName.FIELDS);
            final List<String> fieldNames = fields == null ? Collections.emptyList()
                    : fields.toList().stream().filter(c -> (c instanceof COSString)).map(s -> ((COSString) s).getString()).collect(Collectors.toList());
            final COSName action = lockDict.getCOSName(COSName.getPDFName("Action"));
            if (action.equals(COSName.getPDFName("Include"))) {
                shallBeLocked = f -> fieldNames.contains(f.getFullyQualifiedName());
            } else if (action.equals(COSName.getPDFName("Exclude"))) {
                shallBeLocked = f -> !fieldNames.contains(f.getFullyQualifiedName());
            } else if (action.equals(COSName.getPDFName("All"))) {
                shallBeLocked = f -> true;
            } else { // unknown action, lock nothing
                shallBeLocked = f -> false;
            }
            lockFields(document.getDocumentCatalog().getAcroForm().getFields(), shallBeLocked);
        }

        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
        signature.setName("blablabla");
        signature.setLocation("blablabla");
        signature.setReason("blablabla");
        signature.setSignDate(Calendar.getInstance());
        document.addSignature(signature);
        ExternalSigningSupport externalSigning
                = document.saveIncrementalForExternalSigning(output);
        // invoke external signature service
        byte[] cmsSignature = signatureInterface.sign(externalSigning.getContent());
        // set signature bytes received from the service
        externalSigning.setSignature(cmsSignature);
    }

    boolean lockFields(List<PDField> fields, Predicate<PDField> shallBeLocked) {
        boolean isUpdated = false;
        if (fields != null) {
            for (PDField field : fields) {
                boolean isUpdatedField = false;
                if (shallBeLocked.test(field)) {
                    field.setFieldFlags(field.getFieldFlags() | 1);
                    if (field instanceof PDTerminalField) {
                        for (PDAnnotationWidget widget : ((PDTerminalField) field).getWidgets()) {
                            widget.setLocked(true);
                        }
                    }
                    isUpdatedField = true;
                }
                if (field instanceof PDNonTerminalField) {
                    if (lockFields(((PDNonTerminalField) field).getChildren(), shallBeLocked)) {
                        isUpdatedField = true;
                    }
                }
                if (isUpdatedField) {
                    field.getCOSObject().setNeedToBeUpdated(true);
                    isUpdated = true;
                }
            }
        }
        return isUpdated;
    }

    public byte[] signWithSeparatedHashing(InputStream content) throws IOException {
        try {
            // Digest generation step
            MessageDigest md = MessageDigest.getInstance("SHA256", "BC");
            byte[] digest = md.digest(IOUtils.toByteArray(content));

            // Separate signature container creation step
            List<Certificate> certList = Arrays.asList(chain);
            JcaCertStore certs = new JcaCertStore(certList);

            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

            Attribute attr = new Attribute(CMSAttributes.messageDigest,
                    new DERSet(new DEROctetString(digest)));

            ASN1EncodableVector v = new ASN1EncodableVector();

            v.add(attr);

            SignerInfoGeneratorBuilder builder = new SignerInfoGeneratorBuilder(new BcDigestCalculatorProvider())
                    .setSignedAttributeGenerator(new DefaultSignedAttributeTableGenerator(new AttributeTable(v)));

            AlgorithmIdentifier sha256withRSA = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256withRSA");

            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            InputStream in = new ByteArrayInputStream(chain[0].getEncoded());
            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(in);

            gen.addSignerInfoGenerator(builder.build(
                    new BcRSAContentSignerBuilder(sha256withRSA,
                            new DefaultDigestAlgorithmIdentifierFinder().find(sha256withRSA))
                            .build(PrivateKeyFactory.createKey(pk.getEncoded())),
                    new JcaX509CertificateHolder(cert)));

            gen.addCertificates(certs);

            CMSSignedData s = gen.generate(new CMSAbsentContent(), false);
            return s.getEncoded();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }

}
