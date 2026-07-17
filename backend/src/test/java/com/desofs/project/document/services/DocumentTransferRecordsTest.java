package com.desofs.project.document.services;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTransferRecordsTest {

    @Test
    void documentDownload_ExposesConstructorValues() {
        byte[] content = new byte[] {1, 2, 3};

        DocumentDownload download = new DocumentDownload(content, "application/pdf", "report.pdf");

        assertThat(download.content()).containsExactly(1, 2, 3);
        assertThat(download.contentType()).isEqualTo("application/pdf");
        assertThat(download.filename()).isEqualTo("report.pdf");
    }

    @Test
    void documentDownload_UsesArrayContentForEqualsHashCodeAndToString() {
        DocumentDownload first = new DocumentDownload(new byte[] {1, 2, 3}, "application/pdf", "report.pdf");
        DocumentDownload second = new DocumentDownload(new byte[] {1, 2, 3}, "application/pdf", "report.pdf");

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second)
                .hasToString("DocumentDownload[content=[1, 2, 3], contentType=application/pdf, filename=report.pdf]");
    }

    @Test
    void documentDownload_IsEqualToItself() {
        DocumentDownload download = new DocumentDownload(new byte[] {1}, "application/pdf", "report.pdf");

        assertThat(download).isEqualTo(download);
    }

    @Test
    void documentDownload_IsNotEqualToNullOrDifferentType() {
        DocumentDownload download = new DocumentDownload(new byte[] {1}, "application/pdf", "report.pdf");

        assertThat(download).isNotEqualTo(null);
        assertThat(download).isNotEqualTo("report.pdf");
    }

    @Test
    void documentDownload_IsNotEqualWhenContentDiffers() {
        DocumentDownload download = new DocumentDownload(new byte[] {1, 2, 3}, "application/pdf", "report.pdf");

        assertThat(download).isNotEqualTo(new DocumentDownload(new byte[] {1, 2, 4}, "application/pdf", "report.pdf"));
    }

    @Test
    void documentDownload_IsNotEqualWhenContentTypeDiffers() {
        DocumentDownload download = new DocumentDownload(new byte[] {1, 2, 3}, "application/pdf", "report.pdf");

        assertThat(download).isNotEqualTo(new DocumentDownload(new byte[] {1, 2, 3}, "text/plain", "report.pdf"));
    }

    @Test
    void documentDownload_IsNotEqualWhenFilenameDiffers() {
        DocumentDownload download = new DocumentDownload(new byte[] {1, 2, 3}, "application/pdf", "report.pdf");

        assertThat(download).isNotEqualTo(new DocumentDownload(new byte[] {1, 2, 3}, "application/pdf", "other.pdf"));
    }

    @Test
    void documentDownload_SupportsNullableComponents() {
        DocumentDownload first = new DocumentDownload(null, null, null);
        DocumentDownload second = new DocumentDownload(null, null, null);

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second)
                .hasToString("DocumentDownload[content=null, contentType=null, filename=null]");
        assertThat(first.content()).isNull();
        assertThat(first.contentType()).isNull();
        assertThat(first.filename()).isNull();
    }

    @Test
    void documentDownload_DistinguishesNullComponentsFromValues() {
        DocumentDownload download = new DocumentDownload(null, null, null);

        assertThat(download).isNotEqualTo(new DocumentDownload(new byte[0], null, null));
        assertThat(download).isNotEqualTo(new DocumentDownload(null, "application/pdf", null));
        assertThat(download).isNotEqualTo(new DocumentDownload(null, null, "report.pdf"));
    }

    @Test
    void validatedUpload_ExposesConstructorValues() {
        ValidatedUpload upload = new ValidatedUpload(new byte[] {4, 5, 6}, "application/pdf");

        assertThat(upload.content()).containsExactly(4, 5, 6);
        assertThat(upload.detectedContentType()).isEqualTo("application/pdf");
    }

    @Test
    void validatedUpload_UsesArrayContentForEqualsHashCodeAndToString() {
        ValidatedUpload first = new ValidatedUpload(new byte[] {4, 5, 6}, "application/pdf");
        ValidatedUpload second = new ValidatedUpload(new byte[] {4, 5, 6}, "application/pdf");

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second)
                .hasToString("ValidatedUpload[content=[4, 5, 6], detectedContentType=application/pdf]");
    }

    @Test
    void validatedUpload_IsEqualToItself() {
        ValidatedUpload upload = new ValidatedUpload(new byte[] {4}, "application/pdf");

        assertThat(upload).isEqualTo(upload);
    }

    @Test
    void validatedUpload_IsNotEqualToNullOrDifferentType() {
        ValidatedUpload upload = new ValidatedUpload(new byte[] {4}, "application/pdf");

        assertThat(upload).isNotEqualTo(null);
        assertThat(upload).isNotEqualTo("application/pdf");
    }

    @Test
    void validatedUpload_IsNotEqualWhenContentDiffers() {
        ValidatedUpload upload = new ValidatedUpload(new byte[] {4, 5, 6}, "application/pdf");

        assertThat(upload).isNotEqualTo(new ValidatedUpload(new byte[] {4, 5, 7}, "application/pdf"));
    }

    @Test
    void validatedUpload_IsNotEqualWhenDetectedContentTypeDiffers() {
        ValidatedUpload upload = new ValidatedUpload(new byte[] {4, 5, 6}, "application/pdf");

        assertThat(upload).isNotEqualTo(new ValidatedUpload(new byte[] {4, 5, 6}, "text/plain"));
    }

    @Test
    void validatedUpload_SupportsNullableComponents() {
        ValidatedUpload first = new ValidatedUpload(null, null);
        ValidatedUpload second = new ValidatedUpload(null, null);

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second)
                .hasToString("ValidatedUpload[content=null, detectedContentType=null]");
        assertThat(first.content()).isNull();
        assertThat(first.detectedContentType()).isNull();
    }

    @Test
    void validatedUpload_DistinguishesNullComponentsFromValues() {
        ValidatedUpload upload = new ValidatedUpload(null, null);

        assertThat(upload).isNotEqualTo(new ValidatedUpload(new byte[0], null));
        assertThat(upload).isNotEqualTo(new ValidatedUpload(null, "application/pdf"));
    }
}
