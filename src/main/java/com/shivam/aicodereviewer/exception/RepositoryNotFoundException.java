package com.shivam.aicodereviewer.exception;

public class RepositoryNotFoundException
    extends RuntimeException {

    public RepositoryNotFoundException(
        String owner, String repo) {
        super("Repository '" + owner + "/" + repo
            + "' not found");
    }
}