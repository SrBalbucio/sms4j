package balbucio.sms4j;

/**
 * Result of a single SMS send attempt.
 */
public final class SmsSendResult {

    private final boolean success;
    private final String messageRef;
    private final String rawResponse;
    private final String errorMessage;

    private SmsSendResult(boolean success, String messageRef, String rawResponse, String errorMessage) {
        this.success = success;
        this.messageRef = messageRef;
        this.rawResponse = rawResponse;
        this.errorMessage = errorMessage;
    }

    /**
     * Creates a successful result.
     *
     * @param messageRef optional reference from modem (e.g. index), may be null
     * @param rawResponse raw AT response lines, for debugging
     */
    public static SmsSendResult success(String messageRef, String rawResponse) {
        return new SmsSendResult(true, messageRef, rawResponse, null);
    }

    /**
     * Creates a failed result.
     *
     * @param errorMessage description of the failure
     * @param rawResponse raw AT response lines, for debugging
     */
    public static SmsSendResult failure(String errorMessage, String rawResponse) {
        return new SmsSendResult(false, null, rawResponse, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessageRef() {
        return messageRef;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
