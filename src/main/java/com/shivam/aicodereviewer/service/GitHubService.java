package com.shivam.aicodereviewer.service;

import com.shivam.aicodereviewer.dto.PullRequestDetails;
import com.shivam.aicodereviewer.dto.PullRequestFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;

// @Slf4j gives us a free "log" variable
// so we can do log.info(), log.error() etc.
@Slf4j
@Service
public class GitHubService {

    private final WebClient githubWebClient;

    // @Qualifier tells Spring which WebClient bean 
    // to inject — we have two (github and claude)
    // so we must specify which one we want here
    public GitHubService(
        @Qualifier("githubWebClient") 
        WebClient githubWebClient) {
        this.githubWebClient = githubWebClient;
    }

    // Fetches basic PR info like title, url, repo name
    // owner = github username e.g. "shivam"
    // repo  = repository name e.g. "my-project"
    // prNumber = PR number e.g. 42
    public PullRequestDetails getPullRequestDetails(
        String owner, String repo, Integer prNumber) {

        log.info("Fetching PR details for {}/{} PR#{}",
            owner, repo, prNumber);

        // Build the API URL and make GET request
        // GitHub endpoint: GET /repos/{owner}/{repo}/pulls/{pull_number}
        return githubWebClient
            .get()
            .uri("/repos/{owner}/{repo}/pulls/{prNumber}",
                owner, repo, prNumber)
            .retrieve()
            // If GitHub returns 4xx or 5xx error,
            // this converts it to an exception
            .onStatus(
                status -> status.isError(),
                response -> response.bodyToMono(String.class)
                    .map(body -> new RuntimeException(
                        "GitHub API error: " + body)))
            // Convert JSON response to our 
            // PullRequestDetails object
            .bodyToMono(PullRequestDetails.class)
            // block() waits for the response
            // (makes async call behave synchronously)
            .block();
    }

    // Fetches list of files changed in a PR
    // along with the actual code diff (patch)
    public List<PullRequestFile> getPullRequestFiles(
        String owner, String repo, Integer prNumber) {

        log.info("Fetching PR files for {}/{} PR#{}",
            owner, repo, prNumber);

        // GitHub endpoint: 
        // GET /repos/{owner}/{repo}/pulls/{pull_number}/files
        return githubWebClient
            .get()
            .uri("/repos/{owner}/{repo}/pulls/{prNumber}/files",
                owner, repo, prNumber)
            .retrieve()
            .onStatus(
                status -> status.isError(),
                response -> response.bodyToMono(String.class)
                    .map(body -> new RuntimeException(
                        "GitHub API error fetching files: " 
                        + body)))
            // This time response is a JSON array
            // so we use bodyToFlux (stream of items)
            // then collectList() to get List<>
            .bodyToFlux(PullRequestFile.class)
            .collectList()
            .block();
    }

    // Posts a review comment back to the PR on GitHub
    // This is what makes results visible on the PR page
    public void postPullRequestComment(
        String owner, String repo,
        Integer prNumber, String commentBody) {

        log.info("Posting review comment to {}/{} PR#{}",
            owner, repo, prNumber);

        // GitHub uses issue number (same as PR number)
        // for posting comments
        // Endpoint: POST /repos/{owner}/{repo}/issues/{issue_number}/comments
        githubWebClient
            .post()
            .uri("/repos/{owner}/{repo}/issues/{prNumber}/comments",
                owner, repo, prNumber)
            // Send the comment text as JSON body
            .bodyValue("{\"body\": \"" 
                + escapeJson(commentBody) + "\"}")
            .retrieve()
            .onStatus(
                status -> status.isError(),
                response -> response.bodyToMono(String.class)
                    .map(body -> new RuntimeException(
                        "GitHub API error posting comment: " 
                        + body)))
            .bodyToMono(String.class)
            .block();

        log.info("Comment posted successfully");
    }

    // Helper method to escape special characters
    // in the comment before sending as JSON
    // Without this, quotes or newlines in the 
    // comment would break the JSON format
    private String escapeJson(String text) {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}