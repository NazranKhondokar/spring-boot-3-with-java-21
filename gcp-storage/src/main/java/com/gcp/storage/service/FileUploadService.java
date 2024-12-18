package com.gcp.storage.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class FileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);

    private final Storage storage;
    private final String bucketName;

    public FileUploadService(@Value("${gcp.storage.bucket-name}") String bucketName) {
        this.storage = StorageOptions.getDefaultInstance().getService();
        this.bucketName = bucketName;
    }

    /**
     * Uploads a file to Google Cloud Storage.
     *
     * @param file the file to upload
     * @return the public URL of the uploaded file
     * @throws IOException if an error occurs during upload
     */
    public String uploadFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();

        logger.info("Starting file upload: {}", fileName);

        try {
            BlobId blobId = BlobId.of(bucketName, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.getContentType())
                    .build();

            // Upload file to GCP
            Blob blob = storage.create(blobInfo, file.getInputStream());

            String fileUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);
            logger.info("File uploaded successfully: {}", fileUrl);

            return fileUrl;
        } catch (StorageException e) {
            logger.error("Google Cloud Storage error while uploading file: {}", fileName, e);
            throw new IOException("Failed to upload file to GCP bucket", e);
        } catch (IOException e) {
            logger.error("I/O error while uploading file: {}", fileName, e);
            throw e;
        }
    }
}