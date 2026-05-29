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
import com.shivam.aicodereviewer.dto.ReviewResponse;
import com.shivam.aicodereviewer.dto.CommentResponse;

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

    // forceRefresh=true bypasses cache
    // and runs a fresh review
    @PostMapping("/analyze")
    public ResponseEntity<ReviewResponse> analyzePR(
        @RequestParam String owner,
        @RequestParam String repo,
        @RequestParam Integer prNumber,
        @RequestParam(defaultValue = "false")
            boolean forceRefresh) {

        ReviewResponse response = reviewService
            .processReview(
                owner, repo, prNumber, forceRefresh);

        return ResponseEntity.ok(response);
    }

    // Fetch all reviews for a repo
    // Used by frontend history page
    @GetMapping("/reviews")
    public List<Review> getReviews(
        @RequestParam String repoName) {
        return reviewRepository.findByRepoNameOrderByCreatedAtDesc(repoName);
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> getComments(
        @PathVariable UUID id) {

        return commentRepository
            .findByReviewId(id)
            .stream()
            .map(c -> CommentResponse.builder()
                .id(c.getId())
                .severity(c.getSeverity())
                .category(c.getCategory())
                .lineNumber(c.getLineNumber())
                .issue(c.getIssue())
                .fix(c.getFix())
                .fileName(c.getFileName())
                .build())
            .toList();
    }
}