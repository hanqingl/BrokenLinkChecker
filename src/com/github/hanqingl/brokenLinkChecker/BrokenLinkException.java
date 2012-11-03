package com.github.hanqingl.brokenLinkChecker;

public class BrokenLinkException extends RuntimeException {

    private static final long serialVersionUID = 384193073706819306L;

    public BrokenLinkException() {
        super();
    }

    public BrokenLinkException(String message) {
        super(message);
    }

    public BrokenLinkException(String message, Throwable cause) {
        super(message, cause);
    }

    public BrokenLinkException(Throwable cause) {
        super(cause);
    }

}
