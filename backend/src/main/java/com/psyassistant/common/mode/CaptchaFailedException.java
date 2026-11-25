package com.psyassistant.common.mode;

/**
 * Thrown when the CAPTCHA check on a public form fails.
 */
public class CaptchaFailedException extends RuntimeException {

    /** Creates the exception. */
    public CaptchaFailedException() {
        super("CAPTCHA verification failed");
    }
}
