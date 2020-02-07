/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bg.srebrinb.pdfsign_helper;


import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.text.SimpleDateFormat;
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
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTerminalField;
import org.apache.pdfbox.util.Matrix;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cms.CMSAbsentContent;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle.cms.SignerInfoGeneratorBuilder;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

/**
 *
 * @author sbalabanov
 */
public class SignAndLockExistingField {

    private PDDocument document;
    String defaultAppearanceString = null;
    PDAcroForm acroForm = null;
    private File imageFile = new File("CertBG.png");
    ;
    public static KeyStore ks = null;
    public static PrivateKey pk = null;
    public static Certificate[] chain = null;
    public static final COSName COS_NAME_LOCK = COSName.getPDFName("Lock");
    public static final COSName COS_NAME_ACTION = COSName.getPDFName("Action");
    public static final COSName COS_NAME_ALL = COSName.getPDFName("All");
    public static final COSName COS_NAME_SIG_FIELD_LOCK = COSName.getPDFName("SigFieldLock");

    public SignAndLockExistingField(PDDocument document) {
        this.document = document;
    }

    public void signAndLock(int signatureInx,String reason,OutputStream output) throws IOException {
     //   ByteArrayOutputStream output=new ByteArrayOutputStream();
        SignatureInterface signatureInterface= data -> this.signWithSeparatedHashing(data);
        PDSignatureField signatureField = getDocument().getSignatureFields().get(signatureInx);        
        int pageNum=signatureField.getWidgets().get(0).getPage().getStructParents();
        PDSignature signature = new PDSignature();
        signatureField.setValue(signature);
        
        PDRectangle rect = signatureField.getWidgets().get(0).getRectangle();

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
            sigRef.setItem(COSName.getPDFName("Data"), getDocument().getDocumentCatalog());
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
            lockFields(getDocument().getDocumentCatalog().getAcroForm().getFields(), shallBeLocked);
        }

        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
        X509Certificate cert = (X509Certificate) chain[0];
        // https://stackoverflow.com/questions/2914521/
        X500Name x500Name = new X500Name(cert.getSubjectX500Principal().getName());
        RDN cn = x500Name.getRDNs(BCStyle.CN)[0];
        String name = IETFUtils.valueToString(cn.getFirst().getValue());
        signature.setName(name);
        //signature.setLocation("blablabla");
        signature.setReason(reason);
        signature.setSignDate(Calendar.getInstance());

        // register signature dictionary and sign interface
        SignatureOptions signatureOptions = new SignatureOptions();
        signatureOptions.setVisualSignature(createVisualSignatureTemplate(getDocument(), pageNum, rect, signature));
        
        signatureOptions.setPage(pageNum);

        getDocument().addSignature(signature, signatureInterface, signatureOptions);
        ExternalSigningSupport externalSigning
                = getDocument().saveIncrementalForExternalSigning(output);
        // invoke external signature service
        byte[] cmsSignature = signatureInterface.sign(externalSigning.getContent());
        // set signature bytes received from the service
        externalSigning.setSignature(cmsSignature);
        //return output;
    }

    boolean lockFields(List<PDField> fields, Predicate<PDField> shallBeLocked) {
        boolean isUpdated = false;
        if (fields != null) {
            for (PDField field : fields) {
                if (field==null) break;
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
            throw new IOException(e);
        }
    }

    private InputStream createVisualSignatureTemplate(PDDocument srcDoc, int pageNum,
            PDRectangle rect, PDSignature signature) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(srcDoc.getPage(pageNum).getMediaBox());
            doc.addPage(page);
            PDAcroForm acroForm = new PDAcroForm(doc);
            doc.getDocumentCatalog().setAcroForm(acroForm);
            PDSignatureField signatureField = new PDSignatureField(acroForm);
            PDAnnotationWidget widget = signatureField.getWidgets().get(0);
            List<PDField> acroFormFields = acroForm.getFields();
            acroForm.setSignaturesExist(true);
            acroForm.setAppendOnly(true);
            acroForm.getCOSObject().setDirect(true);
            acroFormFields.add(signatureField);

            widget.setRectangle(rect);

            // from PDVisualSigBuilder.createHolderForm()
            PDStream stream = new PDStream(doc);
            PDFormXObject form = new PDFormXObject(stream);
            PDResources res = new PDResources();
            form.setResources(res);
            form.setFormType(1);
            PDRectangle bbox = new PDRectangle(rect.getWidth(), rect.getHeight());
            
            float height = bbox.getHeight();
            Matrix initialScale = null;
            switch (srcDoc.getPage(pageNum).getRotation()) {
                case 90:
                    form.setMatrix(AffineTransform.getQuadrantRotateInstance(1));
                    initialScale = Matrix.getScaleInstance(bbox.getWidth() / bbox.getHeight(), bbox.getHeight() / bbox.getWidth());
                    height = bbox.getWidth();
                    break;
                case 180:
                    form.setMatrix(AffineTransform.getQuadrantRotateInstance(2));
                    break;
                case 270:
                    form.setMatrix(AffineTransform.getQuadrantRotateInstance(3));
                    initialScale = Matrix.getScaleInstance(bbox.getWidth() / bbox.getHeight(), bbox.getHeight() / bbox.getWidth());
                    height = bbox.getWidth();
                    break;
                case 0:
                default:
                    break;
            }
            form.setBBox(bbox);
            PDFont font = PDType1Font.HELVETICA_BOLD;

            // from PDVisualSigBuilder.createAppearanceDictionary()
            PDAppearanceDictionary appearance = new PDAppearanceDictionary();
            appearance.getCOSObject().setDirect(true);
            PDAppearanceStream appearanceStream = new PDAppearanceStream(form.getCOSObject());
            appearance.setNormalAppearance(appearanceStream);
            widget.setAppearance(appearance);

            try (PDPageContentStream cs = new PDPageContentStream(doc, appearanceStream)) {
                // for 90Â° and 270Â° scale ratio of width / height
                // not really sure about this
                // why does scale have no effect when done in the form matrix???
                if (initialScale != null) {
                    cs.transform(initialScale);
                }

                // show background (just for debugging, to see the rect size + position)
                cs.setNonStrokingColor(Color.LIGHT_GRAY);
                cs.addRect(-5000, -5000, 10000, 10000);
                cs.fill();

                // show background image
                // save and restore graphics if the image is too large and needs to be scaled
                cs.saveGraphicsState();
                //  cs.transform(Matrix.getScaleInstance(0.25f, 0.25f));
                PDImageXObject img = PDImageXObject.createFromFileByExtension(imageFile, doc);

                cs.drawImage(img, 0, 0, bbox.getWidth(), height);
                cs.restoreGraphicsState();

                // show text
                float fontSize = 6;
                float leading = fontSize * 1.5f;
                cs.beginText();
                cs.setFont(font, fontSize);
                cs.setNonStrokingColor(Color.black);
                cs.newLineAtOffset(fontSize, height - leading);
                cs.setLeading(leading);

                String name = signature.getName();

                // See https://stackoverflow.com/questions/12575990
                // for better date formatting
                SimpleDateFormat formatter = new SimpleDateFormat("dd-M-yyyy HH:mm:ss z");
                String date = formatter.format(signature.getSignDate().getTime());
                String reason = signature.getReason();

                cs.showText("Signer: " + name);
                cs.newLine();
                cs.showText("Date: " + date);
                cs.newLine();
                cs.showText("Reason: " + reason);

                cs.endText();
            }

            // no need to set annotations and /P entry
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return new ByteArrayInputStream(baos.toByteArray());
        }
    }

    public void setKeyStore(String ksFilePath, char[] ksPassword)
            throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        BouncyCastleProvider bcp = new BouncyCastleProvider();
        Security.addProvider(bcp);
        //Security.insertProviderAt(bcp, 1);

        ks = KeyStore.getInstance("PKCS12");
        ks.load(new FileInputStream(ksFilePath), ksPassword);
        String alias = (String) ks.aliases().nextElement();
        pk = (PrivateKey) ks.getKey(alias, ksPassword);
        chain = ks.getCertificateChain(alias);
    }

    /**
     * @return the imageFile
     */
    public File getImageFile() {
        return imageFile;
    }

    /**
     * @param imageFile the imageFile to set
     */
    public void setImageFile(File imageFile) {
        this.imageFile = imageFile;
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

}
