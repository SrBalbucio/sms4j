package balbucio.sms4j.modem.zte;

import balbucio.sms4j.SmsSendResult;
import balbucio.sms4j.Sms4jException;
import balbucio.sms4j.at.AtChannel;
import balbucio.sms4j.at.AtResponse;

import java.nio.charset.StandardCharsets;

/**
 * Driver for ZTE MF710 modem: initialization (AT, CMEE, CMGF) and sendSms via AT+CMGS.
 * Only mandatory commands (AT, CMGF=1) fail initialization; CMEE=1 is optional (some MF710 return "unknown").
 */
public class ZteMf710Driver {

    private static final long CMGS_TIMEOUT_MS = 30_000L;
    private static final byte CTRL_Z = 0x1A;

    private final AtChannel atChannel;
    private boolean initialized;

    public ZteMf710Driver(AtChannel atChannel) {
        this.atChannel = atChannel;
    }

    /**
     * Initializes the modem: AT (handshake), AT+CMEE=1 (optional), AT+CMGF=1 (text mode). Fails if AT or CMGF fails.
     *
     * @throws Sms4jException if AT or AT+CMGF=1 fails
     */
    public void initialize() throws Sms4jException {
        AtResponse at = atChannel.sendCommand("AT");
        if (!at.isOk()) {
            throw new Sms4jException("Modem handshake failed (AT): " + String.join(" ", at.getLines()));
        }

        AtResponse cmee = atChannel.sendCommand("AT+CMEE=1");
        if (cmee.isError() || cmee.isUnknown()) {
            // Optional; continue
        }

        AtResponse cmgf = atChannel.sendCommand("AT+CMGF=1");
        if (!cmgf.isOk()) {
            throw new Sms4jException("SMS text mode failed (AT+CMGF=1): " + String.join(" ", cmgf.getLines()));
        }

        initialized = true;
    }

    /**
     * Sends an SMS in text mode. Phone number should be in international format (e.g. +5511999999999).
     * Message encoding is UTF-8; modem may use GSM 7-bit or UTF-16 depending on character set (see modem docs).
     *
     * @param phoneNumber destination number (with + and country code)
     * @param message     SMS body
     * @return result with success/failure and raw response
     */
    public SmsSendResult sendSms(String phoneNumber, String message) {
        if (!initialized) {
            return SmsSendResult.failure("Driver not initialized", "");
        }

        String number = normalizeNumber(phoneNumber);
        String command = "AT+CMGS=\"" + number + "\"";
        byte[] payload = (message + (char) CTRL_Z).getBytes(StandardCharsets.UTF_8);

        AtResponse response = atChannel.sendCommandWithPayload(command, payload, CMGS_TIMEOUT_MS);

        String raw = String.join("\n", response.getLines());

        if (response.isOk()) {
            String ref = extractMessageRef(response.getLines());
            return SmsSendResult.success(ref, raw);
        }
        if (response.isUnknown()) {
            return SmsSendResult.failure("Command not supported or unknown response", raw);
        }
        return SmsSendResult.failure("Send failed: " + raw, raw);
    }

    private static String normalizeNumber(String phoneNumber) {
        if (phoneNumber == null) return "";
        String s = phoneNumber.trim();
        if (s.isEmpty()) return s;
        if (s.startsWith("+")) return s;
        if (s.startsWith("00")) return "+" + s.substring(2);
        return "+" + s;
    }

    private static String extractMessageRef(java.util.List<String> lines) {
        for (String line : lines) {
            if (line != null && line.startsWith("+CMGS:")) {
                String rest = line.substring(6).trim();
                int comma = rest.indexOf(',');
                if (comma > 0) rest = rest.substring(0, comma);
                return rest.trim();
            }
        }
        return null;
    }
}
