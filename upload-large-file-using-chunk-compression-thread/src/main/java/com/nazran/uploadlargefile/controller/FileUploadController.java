package com.nazran.uploadlargefile.controller;

import com.nazran.uploadlargefile.service.FileUploadService;
import com.nazran.uploadlargefile.service.ProgressService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/upload")
public class FileUploadController {

    @Value("${sftp.remote-dir}")
    private String sftpRemoteDir;
    private final FileUploadService fileUploadService;
    private final ProgressService progressService;

    public FileUploadController(FileUploadService fileUploadService, ProgressService progressService) {
        this.fileUploadService = fileUploadService;
        this.progressService = progressService;
    }

    @CrossOrigin
    @RequestMapping(path = "/sftp", method = RequestMethod.POST, consumes = {"multipart/form-data"})
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile multipartFile) {
        try {
            // Convert MultipartFile to a regular File
            File file = convertMultiPartToFile(multipartFile);

            // Set the remote directory on the SFTP server
            String remoteDir = sftpRemoteDir;

            // Call service to handle upload
            fileUploadService.uploadFile(file, remoteDir);

            // Delete temp file
            file.delete();
            return ResponseEntity.ok("File uploaded successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed: " + e.getMessage());
        }
    }

    @CrossOrigin
    @GetMapping("/progress")
    public ResponseEntity<Double> getProgress(@RequestParam("fileName") String fileName) {
        Double progress = progressService.getProgress(fileName);
        return ResponseEntity.ok(progress);
    }

    // Convert MultipartFile to File
    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename());
        file.transferTo(convFile);
        return convFile;
    }
}

