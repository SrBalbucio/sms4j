package balbucio.sms4j.at;

import java.util.Collections;
import java.util.List;

/**
 * Result of sending a single AT command: status (OK, ERROR, UNKNOWN) and raw lines.
 */
public final class AtResponse {

    public enum Status {
        OK,
        ERROR,
        UNKNOWN
    }

    private final Status status;
    private final List<String> lines;

    public AtResponse(Status status, List<String> lines) {
        this.status = status;
        this.lines = lines == null ? List.of() : Collections.unmodifiableList(lines);
    }

    public Status getStatus() {
        return status;
    }

    public List<String> getLines() {
        return lines;
    }

    public boolean isOk() {
        return status == Status.OK;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }

    public boolean isUnknown() {
        return status == Status.UNKNOWN;
    }
}
