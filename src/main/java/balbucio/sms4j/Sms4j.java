package balbucio.sms4j;

import balbucio.sms4j.at.AtChannel;
import balbucio.sms4j.modem.DriverRegistry;
import balbucio.sms4j.modem.ModemDriver;
import balbucio.sms4j.modem.ModemDriverFactory;
import balbucio.sms4j.serial.SerialConnection;

/**
 * Main API: one instance manages one modem on the given serial port.
 * The driver can be selected automatically (AT + ATI probe) or set manually.
 * Call {@link #open()} before {@link #sendSms(String, String)}, then {@link #close()} when done.
 */
public class Sms4j {

    private final String portName;
    private final ModemDriverFactory explicitDriverFactory;
    private final DriverRegistry registry;

    private SerialConnection connection;
    private AtChannel atChannel;
    private ModemDriver driver;
    private boolean open;

    /**
     * Creates an SMS4J instance for the given port. On {@link #open()}, the modem is probed (AT then ATI)
     * and the driver is selected from {@link DriverRegistry#getDefault()} by model (e.g. MF710).
     *
     * @param portName system port descriptor (e.g. "COM3", "/dev/ttyUSB0")
     */
    public Sms4j(String portName) {
        this(portName, (ModemDriverFactory) null);
    }

    /**
     * Creates an SMS4J instance that will use the given driver factory when opened.
     * No ATI probe is performed; use this when auto-detect fails or you want a specific driver.
     *
     * @param portName system port descriptor
     * @param driverFactory factory that creates the driver (e.g. {@code ZteMf710Driver::new})
     */
    public Sms4j(String portName, ModemDriverFactory driverFactory) {
        this(portName, driverFactory, DriverRegistry.getDefault());
    }

    /**
     * Creates an SMS4J instance with a custom registry (for tests or custom driver set).
     * If {@code driverFactory} is null, the driver is resolved by probing (AT + ATI) using {@code registry}.
     *
     * @param portName system port descriptor
     * @param driverFactory optional factory; if null, driver is auto-detected via registry
     * @param registry registry used when auto-detecting (ignored if driverFactory is non-null)
     */
    public Sms4j(String portName, ModemDriverFactory driverFactory, DriverRegistry registry) {
        this.portName = portName;
        this.explicitDriverFactory = driverFactory;
        this.registry = registry != null ? registry : DriverRegistry.getDefault();
    }

    /**
     * Opens the port and initializes the modem. If no driver was set explicitly, sends AT then ATI to detect
     * the model and selects the driver from the registry; then initializes the driver.
     *
     * @throws PortInUseException if the port cannot be opened
     * @throws Sms4jException     if the port is not found, probe fails, or modem init fails
     */
    public void open() throws PortInUseException, Sms4jException {
        if (open) {
            return;
        }
        connection = new SerialConnection(portName);
        connection.open();
        atChannel = new AtChannel(connection);

        if (explicitDriverFactory != null) {
            driver = explicitDriverFactory.create(atChannel);
        } else {
            ModemDriverFactory factory = registry.probe(atChannel);
            driver = factory.create(atChannel);
        }

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
