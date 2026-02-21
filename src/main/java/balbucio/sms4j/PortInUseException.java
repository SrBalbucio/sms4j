package balbucio.sms4j;

/**
 * Thrown when the requested serial port cannot be opened (e.g. already in use).
 */
public class PortInUseException extends Sms4jException {

    public PortInUseException(String message) {
        super(message);
    }

    public PortInUseException(String message, Throwable cause) {
        super(message, cause);
    }
}
