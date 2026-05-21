package com.shivam.aicodereviewer.controller;

import com.shivam.aicodereviewer.dto.PullRequestDetails;
import com.shivam.aicodereviewer.dto.PullRequestFile;
import com.shivam.aicodereviewer.service.GitHubService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.shivam.aicodereviewer.service.ClaudeService;
import com.shivam.aicodereviewer.dto.CodeReviewComment;

// @RequiredArgsConstructor automatically creates
// a constructor for all final fields
// Spring uses this constructor to inject dependencies
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final GitHubService gitHubService;
    private final ClaudeService claudeService;

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

    // Main endpoint to analyze a PR
    @GetMapping("/analyze")
    public List<CodeReviewComment> analyzePR(
        @RequestParam String owner,
        @RequestParam String repo,
        @RequestParam Integer prNumber) {

        // Step 1: Get all changed files from GitHub
        List<PullRequestFile> files = gitHubService
            .getPullRequestFiles(owner, repo, prNumber);

        // Step 2: For each file, send diff to Claude
        // flatMap flattens List<List<>> into List<>
        return files.stream()
            // Skip files with no diff
            // (deleted files, binary files etc.)
            .filter(file -> file.getPatch() != null)
            // Analyze each file with Claude
            .flatMap(file -> claudeService
                .analyzeCode(
                    file.getPatch(),
                    file.getFilename())
                .stream())
            .toList();
    }
}