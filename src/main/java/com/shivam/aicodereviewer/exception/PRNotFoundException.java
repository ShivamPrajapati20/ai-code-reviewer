package com.shivam.aicodereviewer.exception;

// Custom exception thrown when PR doesn't exist
// on GitHub
public class PRNotFoundException 
    extends RuntimeException {

    public PRNotFoundException(
        String owner, String repo, Integer prNumber) {
        super("PR #" + prNumber + " not found in "
            + owner + "/" + repo);
    }
}