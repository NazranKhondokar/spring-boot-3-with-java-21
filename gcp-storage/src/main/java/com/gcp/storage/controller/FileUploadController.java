package com.gcp.storage.controller;

import com.gcp.storage.service.FileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    private final FileUploadService fileUploadService;

    @Autowired
    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    /**
     * Endpoint to upload a file to GCP.
     *
     * @param file the file to upload
     * @return the URL of the uploaded file
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileName = file.getOriginalFilename();
        logger.info("Received request to upload file: {}", fileName);

        try {
            String fileUrl = fileUploadService.uploadFile(file);
            logger.info("File uploaded successfully: {}", fileName);
            return ResponseEntity.ok(fileUrl);
        } catch (IOException e) {
            logger.error("File upload failed for file: {}", fileName, e);
            return ResponseEntity.internalServerError().body("File upload failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during file upload: {}", fileName, e);
            return ResponseEntity.internalServerError().body("An unexpected error occurred: " + e.getMessage());
        }
    }
}