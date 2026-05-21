package com.shivam.aicodereviewer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PullRequestDetails {

    // PR number e.g. #42
    @JsonProperty("number")
    private Integer number;

    // PR title e.g. "Fix login bug"
    @JsonProperty("title")
    private String title;

    // URL to view the PR on GitHub
    @JsonProperty("html_url")
    private String htmlUrl;

    // PR body/description written by the author
    @JsonProperty("body")
    private String body;

    // Nested object — info about the repo
    @JsonProperty("head")
    private HeadDetails head;

    // Inner class to hold repo details
    // GitHub returns head as a nested JSON object
    @Data
    public static class HeadDetails {
        @JsonProperty("repo")
        private RepoDetails repo;
    }

    @Data
    public static class RepoDetails {
        // Full repo name e.g. "shivam/my-project"
        @JsonProperty("full_name")
        private String fullName;
    }
}