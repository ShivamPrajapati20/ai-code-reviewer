package com.shivam.aicodereviewer.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

// Clean response object — no circular refs
// no comments — frontend fetches those separately
@Data
@Builder
public class ReviewResponse {

    private UUID id;
    private String repoName;
    private Integer prNumber;
    private String prTitle;
    private String prUrl;
    private LocalDateTime createdAt;

    // Tells frontend if this came from cache
    // or was freshly processed
    private boolean cached;
}