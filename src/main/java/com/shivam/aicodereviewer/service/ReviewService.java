package com.shivam.aicodereviewer.service;

import com.shivam.aicodereviewer.dto.CodeReviewComment;
import com.shivam.aicodereviewer.dto.PullRequestDetails;
import com.shivam.aicodereviewer.dto.PullRequestFile;
import com.shivam.aicodereviewer.model.Review;
import com.shivam.aicodereviewer.model.ReviewComment;
import com.shivam.aicodereviewer.repository.ReviewCommentRepository;
import com.shivam.aicodereviewer.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import com.shivam.aicodereviewer.exception.PRNotFoundException;
import com.shivam.aicodereviewer.model.PullRequestFileEntity;
import com.shivam.aicodereviewer.repository.PullRequestFileRepository;
import com.shivam.aicodereviewer.exception.RepositoryNotFoundException;
import com.shivam.aicodereviewer.exception.UserNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final GitHubService gitHubService;
    private final ClaudeService claudeService;
    private final ReviewRepository reviewRepository;
    private final ReviewCommentRepository commentRepository;
    private final PullRequestFileRepository pullRequestFileRepository;
    // Main method that orchestrates the entire flow:
    // 1. Fetch PR details from GitHub
    // 2. Fetch changed files from GitHub
    // 3. Send each file to Claude for analysis
    // 4. Save results to database
    // 5. Post formatted comment back to GitHub PR
    public Review processReview(
        String owner, String repo, Integer prNumber) {

        log.info("Starting review for {}/{} PR#{}",
            owner, repo, prNumber);


        // Step 1: Check GitHub user exists
        if (!gitHubService.userExists(owner)) {
            throw new UserNotFoundException(owner);
        }

        // Step 2: Check repo exists
        if (!gitHubService.repoExists(owner, repo)) {
            throw new RepositoryNotFoundException(owner, repo);
        }

        // Step 3: Check PR exists FIRST
        // Throws PRNotFoundException if not found
        // GlobalExceptionHandler converts it to 404
        if (!gitHubService.pullRequestExists(
                owner, repo, prNumber)) {
            throw new PRNotFoundException(
                owner, repo, prNumber);
        }

        // Step 2: Fetch PR details
        PullRequestDetails prDetails = gitHubService
            .getPullRequestDetails(owner, repo, prNumber);

        // Step 3: Save Review to DB first
        // We need the review ID to link files
        // and comments to it
        Review review = Review.builder()
            .repoName(owner + "/" + repo)
            .prNumber(prNumber)
            .prTitle(prDetails.getTitle())
            .prUrl(prDetails.getHtmlUrl())
            .build();

        review = reviewRepository.save(review);
        log.info("Review saved: {}", review.getId());


        // Step 4: Fetch changed files from GitHub
        List<PullRequestFile> files = gitHubService
            .getPullRequestFiles(owner, repo, prNumber);

        log.info("Found {} changed files", files.size());

        // Step 5: Store files in DB
        // We save ALL files — even ones with no patch
        // so we have a complete record of what changed
        List<PullRequestFileEntity> fileEntities =
            new ArrayList<>();

        for (PullRequestFile file : files) {
            PullRequestFileEntity entity =
                PullRequestFileEntity.builder()
                    .review(review)
                    .filename(file.getFilename())
                    .status(file.getStatus())
                    .patch(file.getPatch())
                    .additions(file.getAdditions())
                    .deletions(file.getDeletions())
                    .build();

            fileEntities.add(entity);
        }

        pullRequestFileRepository.saveAll(fileEntities);
        log.info("Saved {} files to DB",
            fileEntities.size());

        // Step 6: Analyze files with Claude
        // Only files that have a patch (code changes)
        List<ReviewComment> allComments = new ArrayList<>();

        for (PullRequestFileEntity file : fileEntities) {

            // Skip files with no diff
            if (file.getPatch() == null ||
                file.getPatch().isBlank()) {
                log.info("Skipping (no patch): {}",
                    file.getFilename());
                continue;
            }

            log.info("Analyzing: {}", file.getFilename());

            // Send to Claude for review
            List<CodeReviewComment> claudeComments =
                claudeService.analyzeCode(
                    file.getPatch(),
                    file.getFilename());

            // Convert to DB entities
            for (CodeReviewComment cc : claudeComments) {
                ReviewComment comment = ReviewComment
                    .builder()
                    .review(review)
                    .severity(cc.getSeverity())
                    .category(cc.getCategory())
                    .lineNumber(cc.getLine())
                    .issue(cc.getIssue())
                    .fix(cc.getFix())
                    .fileName(file.getFilename())
                    .build();

                allComments.add(comment);
            }
        }

        // Step 7: Save all comments to DB
        commentRepository.saveAll(allComments);
        log.info("Saved {} comments", allComments.size());

        // Step 8: Post formatted comment to GitHub PR
        if (!allComments.isEmpty()) {
            String markdown = formatAsMarkdown(
                allComments, prDetails.getTitle());
            gitHubService.postPullRequestComment(
                owner, repo, prNumber, markdown);
        } else {
            gitHubService.postPullRequestComment(
                owner, repo, prNumber,
                "✅ **AI Code Review** — No issues found!");
        }

        log.info("Review complete: {}", review.getId());

        // Step 9: Return the saved review
        // Frontend uses the ID to fetch comments
        return review;
    }

    // Formats review comments into readable markdown
    // This is what appears as the PR comment on GitHub
    private String formatAsMarkdown(
        List<ReviewComment> comments, String prTitle) {

        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("## 🤖 AI Code Review\n\n");
        sb.append("**PR:** ").append(prTitle).append("\n");
        sb.append("**Total Issues Found:** ")
            .append(comments.size()).append("\n\n");

        // Count by severity for summary
        long critical = comments.stream()
            .filter(c -> "critical"
                .equals(c.getSeverity()))
            .count();
        long warnings = comments.stream()
            .filter(c -> "warning"
                .equals(c.getSeverity()))
            .count();
        long suggestions = comments.stream()
            .filter(c -> "suggestion"
                .equals(c.getSeverity()))
            .count();

        // Summary line
        sb.append("🔴 **Critical:** ").append(critical)
            .append(" | ");
        sb.append("🟡 **Warnings:** ").append(warnings)
            .append(" | ");
        sb.append("🔵 **Suggestions:** ")
            .append(suggestions).append("\n\n");
        sb.append("---\n\n");

        // Critical issues first
        if (critical > 0) {
            sb.append("### 🔴 Critical Issues\n\n");
            comments.stream()
                .filter(c -> "critical"
                    .equals(c.getSeverity()))
                .forEach(c -> appendComment(sb, c));
        }

        // Then warnings
        if (warnings > 0) {
            sb.append("### 🟡 Warnings\n\n");
            comments.stream()
                .filter(c -> "warning"
                    .equals(c.getSeverity()))
                .forEach(c -> appendComment(sb, c));
        }

        // Then suggestions
        if (suggestions > 0) {
            sb.append("### 🔵 Suggestions\n\n");
            comments.stream()
                .filter(c -> "suggestion"
                    .equals(c.getSeverity()))
                .forEach(c -> appendComment(sb, c));
        }

        sb.append("\n---\n");
        sb.append("*Generated by AI Code Reviewer*");

        return sb.toString();
    }

    // Formats a single comment into markdown block
    private void appendComment(
        StringBuilder sb, ReviewComment c) {

        sb.append("**File:** `")
            .append(c.getFileName()).append("`");

        if (c.getLineNumber() != null) {
            sb.append(" — Line ").append(c.getLineNumber());
        }

        sb.append("\n");
        sb.append("**Category:** ")
            .append(c.getCategory()).append("\n");
        sb.append("**Issue:** ")
            .append(c.getIssue()).append("\n");
        sb.append("**Fix:** ")
            .append(c.getFix()).append("\n\n");
    }
}