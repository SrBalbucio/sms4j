package balbucio.sms4j.at;

import balbucio.sms4j.serial.SerialPortAccess;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Sends AT commands over a serial connection and reads response until OK, ERROR, UNKNOWN, or timeout.
 * Lines containing "unknown" (case-insensitive) are treated as UNKNOWN (command not supported).
 */
public class AtChannel {

    private static final long DEFAULT_TIMEOUT_MS = 5000L;
    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.US_ASCII);

    private final SerialPortAccess connection;

    public AtChannel(SerialPortAccess connection) {
        this.connection = connection;
    }

    /**
     * Sends an AT command and reads response with default timeout.
     *
     * @param command command string without CR/LF (e.g. "AT", "AT+CMGF=1")
     * @return response with status and raw lines
     */
    public AtResponse sendCommand(String command) {
        return sendCommand(command, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Sends an AT command and reads response with the given timeout.
     *
     * @param command   command string without CR/LF
     * @param timeoutMs read timeout in milliseconds (use higher value for CMGS)
     * @return response with status and raw lines
     */
    public AtResponse sendCommand(String command, long timeoutMs) {
        List<String> lines = new ArrayList<>();
        InputStream in = connection.getInputStream();
        OutputStream out = connection.getOutputStream();
        if (in == null || out == null) {
            return new AtResponse(AtResponse.Status.ERROR, List.of("Port not open"));
        }

        try {
            if (timeoutMs != DEFAULT_TIMEOUT_MS && timeoutMs > 0 && timeoutMs <= Integer.MAX_VALUE) {
                connection.setReadTimeoutMs((int) timeoutMs);
            }
            out.write(command.getBytes(StandardCharsets.US_ASCII));
            out.write(CRLF);
            out.flush();

            StringBuilder lineBuffer = new StringBuilder();
            long deadline = System.currentTimeMillis() + timeoutMs;

            while (System.currentTimeMillis() < deadline) {
                int b = in.read();
                if (b < 0) {
                    break;
                }
                char c = (char) (b & 0xFF);
                if (c == '\r') {
                    continue;
                }
                if (c == '\n') {
                    String line = lineBuffer.toString().trim();
                    lineBuffer.setLength(0);
                    if (line.isEmpty()) {
                        continue;
                    }
                    lines.add(line);
                    String upper = line.toUpperCase();
                    if ("OK".equals(upper)) {
                        return new AtResponse(AtResponse.Status.OK, lines);
                    }
                    if ("ERROR".equals(upper)) {
                        return new AtResponse(AtResponse.Status.ERROR, lines);
                    }
                    if (upper.contains("UNKNOWN")) {
                        return new AtResponse(AtResponse.Status.UNKNOWN, lines);
                    }
                    continue;
                }
                lineBuffer.append(c);
            }

            if (lineBuffer.length() > 0) {
                lines.add(lineBuffer.toString().trim());
            }
            return new AtResponse(AtResponse.Status.ERROR, lines);
        } catch (IOException e) {
            lines.add("IOException: " + e.getMessage());
            return new AtResponse(AtResponse.Status.ERROR, lines);
        } finally {
            if (timeoutMs != DEFAULT_TIMEOUT_MS) {
                connection.setReadTimeoutMs(5000);
            }
        }
    }

    /**
     * Sends an AT command that expects a ">" prompt (e.g. AT+CMGS), then sends the payload (e.g. message + Ctrl+Z),
     * then reads until OK/ERROR/UNKNOWN. Use for SMS send.
     *
     * @param command   command string (e.g. "AT+CMGS=\"+5511999999999\"")
     * @param payload   data to send after the prompt (e.g. message bytes + 0x1A)
     * @param timeoutMs timeout for both waiting for ">" and for final OK/ERROR
     * @return response with status and raw lines
     */
    public AtResponse sendCommandWithPayload(String command, byte[] payload, long timeoutMs) {
        List<String> lines = new ArrayList<>();
        InputStream in = connection.getInputStream();
        OutputStream out = connection.getOutputStream();
        if (in == null || out == null) {
            return new AtResponse(AtResponse.Status.ERROR, List.of("Port not open"));
        }

        try {
            if (timeoutMs > 0 && timeoutMs <= Integer.MAX_VALUE) {
                connection.setReadTimeoutMs((int) timeoutMs);
            }
            out.write(command.getBytes(StandardCharsets.US_ASCII));
            out.write(CRLF);
            out.flush();

            long deadline = System.currentTimeMillis() + timeoutMs;
            StringBuilder lineBuffer = new StringBuilder();
            boolean seenPrompt = false;

            while (System.currentTimeMillis() < deadline && !seenPrompt) {
                int b = in.read();
                if (b < 0) break;
                char c = (char) (b & 0xFF);
                if (c == '\r') continue;
                if (c == '\n') {
                    String line = lineBuffer.toString().trim();
                    lineBuffer.setLength(0);
                    if (!line.isEmpty()) lines.add(line);
                    if (line.contains(">")) {
                        seenPrompt = true;
                        break;
                    }
                    continue;
                }
                lineBuffer.append(c);
            }

            if (!seenPrompt) {
                if (lineBuffer.length() > 0) lines.add(lineBuffer.toString().trim());
                return new AtResponse(AtResponse.Status.ERROR, lines);
            }

            out.write(payload);
            out.flush();

            lineBuffer.setLength(0);
            deadline = System.currentTimeMillis() + timeoutMs;

            while (System.currentTimeMillis() < deadline) {
                int b = in.read();
                if (b < 0) break;
                char c = (char) (b & 0xFF);
                if (c == '\r') continue;
                if (c == '\n') {
                    String line = lineBuffer.toString().trim();
                    lineBuffer.setLength(0);
                    if (line.isEmpty()) continue;
                    lines.add(line);
                    String upper = line.toUpperCase();
                    if ("OK".equals(upper)) return new AtResponse(AtResponse.Status.OK, lines);
                    if ("ERROR".equals(upper)) return new AtResponse(AtResponse.Status.ERROR, lines);
                    if (upper.contains("UNKNOWN")) return new AtResponse(AtResponse.Status.UNKNOWN, lines);
                    continue;
                }
                lineBuffer.append(c);
            }

            if (lineBuffer.length() > 0) lines.add(lineBuffer.toString().trim());
            return new AtResponse(AtResponse.Status.ERROR, lines);
        } catch (IOException e) {
            lines.add("IOException: " + e.getMessage());
            return new AtResponse(AtResponse.Status.ERROR, lines);
        } finally {
            connection.setReadTimeoutMs(5000);
        }
    }

    /**
     * Sends raw data (e.g. SMS body + Ctrl+Z) without appending CR/LF. Caller is responsible for terminator.
     * Does not read response; use when the response will be read by a subsequent sendCommand or custom read.
     *
     * @param data bytes to send
     */
    public void sendRaw(byte[] data) throws IOException {
        OutputStream out = connection.getOutputStream();
        if (out != null) {
            out.write(data);
            out.flush();
        }
    }
}
