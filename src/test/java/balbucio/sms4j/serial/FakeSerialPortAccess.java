package balbucio.sms4j.serial;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Test double: provides fixed streams (e.g. from pipes) so AtChannel can be tested without a real port.
 */
public final class FakeSerialPortAccess implements SerialPortAccess {

    private final InputStream inputStream;
    private final OutputStream outputStream;

    public FakeSerialPortAccess(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public void setReadTimeoutMs(int timeoutMs) {
        // no-op for test
    }
}
