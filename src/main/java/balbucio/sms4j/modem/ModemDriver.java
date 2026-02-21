package balbucio.sms4j.modem;

import balbucio.sms4j.Sms4jException;
import balbucio.sms4j.SmsSendResult;

/**
 * Contract for a modem driver: initialize and send SMS.
 * Drivers are created with an {@link balbucio.sms4j.at.AtChannel} via {@link ModemDriverFactory}.
 */
public interface ModemDriver {

    /**
     * Initializes the modem (handshake, SMS mode, etc.). Called once by {@link balbucio.sms4j.Sms4j#open()}.
     *
     * @throws Sms4jException if initialization fails
     */
    void initialize() throws Sms4jException;

    /**
     * Sends an SMS. Phone number in international format (e.g. +5511999999999).
     *
     * @param phoneNumber destination number
     * @param message     SMS body
     * @return result with success/failure and optional message ref
     */
    SmsSendResult sendSms(String phoneNumber, String message);
}
