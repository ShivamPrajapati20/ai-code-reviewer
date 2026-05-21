package com.shivam.aicodereviewer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

// This represents ONE issue found by Claude
// in the code review
// Claude will return a list of these
@Data
public class CodeReviewComment {

    // How serious is this issue?
    // Values: "critical", "warning", "suggestion"
    @JsonProperty("severity")
    private String severity;

    // What type of problem is it?
    // Values: "security", "performance", 
    //         "bug", "best-practice"
    @JsonProperty("category")
    private String category;

    // Which line has the issue
    // Can be null if it's a general file-level issue
    @JsonProperty("line")
    private Integer line;

    // Short description of what the problem is
    @JsonProperty("issue")
    private String issue;

    // Concrete suggestion on how to fix it
    @JsonProperty("fix")
    private String fix;
}