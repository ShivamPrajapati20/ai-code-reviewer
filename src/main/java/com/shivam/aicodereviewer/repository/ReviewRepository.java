package com.shivam.aicodereviewer.repository;

import com.shivam.aicodereviewer.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Repository
public interface ReviewRepository
    extends JpaRepository<Review, UUID> {

    List<Review> findByRepoNameOrderByCreatedAtDesc(
        String repoName);

    // Find existing review by repo + PR number
    // Returns the most recent one if multiple exist
    Optional<Review> findTopByRepoNameAndPrNumberOrderByCreatedAtDesc(
        String repoName, Integer prNumber);
}