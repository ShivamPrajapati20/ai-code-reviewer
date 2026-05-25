package com.shivam.aicodereviewer.repository;

import com.shivam.aicodereviewer.model.PullRequestFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PullRequestFileRepository
    extends JpaRepository<PullRequestFileEntity, UUID> {

    // Fetch all files for a specific review
    List<PullRequestFileEntity> findByReviewId(
        UUID reviewId);
}