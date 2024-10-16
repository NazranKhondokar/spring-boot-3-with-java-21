package com.nazran.uploadlargefile.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.util.unit.DataSize;

@Configuration
public class FileUploadConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();

        // Set the maximum file size and request size to 600MBfactory.setMaxFileSize(DataSize.ofMegabytes(600));  // 600MB
        factory.setMaxFileSize(DataSize.ofMegabytes(600));  // 600MB
        factory.setMaxRequestSize(DataSize.ofMegabytes(600));  // 600MB

        return factory.createMultipartConfig();
    }
}

