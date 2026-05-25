package com.shivam.aicodereviewer.exception;

public class UserNotFoundException
    extends RuntimeException {

    public UserNotFoundException(String owner) {
        super("GitHub user '" + owner + "' not found");
    }
}