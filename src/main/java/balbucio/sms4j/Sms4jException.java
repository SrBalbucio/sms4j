package balbucio.sms4j;

/**
 * Base exception for SMS4J library errors (I/O, modem response, configuration).
 */
public class Sms4jException extends Exception {

    public Sms4jException(String message) {
        super(message);
    }

    public Sms4jException(String message, Throwable cause) {
        super(message, cause);
    }
}
