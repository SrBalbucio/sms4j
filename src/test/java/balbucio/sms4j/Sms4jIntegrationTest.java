package balbucio.sms4j;

import org.junit.Assume;
import org.junit.Test;

/**
 * Integration test: runs only when system property {@code sms4j.test.port} is set (e.g. COM3).
 * Skip when no modem is connected: {@code mvn test -Dsms4j.test.port=COM3}
 */
public class Sms4jIntegrationTest {

    @Test
    public void openAndClose_whenPortConfigured() throws Exception {
        String port = System.getProperty("sms4j.test.port");
        Assume.assumeNotNull("Set -Dsms4j.test.port=COMx to run", port);

        Sms4j modem = new Sms4j(port);
        try {
            modem.open();
            Assume.assumeTrue("Port should be open", modem.isOpen());
        } finally {
            modem.close();
        }
    }
}
