package com.nazran.bigfile.service;

import org.springframework.beans.factory.annotation.Value;
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

    @Value("${ftpFileUploadPath}")
    private String ftpFileUploadPath;

    public void storeFile(MultipartFile file) {
        // First, read the file data into memory (byte array)
        try {
            byte[] fileBytes = file.getBytes();  // This ensures the file data is read before the thread is started

            // Process the file asynchronously using virtual threads
            Thread.startVirtualThread(() -> {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                    ByteArrayResource fileAsResource = new ByteArrayResource(fileBytes) {
                        @Override
                        public String getFilename() {
                            return file.getOriginalFilename();
                        }
                    };

                    HttpEntity<ByteArrayResource> requestEntity = new HttpEntity<>(fileAsResource, headers);
                    RestTemplate restTemplate = new RestTemplate();
                    restTemplate.postForEntity(ftpFileUploadPath, requestEntity, String.class);

                    System.out.println("File uploaded successfully");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}