package com.shivam.aicodereviewer.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "repo_name", nullable = false)
    private String repoName;

    @Column(name = "pr_number", nullable = false)
    private Integer prNumber;

    @Column(name = "pr_title")
    private String prTitle;

    @Column(name = "pr_url")
    private String prUrl;

    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @OneToMany(mappedBy = "review",
               cascade = CascadeType.ALL,
               fetch = FetchType.LAZY)
    private List<ReviewComment> comments;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}