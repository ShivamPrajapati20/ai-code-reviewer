package com.shivam.aicodereviewer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PullRequestFile {

    // The name/path of the file that was changed
    // @JsonProperty maps GitHub's JSON field name 
    // to our Java field name
    @JsonProperty("filename")
    private String filename;

    // What happened to this file
    // possible values: added, removed, modified, renamed
    @JsonProperty("status")
    private String status;

    // The actual code changes in "diff" format
    // This is what we'll send to Claude for review
    @JsonProperty("patch")
    private String patch;

    // How many lines were added
    @JsonProperty("additions")
    private int additions;

    // How many lines were removed
    @JsonProperty("deletions")
    private int deletions;
}