package balbucio.sms4j.serial;

import java.io.InputStream;
import java.io.OutputStream;

import com.fazecast.jSerialComm.SerialPort;

import balbucio.sms4j.PortInUseException;
import balbucio.sms4j.Sms4jException;

/**
 * Thin wrapper over jSerialComm: open/close a port by name, configure baud and timeouts,
 * expose InputStream/OutputStream for the AT layer.
 */
public class SerialConnection implements SerialPortAccess {

    private static final int DEFAULT_BAUD_RATE = 115200;
    private static final int DEFAULT_READ_TIMEOUT_MS = 5000;
    private static final int DEFAULT_WRITE_TIMEOUT_MS = 1000;

    private final String portName;
    private final int baudRate;
    private SerialPort port;
    private InputStream inputStream;
    private OutputStream outputStream;

    /**
     * Creates a connection for the given port (e.g. "COM3" on Windows, "/dev/ttyUSB0" on Linux).
     *
     * @param portName system port descriptor
     */
    public SerialConnection(String portName) {
        this(portName, DEFAULT_BAUD_RATE);
    }

    /**
     * Creates a connection with custom baud rate.
     *
     * @param portName system port descriptor
     * @param baudRate baud rate (e.g. 9600, 115200)
     */
    public SerialConnection(String portName, int baudRate) {
        this.portName = portName;
        this.baudRate = baudRate;
    }

    /**
     * Opens the port and configures 8N1. Timeouts: semi-blocking read with default read timeout.
     *
     * @throws PortInUseException if the port cannot be opened (e.g. in use)
     * @throws Sms4jException     if the port is not found or configuration fails
     */
    public void open() throws PortInUseException, Sms4jException {
        port = SerialPort.getCommPort(portName);
        if (port == null) {
            throw new Sms4jException("Port not found: " + portName);
        }
        if (!port.openPort()) {
            throw new PortInUseException("Cannot open port: " + portName + " (may be in use)");
        }
        port.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, DEFAULT_READ_TIMEOUT_MS, DEFAULT_WRITE_TIMEOUT_MS);
        inputStream = port.getInputStream();
        outputStream = port.getOutputStream();
    }

    /**
     * Closes the port and releases resources. Safe to call if already closed.
     */
    public void close() {
        if (port != null && port.isOpen()) {
            port.closePort();
            port = null;
        }
        inputStream = null;
        outputStream = null;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    public boolean isOpen() {
        return port != null && port.isOpen();
    }

    public String getPortName() {
        return portName;
    }

    /**
     * Sets the read timeout in milliseconds. Call after open(). Used by AT layer for long-running commands (e.g. CMGS).
     *
     * @param timeoutMs read timeout in ms
     */
    @Override
    public void setReadTimeoutMs(int timeoutMs) {
        if (port != null && port.isOpen()) {
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeoutMs, DEFAULT_WRITE_TIMEOUT_MS);
        }
    }
}
