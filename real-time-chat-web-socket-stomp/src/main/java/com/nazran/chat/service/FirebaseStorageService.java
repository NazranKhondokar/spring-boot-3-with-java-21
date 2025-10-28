package com.nazran.chat.service;

import com.nazran.chat.dto.response.MediaStorageResponse;
import com.nazran.chat.enums.ReferenceType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Service interface for handling file storage operations using Firebase Storage.
 */
public interface FirebaseStorageService {

    /**
     * Uploads a file to Firebase Storage and returns its accessible URL.
     *
     * @param referenceType The type of entity the file is associated with (e.g., "UsersAvatars", "ArticlesCoverImages").
     * @param referenceId   The unique identifier of the entity instance the file belongs to.
     * @throws IOException If an error occurs during file upload.
     */
    void uploadFile(MultipartFile file, Integer ownerId, ReferenceType referenceType, Integer referenceId) throws IOException;

    /**
     * Deletes all media files associated with a given reference ID and reference type.
     *
     * @param referenceId   The ID of the entity the files belong to.
     * @param referenceType The type of entity the files are associated with.
     */
    void deleteFilesByReference(Integer referenceId, ReferenceType referenceType);

    /**
     * Retrieve file information from local database.
     *
     * @param referenceType The type of entity the file is associated with (e.g., "UsersAvatars", "ArticlesCoverImages").
     * @param referenceId   The unique identifier of the entity instance the file belongs to.
     * @return MediaStorageResponse containing file information, or an error response if file retrieval fails.
     */
    MediaStorageResponse getMediaStorage(Integer referenceId, ReferenceType referenceType);

    void deleteFile(String externalId);
}
