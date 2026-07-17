package com.desofs.project.document.services;

import com.desofs.project.document.config.DocumentUploadProperties;
import com.desofs.project.shared.exceptions.UnsafePdfException;
import com.desofs.project.shared.exceptions.UnsupportedDocumentTypeException;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDAction;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DocumentUploadValidationService {

    private static final String PDF_MIME_TYPE = "application/pdf";
    private static final Set<String> DANGEROUS_ACTION_TYPES = Set.of(
            "JavaScript",
            "Launch",
            "GoToE",
            "RichMediaExecute",
            "SubmitForm",
            "ImportData",
            "ResetForm"
    );

    private final DocumentUploadProperties uploadProperties;

    public ValidatedUpload validate(MultipartFile file) throws IOException {
        byte[] content = file.getBytes();
        String detectedContentType = detectContentType(file, content);

        if (!isAllowedContentType(detectedContentType)) {
            throw new UnsupportedDocumentTypeException("Only PDF files are allowed");
        }

        if (isPdfContentType(detectedContentType)) {
            validatePdfContent(content);
        }

        return new ValidatedUpload(content, detectedContentType);
    }

    private String detectContentType(MultipartFile file, byte[] content) {
        if (hasPdfHeader(content)) {
            return PDF_MIME_TYPE;
        }
        return "";
    }

    private boolean hasPdfHeader(byte[] content) {
        return content.length >= 5
                && content[0] == '%'
                && content[1] == 'P'
                && content[2] == 'D'
                && content[3] == 'F'
                && content[4] == '-';
    }

    private boolean isAllowedContentType(String detectedContentType) {
        return normalizedAllowedContentTypes().contains(detectedContentType);
    }

    private boolean isPdfContentType(String detectedContentType) {
        return PDF_MIME_TYPE.equalsIgnoreCase(detectedContentType) || "application/x-pdf".equalsIgnoreCase(detectedContentType);
    }

    private Set<String> normalizedAllowedContentTypes() {
        List<String> configured = uploadProperties.getAllowedContentTypes();
        Set<String> normalized = new HashSet<>();
        for (String contentType : configured) {
            if (contentType != null && !contentType.isBlank()) {
                normalized.add(contentType.trim().toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
    }

    private void validatePdfContent(byte[] content) throws IOException {
        try (PDDocument document = Loader.loadPDF(content)) {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            assertNoDangerousCatalogActions(catalog);
            assertNoDangerousNames(catalog.getCOSObject());
            for (PDPage page : document.getPages()) {
                assertNoDangerousPageActions(page);
            }
        } catch (IOException ex) {
            throw new UnsafePdfException("Uploaded PDF failed security inspection");
        }
    }

    /**
     * Checks the PDF document catalog for any dangerous actions that could be executed when the document is opened.
     * @param catalog
     * @throws IOException
     */
    private void assertNoDangerousCatalogActions(PDDocumentCatalog catalog) throws IOException {
        Object openAction = catalog.getOpenAction();
        if (openAction instanceof PDAction action && isDangerousAction(action.getCOSObject())) {
            throw new UnsafePdfException("Uploaded PDF failed security inspection");
        }
        if (openAction instanceof COSDictionary actionDictionary && isDangerousAction(actionDictionary)) {
            throw new UnsafePdfException("Uploaded PDF failed security inspection");
        }

        COSBase additionalActions = catalog.getCOSObject().getDictionaryObject(COSName.AA);
        if (additionalActions instanceof COSDictionary actions && containsDangerousAction(actions)) {
            throw new UnsafePdfException("Uploaded PDF failed security inspection");
        }
    }

    /** 
     * Checks the PDF document catalog for any dangerous names that could be used to execute JavaScript or launch external files.
     * @param catalogDictionary
     */
    private void assertNoDangerousNames(COSDictionary catalogDictionary) {
        COSBase namesBase = catalogDictionary.getDictionaryObject(COSName.NAMES);
        if (!(namesBase instanceof COSDictionary namesDictionary)) {
            return;
        }

        if (namesDictionary.containsKey(COSName.getPDFName("JavaScript"))
                || namesDictionary.containsKey(COSName.getPDFName("EmbeddedFiles"))) {
            throw new UnsafePdfException("Uploaded PDF failed security inspection");
        }
    }

    private void assertNoDangerousPageActions(PDPage page) throws IOException {
        COSDictionary pageDictionary = page.getCOSObject();
        if (pageDictionary.containsKey(COSName.AA)) {
            COSBase additionalActions = pageDictionary.getDictionaryObject(COSName.AA);
            if (additionalActions instanceof COSDictionary actions && containsDangerousAction(actions)) {
                throw new UnsafePdfException("Uploaded PDF failed security inspection");
            }
        }

        for (PDAnnotation annotation : page.getAnnotations()) {
            COSDictionary annotationDictionary = annotation.getCOSObject();
            COSBase actionBase = annotationDictionary.getDictionaryObject(COSName.A);
            if (actionBase instanceof COSDictionary actionDictionary && isDangerousAction(actionDictionary)) {
                throw new UnsafePdfException("Uploaded PDF failed security inspection");
            }

            COSBase annotationAdditionalActions = annotationDictionary.getDictionaryObject(COSName.AA);
            if (annotationAdditionalActions instanceof COSDictionary actions && containsDangerousAction(actions)) {
                throw new UnsafePdfException("Uploaded PDF failed security inspection");
            }
        }
    }

    private boolean containsDangerousAction(COSDictionary actionsDictionary) {
        for (COSName key : actionsDictionary.keySet()) {
            COSBase actionBase = actionsDictionary.getDictionaryObject(key);
            if (actionBase instanceof COSDictionary actionDictionary && isDangerousAction(actionDictionary)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDangerousAction(COSDictionary actionDictionary) {
        String actionType = actionDictionary.getNameAsString(COSName.S);
        return actionType != null && DANGEROUS_ACTION_TYPES.contains(actionType);
    }
}