package balbucio.sms4j.modem;

import balbucio.sms4j.at.AtChannel;

/**
 * Creates a {@link ModemDriver} for the given AT channel.
 * Used for dynamic driver selection (by model) or when the user supplies a driver explicitly.
 */
@FunctionalInterface
public interface ModemDriverFactory {

    /**
     * Creates a driver that will use the given AT channel.
     *
     * @param atChannel channel to use for AT commands
     * @return new driver instance
     */
    ModemDriver create(AtChannel atChannel);
}
