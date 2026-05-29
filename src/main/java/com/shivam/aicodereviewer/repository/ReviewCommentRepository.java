package com.shivam.aicodereviewer.repository;

import com.shivam.aicodereviewer.model.ReviewComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewCommentRepository
    extends JpaRepository<ReviewComment, UUID> {

    List<ReviewComment> findByReviewId(UUID reviewId);

    // Check if comments exist for a review
    // If no comments saved, review is incomplete
    boolean existsByReviewId(UUID reviewId);
}