package com.shivam.aicodereviewer.service;

import com.shivam.aicodereviewer.dto.CodeReviewComment;
import com.shivam.aicodereviewer.dto.PullRequestDetails;
import com.shivam.aicodereviewer.dto.PullRequestFile;
import com.shivam.aicodereviewer.dto.ReviewResponse;
import com.shivam.aicodereviewer.exception.PRNotFoundException;
import com.shivam.aicodereviewer.exception.PRNotOpenException;
import com.shivam.aicodereviewer.exception.RepositoryNotFoundException;
import com.shivam.aicodereviewer.exception.UserNotFoundException;
import com.shivam.aicodereviewer.model.PullRequestFileEntity;
import com.shivam.aicodereviewer.model.Review;
import com.shivam.aicodereviewer.model.ReviewComment;
import com.shivam.aicodereviewer.repository.PullRequestFileRepository;
import com.shivam.aicodereviewer.repository.ReviewCommentRepository;
import com.shivam.aicodereviewer.repository.ReviewRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final GitHubService gitHubService;
    private final ClaudeService claudeService;
    private final ReviewRepository reviewRepository;
    private final ReviewCommentRepository commentRepository;
    private final PullRequestFileRepository pullRequestFileRepository;

    public ReviewResponse processReview(
        String owner,
        String repo,
        Integer prNumber,
        boolean forceRefresh) {

        String repoFullName = owner + "/" + repo;

        log.info("Starting review for {}/{} PR#{}",
            owner, repo, prNumber);

        if (!gitHubService.userExists(owner)) {
            throw new UserNotFoundException(owner);
        }

        if (!gitHubService.repoExists(owner, repo)) {
            throw new RepositoryNotFoundException(owner, repo);
        }

        if (!gitHubService.pullRequestExists(
                owner, repo, prNumber)) {
            throw new PRNotFoundException(
                owner, repo, prNumber);
        }

        String prState = gitHubService.getPullRequestState(
            owner, repo, prNumber);

        if (!"open".equals(prState)) {
            throw new PRNotOpenException(
                owner, repo, prNumber, prState);
        }

        if (forceRefresh) {
            log.info("Force refresh requested for PR#{}",
                prNumber);
        } else {
            Optional<Review> existingReview = reviewRepository
                .findTopByRepoNameAndPrNumberOrderByCreatedAtDesc(
                    repoFullName, prNumber);

            if (existingReview.isPresent()) {
                Review cached = existingReview.get();
                boolean hasComments = commentRepository
                    .existsByReviewId(cached.getId());

                if (hasComments) {
                    log.info(
                        "Cache hit for open PR#{} review {}",
                        prNumber, cached.getId());
                    return toResponse(cached, true);
                }

                log.info(
                    "Review exists but no comments found "
                    + "- reprocessing PR#{}", prNumber);
            }
        }

        log.info("Cache miss - processing open PR#{}",
            prNumber);

        PullRequestDetails prDetails = gitHubService
            .getPullRequestDetails(owner, repo, prNumber);

        Review review = Review.builder()
            .repoName(repoFullName)
            .prNumber(prNumber)
            .prTitle(prDetails.getTitle())
            .prUrl(prDetails.getHtmlUrl())
            .build();

        review = reviewRepository.save(review);

        List<PullRequestFile> files = gitHubService
            .getPullRequestFiles(owner, repo, prNumber);

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

        List<ReviewComment> allComments = new ArrayList<>();

        for (PullRequestFileEntity file : fileEntities) {
            if (file.getPatch() == null ||
                file.getPatch().isBlank()) {
                continue;
            }

            List<CodeReviewComment> claudeComments =
                claudeService.analyzeCode(
                    file.getPatch(),
                    file.getFilename());

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

        commentRepository.saveAll(allComments);

        if (!allComments.isEmpty()) {
            String markdown = formatAsMarkdown(
                allComments, prDetails.getTitle());
            gitHubService.postPullRequestComment(
                owner, repo, prNumber, markdown);
        } else {
            gitHubService.postPullRequestComment(
                owner, repo, prNumber,
                "AI Code Review - No issues found!");
        }

        log.info("Review complete: {}", review.getId());
        return toResponse(review, false);
    }

    private ReviewResponse toResponse(
        Review review, boolean cached) {

        return ReviewResponse.builder()
            .id(review.getId())
            .repoName(review.getRepoName())
            .prNumber(review.getPrNumber())
            .prTitle(review.getPrTitle())
            .prUrl(review.getPrUrl())
            .createdAt(review.getCreatedAt())
            .cached(cached)
            .build();
    }

    private String formatAsMarkdown(
        List<ReviewComment> comments, String prTitle) {

        StringBuilder sb = new StringBuilder();

        sb.append("## AI Code Review\n\n");
        sb.append("**PR:** ").append(prTitle).append("\n");
        sb.append("**Total Issues Found:** ")
            .append(comments.size()).append("\n\n");

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

        sb.append("**Critical:** ").append(critical)
            .append(" | ");
        sb.append("**Warnings:** ").append(warnings)
            .append(" | ");
        sb.append("**Suggestions:** ")
            .append(suggestions).append("\n\n");
        sb.append("---\n\n");

        if (critical > 0) {
            sb.append("### Critical Issues\n\n");
            comments.stream()
                .filter(c -> "critical"
                    .equals(c.getSeverity()))
                .forEach(c -> appendComment(sb, c));
        }

        if (warnings > 0) {
            sb.append("### Warnings\n\n");
            comments.stream()
                .filter(c -> "warning"
                    .equals(c.getSeverity()))
                .forEach(c -> appendComment(sb, c));
        }

        if (suggestions > 0) {
            sb.append("### Suggestions\n\n");
            comments.stream()
                .filter(c -> "suggestion"
                    .equals(c.getSeverity()))
                .forEach(c -> appendComment(sb, c));
        }

        sb.append("\n---\n");
        sb.append("*Generated by AI Code Reviewer*");

        return sb.toString();
    }

    private void appendComment(
        StringBuilder sb, ReviewComment c) {

        sb.append("**File:** `")
            .append(c.getFileName()).append("`");

        if (c.getLineNumber() != null) {
            sb.append(" - Line ").append(c.getLineNumber());
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
