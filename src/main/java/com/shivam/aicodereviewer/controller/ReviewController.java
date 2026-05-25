package com.shivam.aicodereviewer.controller;

import com.shivam.aicodereviewer.dto.PullRequestDetails;
import com.shivam.aicodereviewer.dto.PullRequestFile;
import com.shivam.aicodereviewer.service.GitHubService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.shivam.aicodereviewer.service.ClaudeService;
import com.shivam.aicodereviewer.service.ReviewService;
import com.shivam.aicodereviewer.dto.CodeReviewComment;
import com.shivam.aicodereviewer.model.Review;
import com.shivam.aicodereviewer.model.ReviewComment;
import com.shivam.aicodereviewer.repository.ReviewRepository;
import com.shivam.aicodereviewer.repository.ReviewCommentRepository;
import org.springframework.http.ResponseEntity;
import java.util.UUID;

// @RequiredArgsConstructor automatically creates
// a constructor for all final fields
// Spring uses this constructor to inject dependencies
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final GitHubService gitHubService;
    private final ClaudeService claudeService;
    private final ReviewService reviewService;
    private final ReviewRepository reviewRepository;
    private final ReviewCommentRepository commentRepository;

    // Test endpoint to verify GitHub integration
    // Call it like: GET /api/review/test?owner=shivam&repo=my-project&prNumber=1
    @GetMapping("/test")
    public PullRequestDetails testGitHub(
        @RequestParam String owner,
        @RequestParam String repo,
        @RequestParam Integer prNumber) {

        return gitHubService
            .getPullRequestDetails(owner, repo, prNumber);
    }

    // Test endpoint to see changed files in a PR
    @GetMapping("/files")
    public List<PullRequestFile> testFiles(
        @RequestParam String owner,
        @RequestParam String repo,
        @RequestParam Integer prNumber) {

        return gitHubService
            .getPullRequestFiles(owner, repo, prNumber);
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzePR(
        @RequestParam String owner,
        @RequestParam String repo,
        @RequestParam Integer prNumber) {

        // ReviewService handles everything:
        // PR check → fetch files → save to DB
        // → Claude analysis → post to GitHub
        Review review = reviewService
            .processReview(owner, repo, prNumber);

        return ResponseEntity.ok(review);
    }

    // Fetch all reviews for a repo
    // Used by frontend history page
    @GetMapping("/reviews")
    public List<Review> getReviews(
        @RequestParam String repoName) {
        return reviewRepository.findByRepoNameOrderByCreatedAtDesc(repoName);
    }

    // Fetch all comments for a specific review
    @GetMapping("/reviews/{id}/comments")
    public List<ReviewComment> getComments(
        @PathVariable UUID id) {
        return commentRepository.findByReviewId(id);
    }
}