package balbucio.sms4j.serial;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Minimal interface for serial I/O used by the AT layer (allows test doubles).
 */
public interface SerialPortAccess {

    InputStream getInputStream();

    OutputStream getOutputStream();

    void setReadTimeoutMs(int timeoutMs);
}
