package com.shivam.aicodereviewer.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

// Stores each changed file from a PR
// Linked to a Review
@Entity
@Table(name = "pull_request_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PullRequestFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Which review this file belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    // File path e.g. src/main/java/Service.java
    @Column(name = "filename", nullable = false)
    private String filename;

    // added / modified / removed
    @Column(name = "status")
    private String status;

    // The actual code diff
    // Text type handles large diffs
    @Column(name = "patch", columnDefinition = "TEXT")
    private String patch;

    @Column(name = "additions")
    private int additions;

    @Column(name = "deletions")
    private int deletions;
}