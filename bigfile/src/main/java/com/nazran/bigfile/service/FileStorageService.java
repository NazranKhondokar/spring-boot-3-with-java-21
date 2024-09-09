package com.nazran.bigfile.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.io.ByteArrayResource;
import java.io.IOException;

@Service
public class FileStorageService {

    private static final String EXTERNAL_SERVER_UPLOAD_URL = "http://externalserver.com/upload";

    public void storeFile(MultipartFile file) {
        Thread.startVirtualThread(() -> { // Use virtual thread for asynchronous processing
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                ByteArrayResource fileAsResource = new ByteArrayResource(file.getBytes()) {
                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename();
                    }
                };

                HttpEntity<ByteArrayResource> requestEntity = new HttpEntity<>(fileAsResource, headers);
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.postForEntity(EXTERNAL_SERVER_UPLOAD_URL, requestEntity, String.class);

                System.out.println("File uploaded successfully");

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}