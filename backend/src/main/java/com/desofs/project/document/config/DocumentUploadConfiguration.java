package com.desofs.project.document.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DocumentUploadProperties.class)
public class DocumentUploadConfiguration {
}