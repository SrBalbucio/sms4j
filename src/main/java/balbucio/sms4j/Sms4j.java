package balbucio.sms4j;

import balbucio.sms4j.at.AtChannel;
import balbucio.sms4j.modem.zte.ZteMf710Driver;
import balbucio.sms4j.serial.SerialConnection;

/**
 * Main API: one instance manages one modem on the given serial port.
 * Supports ZTE MF710. Call {@link #open()} before {@link #sendSms(String, String)}, then {@link #close()} when done.
 */
public class Sms4j {

    private final String portName;
    private SerialConnection connection;
    private AtChannel atChannel;
    private ZteMf710Driver driver;
    private boolean open;

    /**
     * Creates an SMS4J instance for the given port (e.g. "COM3" on Windows, "/dev/ttyUSB0" on Linux).
     *
     * @param portName system port descriptor
     */
    public Sms4j(String portName) {
        this.portName = portName;
    }

    /**
     * Opens the port and initializes the modem (AT, CMGF=1). Must be called before sendSms.
     *
     * @throws PortInUseException if the port cannot be opened
     * @throws Sms4jException     if the port is not found or modem init fails
     */
    public void open() throws PortInUseException, Sms4jException {
        if (open) {
            return;
        }
        connection = new SerialConnection(portName);
        connection.open();
        atChannel = new AtChannel(connection);
        driver = new ZteMf710Driver(atChannel);
        driver.initialize();
        open = true;
    }

    /**
     * Closes the serial port and releases resources. Safe to call if already closed.
     */
    public void close() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
        atChannel = null;
        driver = null;
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    /**
     * Sends an SMS. Phone number should be in international format (e.g. +5511999999999).
     *
     * @param phoneNumber destination number
     * @param message     SMS body (encoding: UTF-8; see modem docs for GSM 7-bit limits)
     * @return result with success/failure and optional message ref
     * @throws Sms4jException if modem is not open
     */
    public SmsSendResult sendSms(String phoneNumber, String message) throws Sms4jException {
        if (!open || driver == null) {
            throw new Sms4jException("Modem not open. Call open() first.");
        }
        return driver.sendSms(phoneNumber, message);
    }

    /**
     * Returns the port name this instance is bound to.
     */
    public String getPortName() {
        return portName;
    }
}
