package balbucio.sms4j.modem;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import balbucio.sms4j.Sms4jException;
import balbucio.sms4j.at.AtChannel;
import balbucio.sms4j.at.AtResponse;
import balbucio.sms4j.modem.zte.ZteMf710Driver;

/**
 * Maps modem model identifiers (from ATI response) to driver factories.
 * Used by {@link balbucio.sms4j.Sms4j} to select a driver when opening by port only.
 * <p>
 * Default registration: {@code MF710} → {@link ZteMf710Driver}.
 * Use {@link #register(String, ModemDriverFactory)} to add more modems.
 */
public final class DriverRegistry {

    private static final DriverRegistry DEFAULT = new DriverRegistry();

    static {
        DEFAULT.register("MF710", ZteMf710Driver::new);
    }

    private final Map<String, ModemDriverFactory> byModel = new ConcurrentHashMap<>();

    /**
     * Returns the default registry (includes MF710).
     */
    public static DriverRegistry getDefault() {
        return DEFAULT;
    }

    /**
     * Registers a factory for the given model identifier.
     * The identifier is matched case-insensitively; ATI response may contain it (e.g. "ZTE MF710" matches "MF710").
     *
     * @param modelId model string (e.g. "MF710")
     * @param factory factory that creates the driver given an AtChannel
     */
    public void register(String modelId, ModemDriverFactory factory) {
        if (modelId != null && factory != null) {
            byModel.put(modelId.trim().toUpperCase(), factory);
        }
    }

    /**
     * Resolves the driver factory for the given ATI response lines.
     * Expects lines from a successful ATI response (e.g. ["MF710", "OK"] or ["ZTE MF710", "OK"]).
     * The first non-empty line that is not "OK" is treated as the model line; it is matched against
     * registered ids (exact or contained).
     *
     * @param atiLines lines from ATI response (without the final status)
     * @return factory for this model
     * @throws Sms4jException if no driver is registered for the model
     */
    public ModemDriverFactory resolve(List<String> atiLines) throws Sms4jException {
        String modelLine = parseModelLine(atiLines);
        if (modelLine == null || modelLine.isBlank()) {
            throw new Sms4jException("Could not determine modem model from ATI response: " + atiLines);
        }
        String normalized = modelLine.toUpperCase();
        // Exact match
        ModemDriverFactory factory = byModel.get(normalized);
        if (factory != null) {
            return factory;
        }
        // Contains match: e.g. "ZTE MF710" contains "MF710"
        for (Map.Entry<String, ModemDriverFactory> e : byModel.entrySet()) {
            if (normalized.contains(e.getKey())) {
                return e.getValue();
            }
        }
        throw new Sms4jException("No driver registered for modem model: " + modelLine
                + ". Use Sms4j(port, driverFactory) to specify the driver manually.");
    }

    /**
     * Probes the modem on the given channel: sends AT, then ATI, and resolves the driver from the ATI response.
     *
     * @param atChannel open AT channel
     * @return factory for the detected model
     * @throws Sms4jException if AT fails, ATI fails, or no driver is registered for the model
     */
    public ModemDriverFactory probe(AtChannel atChannel) throws Sms4jException {
        AtResponse at = atChannel.sendCommand("AT");
        if (!at.isOk()) {
            throw new Sms4jException("Modem did not respond to AT: " + String.join(" ", at.getLines()));
        }
        AtResponse ati = atChannel.sendCommand("ATI");
        if (!ati.isOk()) {
            throw new Sms4jException("Modem did not respond to ATI: " + String.join(" ", ati.getLines()));
        }
        List<String> lines = ati.getLines();
        return resolve(lines);
    }

    private static String parseModelLine(List<String> atiLines) {
        if (atiLines == null) return null;
        for (String line : atiLines) {
            if (line == null) continue;
            String s = line.trim();
            if (s.isEmpty()) continue;
            if ("OK".equalsIgnoreCase(s)) continue;
            if ("ATI".equalsIgnoreCase(s)) continue;
            return s;
        }
        return null;
    }

    /**
     * Returns a new registry with no registrations (for tests or custom defaults).
     */
    public static DriverRegistry createEmpty() {
        return new DriverRegistry();
    }
}
