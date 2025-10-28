package com.nazran.chat.service.impl;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.nazran.chat.dto.response.MediaStorageResponse;
import com.nazran.chat.entity.MediaStorage;
import com.nazran.chat.enums.ReferenceType;
import com.nazran.chat.exception.CustomMessagePresentException;
import com.nazran.chat.repository.MediaStorageRepository;
import com.nazran.chat.service.FirebaseStorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of FirebaseStorageService for handling file uploads to Firebase Storage.
 */
@Service
@RequiredArgsConstructor
public class FirebaseStorageServiceImpl implements FirebaseStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseStorageServiceImpl.class);

    private final MediaStorageRepository mediaStorageRepository;
    private final Storage storage;
    @Value("${BUCKET_NAME}")
    private String bucketName;
    @Value("${FIREBASE_URL}")
    private String firebaseUrl;

    /**
     * {@inheritDoc}
     */
    @Override
    public void uploadFile(MultipartFile file, Integer ownerId, ReferenceType referenceType, Integer referenceId) throws IOException {

        // Validate input parameters
        Objects.requireNonNull(file, "File must not be null.");
        Objects.requireNonNull(referenceType, "Reference type must not be null.");
        Objects.requireNonNull(referenceId, "Reference ID must not be null.");

        if (file.isEmpty()) {
            throw new CustomMessagePresentException("File cannot be empty.");
        }
        // Prepare file metadata
        String fileExtension = getFileExtension(file.getOriginalFilename()); //The file extension (e.g., "jpg", "png", "pdf").
        String mimeType = file.getContentType(); //The MIME type of the file (e.g., "image/png", "application/pdf").


        // Generate new file key
        String fileKey = String.format("%s/%d/%s.%s", referenceType, referenceId, UUID.randomUUID(), fileExtension);
        BlobId blobId = BlobId.of(bucketName, fileKey);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(mimeType).build();

        // Find existing media record
        Optional<MediaStorage> existingMediaOpt = mediaStorageRepository.findByReferenceIdAndReferenceType(referenceId, referenceType);

        existingMediaOpt.ifPresent(existingMedia -> {
            logger.info("Existing file found: {} - Deleting before upload...", existingMedia.getExternalId());
            deleteFile(existingMedia.getExternalId()); // Delete the old file
        });

        try (InputStream stream = file.getInputStream()) { // Ensures InputStream is properly closed
            storage.create(blobInfo, stream.readAllBytes());

            // Construct the URL
            String encodedFileKey = URLEncoder.encode(fileKey, StandardCharsets.UTF_8);
            String fileUrl = String.format("%s%s/o/%s?alt=media", firebaseUrl, bucketName, encodedFileKey);

            logger.info("File uploaded successfully: {}", fileUrl);

            MediaStorage mediaStorage;
            if (existingMediaOpt.isPresent()) {
                // Update existing record
                mediaStorage = existingMediaOpt.get();
                mediaStorage.setExternalId(fileKey);
                mediaStorage.setUrl(fileUrl);
                mediaStorage.setMimeType(mimeType);
                mediaStorage.setUpdatedAt(OffsetDateTime.now());
            } else {
                // Create a new record
                mediaStorage = new MediaStorage();
                mediaStorage.setReferenceType(referenceType);
                mediaStorage.setOwnerId(ownerId);
                mediaStorage.setReferenceId(referenceId);
                mediaStorage.setExternalId(fileKey);
                mediaStorage.setUrl(fileUrl);
                mediaStorage.setMimeType(mimeType);
            }

            mediaStorageRepository.save(mediaStorage);
        } catch (StorageException | IOException e) {
            logger.error(" Failed to upload file to Firebase Storage: {}", e.getMessage(), e);
            throw new IOException("Error uploading file to Firebase Storage", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteFilesByReference(Integer referenceId, ReferenceType referenceType) {
        List<MediaStorage> mediaFiles = mediaStorageRepository.findAllByReferenceIdAndReferenceType(referenceId, referenceType);

        if (!mediaFiles.isEmpty()) {
            for (MediaStorage media : mediaFiles) {
                deleteFile(media.getExternalId()); // Delete file from Firebase Storage
            }
            mediaStorageRepository.deleteAll(mediaFiles); // Remove records from DB
            logger.info("Deleted all media files for ReferenceId: {}", referenceId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MediaStorageResponse getMediaStorage(Integer referenceId, ReferenceType referenceType) {
        return mediaStorageRepository.findByReferenceIdAndReferenceType(referenceId, referenceType)
                .map(mediaStorage -> MediaStorageResponse.builder()
                        .ownerId(mediaStorage.getOwnerId())
                        .referenceType(mediaStorage.getReferenceType())
                        .referenceId(mediaStorage.getReferenceId())
                        .externalId(mediaStorage.getExternalId())
                        .url(mediaStorage.getUrl())
                        .mimeType(mediaStorage.getMimeType())
                        .build()
                )
                .orElse(null);
    }

    /**
     * Deletes a file from Firebase Storage using its external ID.
     *
     * @param externalId The unique identifier of the file in Firebase Storage.
     * @throws RuntimeException if the file deletion fails.
     */
    @Override
    public void deleteFile(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            logger.warn("Attempted to delete a file with a null or empty external ID.");
            return;
        }

        try {
            BlobId blobId = BlobId.of(bucketName, externalId);
            boolean deleted = storage.delete(blobId);

            if (deleted) {
                logger.info("Successfully deleted file from Firebase Storage: {}", externalId);
            } else {
                logger.warn("File not found or already deleted: {}", externalId);
            }
        } catch (StorageException e) {
            logger.error("Error deleting file from Firebase Storage: {} - {}", externalId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete file: " + externalId, e);
        }
    }

    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.') + 1); // Extract the file extension
    }

    /**
     * {@inheritDoc}
     */
    public String handleFileUpload(Integer userId, MultipartFile file, ReferenceType type)
            throws IOException {
        uploadFile(file, userId, type, userId);
        MediaStorage media = mediaStorageRepository.findByReferenceIdAndReferenceType(userId, type)
                .orElseThrow(() -> new CustomMessagePresentException("File upload failed"));
        logger.info("File uploaded successfully for user/organization ID: {}", userId);
        return media.getUrl();
    }
}