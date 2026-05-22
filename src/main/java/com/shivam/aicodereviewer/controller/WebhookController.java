package com.shivam.aicodereviewer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivam.aicodereviewer.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final ReviewService reviewService;
    private final ObjectMapper objectMapper
        = new ObjectMapper();

    // Webhook secret from .env
    // Used to verify request is really from GitHub
    @Value("${app.github.webhook-secret}")
    private String webhookSecret;

    // GitHub calls this endpoint when PR events happen
    @PostMapping("/github")
    public ResponseEntity<String> handleWebhook(

        // GitHub sends event type in this header
        // e.g. "pull_request", "push", "ping"
        @RequestHeader("X-GitHub-Event")
        String eventType,

        // GitHub sends a signature to prove
        // the request is genuine — not from attackers
        @RequestHeader("X-Hub-Signature-256")
        String signature,

        // Raw request body as string
        // We need raw string for signature verification
        @RequestBody String payload) {

        log.info("Received GitHub webhook: {}",
            eventType);

        // Step 1: Verify signature
        // If verification fails, reject the request
        // This prevents attackers from triggering
        // fake reviews on your server
        if (!verifySignature(payload, signature)) {
            log.warn("Invalid webhook signature");
            return ResponseEntity
                .status(401)
                .body("Invalid signature");
        }

        // Step 2: Only process pull_request events
        // GitHub sends many event types — we only
        // care about PR events
        if (!"pull_request".equals(eventType)) {
            log.info("Ignoring event type: {}",
                eventType);
            return ResponseEntity.ok("Event ignored");
        }

        try {
            // Step 3: Parse the payload JSON
            JsonNode root = objectMapper
                .readTree(payload);

            // Get the action — what happened to the PR
            // opened = new PR created
            // synchronize = new commits pushed to PR
            // reopened = closed PR was reopened
            String action = root
                .path("action")
                .asText();

            log.info("PR action: {}", action);

            // Step 4: Only review on these actions
            // We don't want to review on every
            // event — only when code actually changes
            if (!action.equals("opened") &&
                !action.equals("synchronize") &&
                !action.equals("reopened")) {
                return ResponseEntity
                    .ok("Action ignored: " + action);
            }

            // Step 5: Extract PR details from payload
            Integer prNumber = root
                .path("pull_request")
                .path("number")
                .asInt();

            String repoFullName = root
                .path("repository")
                .path("full_name")
                .asText();

            // full_name is "owner/repo" e.g. "shivam/my-project"
            // Split it to get owner and repo separately
            String[] repoParts = repoFullName.split("/");
            String owner = repoParts[0];
            String repo = repoParts[1];

            log.info("Processing PR#{} for {}/{}",
                prNumber, owner, repo);

            // Step 6: Trigger the review
            // Run in a separate thread so we can
            // return 200 to GitHub immediately
            // GitHub expects response within 10 seconds
            // or it marks delivery as failed
            new Thread(() -> {
                try {
                    reviewService.processReview(
                        owner, repo, prNumber);
                } catch (Exception e) {
                    log.error(
                        "Error processing review: {}",
                        e.getMessage());
                }
            }).start();

            // Return 200 immediately to GitHub
            return ResponseEntity.ok("Review started");

        } catch (Exception e) {
            log.error("Error handling webhook: {}",
                e.getMessage());
            return ResponseEntity
                .status(500)
                .body("Internal error");
        }
    }

    // Verifies GitHub's signature to confirm
    // the request is genuinely from GitHub
    // GitHub signs the payload using your webhook
    // secret with HMAC-SHA256 algorithm
    private boolean verifySignature(
        String payload, String signature) {

        try {
            // Create HMAC-SHA256 with your secret
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                webhookSecret.getBytes(),
                "HmacSHA256");
            mac.init(secretKey);

            // Compute signature of the payload
            byte[] hash = mac.doFinal(
                payload.getBytes());

            // Convert to hex string
            String computed = "sha256=" +
                HexFormat.of().formatHex(hash);

            // Compare with GitHub's signature
            // Using equals is fine here —
            // timing attacks not a concern for webhooks
            return computed.equals(signature);

        } catch (Exception e) {
            log.error(
                "Signature verification failed: {}",
                e.getMessage());
            return false;
        }
    }
}