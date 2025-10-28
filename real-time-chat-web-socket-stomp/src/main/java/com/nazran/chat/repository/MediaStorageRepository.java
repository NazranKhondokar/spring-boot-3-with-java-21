package com.nazran.chat.repository;

import com.nazran.chat.entity.MediaStorage;
import com.nazran.chat.enums.ReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link MediaStorage} entities.
 */
@Repository
public interface MediaStorageRepository extends JpaRepository<MediaStorage, Integer> {

    /**
     * Finds a {@link MediaStorage} entity by its reference ID and reference type.
     *
     * @param referenceId the ID of the reference
     * @param referenceType the type of the reference
     * @return an Optional containing the found MediaStorage, or empty if not found
     */
    Optional<MediaStorage> findByReferenceIdAndReferenceType(Integer referenceId, ReferenceType referenceType);

    /**
     * Finds all {@link MediaStorage} entities associated with a specific reference ID and reference type.
     *
     * @param referenceId the ID of the reference
     * @param referenceType the type of the reference
     * @return a list of MediaStorage entries associated with the given reference ID and type
     */
    List<MediaStorage> findAllByReferenceIdAndReferenceType(Integer referenceId, ReferenceType referenceType);

    /**
     * Finds a {@link MediaStorage} entity by its external ID.
     *
     * @param externalId the external ID (Firebase Storage file key)
     * @return an Optional containing the found MediaStorage, or empty if not found
     */
    Optional<MediaStorage> findByExternalId(String externalId);

    /**
     * Finds all {@link MediaStorage} entities by owner ID.
     *
     * @param ownerId the ID of the owner
     * @return a list of MediaStorage entries owned by the given user
     */
    List<MediaStorage> findAllByOwnerId(Integer ownerId);

    /**
     * Finds all {@link MediaStorage} entities by reference type.
     *
     * @param referenceType the type of the reference
     * @return a list of MediaStorage entries of the given reference type
     */
    List<MediaStorage> findAllByReferenceType(ReferenceType referenceType);

    /**
     * Deletes all {@link MediaStorage} entities by reference ID and reference type.
     *
     * @param referenceId the ID of the reference
     * @param referenceType the type of the reference
     */
    void deleteByReferenceIdAndReferenceType(Integer referenceId, ReferenceType referenceType);

    /**
     * Checks if a {@link MediaStorage} entity exists for a given reference ID and reference type.
     *
     * @param referenceId the ID of the reference
     * @param referenceType the type of the reference
     * @return true if exists, false otherwise
     */
    boolean existsByReferenceIdAndReferenceType(Integer referenceId, ReferenceType referenceType);
}