package com.shivam.aicodereviewer.exception;

public class PRNotOpenException extends RuntimeException {

    public PRNotOpenException(
        String owner,
        String repo,
        Integer prNumber,
        String state) {

        super("PR #" + prNumber + " in " + owner + "/" + repo
            + " is " + state
            + ". Only open pull requests can be reviewed.");
    }
}
