package com.shivam.aicodereviewer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivam.aicodereviewer.dto.CodeReviewComment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ClaudeService {

    private final WebClient claudeWebClient;
    private final ObjectMapper objectMapper;

    // Read model name and max tokens 
    // from application.yml
    @Value("${app.claude.model}")
    private String claudeModel;

    @Value("${app.claude.max-tokens}")
    private int maxTokens;

    public ClaudeService(
        @Qualifier("claudeWebClient")
        WebClient claudeWebClient) {
        this.claudeWebClient = claudeWebClient;
        // ObjectMapper is Jackson's main class
        // for converting between Java objects and JSON
        this.objectMapper = new ObjectMapper();
    }

    // Main method — takes a code diff and filename,
    // sends to Claude, returns list of review comments
    public List<CodeReviewComment> analyzeCode(
        String codeDiff, String filename) {

        log.info("Analyzing file: {}", filename);
        log.info("testing the webhook");

        // If there's no diff (e.g. binary file),
        // skip it — nothing to review
        if (codeDiff == null || codeDiff.isBlank()) {
            log.warn("Empty diff for file: {}", filename);
            return Collections.emptyList();
        }

        // Detect language from file extension
        // so Claude gives language-specific advice
        String language = detectLanguage(filename);

        // Build the prompt we'll send to Claude
        String prompt = buildPrompt(codeDiff, language, filename);

        try {
            // Build the request body as a Map
            // Claude API expects this JSON structure:
            // {
            //   "model": "...",
            //   "max_tokens": 2000,
            //   "messages": [{"role": "user", "content": "..."}]
            // }
            Map<String, Object> requestBody = Map.of(
                "model", claudeModel,
                "max_tokens", maxTokens,
                "messages", List.of(
                    Map.of(
                        "role", "user",
                        "content", prompt
                    )
                )
            );

            // Convert request Map to JSON string
            String requestJson = objectMapper
                .writeValueAsString(requestBody);

            // Send POST request to Claude API
            String responseJson = claudeWebClient
                .post()
                .uri("/v1/messages")
                .bodyValue(requestJson)
                .retrieve()
                .onStatus(
                    status -> status.isError(),
                    response -> response
                        .bodyToMono(String.class)
                        .map(body -> new RuntimeException(
                            "Claude API error: " + body)))
                .bodyToMono(String.class)
                .block();

            // Parse and return the review comments
            return parseClaudeResponse(responseJson);

        } catch (Exception e) {
            log.error("Error calling Claude API: {}",
                e.getMessage());
            return Collections.emptyList();
        }
    }

    // Builds the prompt sent to Claude
    // Good prompt = good review output
    // We tell Claude exactly what format to return
    private String buildPrompt(
        String codeDiff,
        String language,
        String filename) {

        return """
            You are a senior software engineer 
            doing a code review.
                        
            Analyze the following code diff from 
            file: %s (Language: %s)
                        
            Return ONLY a JSON array. 
            No explanation. No markdown. 
            No text before or after the array.
                        
            Each item in the array must have 
            exactly these fields:
            - "severity": "critical" | "warning" 
              | "suggestion"
            - "category": "security" | "performance" 
              | "bug" | "best-practice"
            - "line": line number as integer 
              or null if general
            - "issue": short description of the problem
            - "fix": concrete suggestion to fix it
                        
            Focus on:
            - Security vulnerabilities
            - Performance problems  
            - Bugs and logic errors
            - Best practice violations
                        
            If the code looks good, return 
            an empty array: []
                        
            Code diff:
            %s
            """.formatted(filename, language, codeDiff);
    }

    // Parses Claude's response JSON
    // Claude wraps its reply in a content array
    // We need to dig into it to get our review JSON
    private List<CodeReviewComment> parseClaudeResponse(
        String responseJson) {

        try {
            // Claude response structure:
            // {
            //   "content": [
            //     {
            //       "type": "text",
            //       "text": "[{...our JSON array...}]"
            //     }
            //   ]
            // }

            // Step 1: Parse the full response
            JsonNode root = objectMapper
                .readTree(responseJson);

            // Step 2: Navigate to the text content
            String textContent = root
                .path("content")  // get content array
                .path(0)          // get first item
                .path("text")     // get text field
                .asText();

            log.info("Claude raw response: {}",
                textContent);

            // Step 3: Clean up the response
            // Sometimes Claude wraps JSON in 
            // markdown backticks despite instructions
            // We strip those just in case
            String cleanJson = textContent
                .trim()
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();

            // Step 4: Parse the JSON array into
            // a List of CodeReviewComment objects
            return objectMapper.readValue(
                cleanJson,
                new TypeReference<List<CodeReviewComment>>() {}
            );

        } catch (Exception e) {
            log.error(
                "Error parsing Claude response: {}",
                e.getMessage());
            return Collections.emptyList();
        }
    }

    // Detects programming language from file extension
    // This helps Claude give relevant advice
    // e.g. Java-specific patterns vs Python patterns
    private String detectLanguage(String filename) {
        if (filename == null) return "Unknown";

        // Get the file extension
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex == -1) return "Unknown";

        String ext = filename
            .substring(dotIndex + 1)
            .toLowerCase();

        // Map extension to language name
        return switch (ext) {
            case "java"       -> "Java";
            case "py"         -> "Python";
            case "js"         -> "JavaScript";
            case "ts"         -> "TypeScript";
            case "jsx", "tsx" -> "React";
            case "sql"        -> "SQL";
            case "yml", "yaml"-> "YAML";
            case "json"       -> "JSON";
            case "xml"        -> "XML";
            case "html"       -> "HTML";
            case "css"        -> "CSS";
            case "sh"         -> "Shell";
            default           -> "Unknown";
        };
    }
}
