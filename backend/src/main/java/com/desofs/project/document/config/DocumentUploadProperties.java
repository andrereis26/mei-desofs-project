package com.desofs.project.document.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "app.file-upload")
public class DocumentUploadProperties {

    @NotEmpty
    private List<String> allowedContentTypes = new ArrayList<>(List.of("application/pdf"));
}