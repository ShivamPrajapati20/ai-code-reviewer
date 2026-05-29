package com.shivam.aicodereviewer.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class CommentResponse {
    private UUID id;
    private String severity;
    private String category;
    private Integer lineNumber;
    private String issue;
    private String fix;
    private String fileName;
}