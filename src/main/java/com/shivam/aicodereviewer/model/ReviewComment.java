package com.shivam.aicodereviewer.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "review_comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    private String category;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(nullable = false, length = 1000)
    private String issue;

    @Column(nullable = false, length = 1000)
    private String fix;

    @Column(name = "file_name")
    private String fileName;
}