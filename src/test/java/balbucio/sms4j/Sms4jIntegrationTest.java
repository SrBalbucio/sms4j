package balbucio.sms4j;

import org.junit.Assume;
import org.junit.Test;

/**
 * Integration tests: run only when port (and for send test, phone) are configured.
 * <ul>
 *   <li>Port: system property {@code sms4j.test.port} (e.g. COM3)</li>
 *   <li>Phone (send test): system property {@code sms4j.test.phone} or env {@code SMS4J_TEST_PHONE}</li>
 * </ul>
 * Example: {@code mvn test -Dsms4j.test.port=COM3 -Dsms4j.test.phone=+5511999999999}
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

    @Test
    public void sendSms_whenPortAndPhoneConfigured() throws Exception {
        String port = System.getProperty("sms4j.test.port");
        String phone = System.getProperty("sms4j.test.phone");
        if (phone == null || phone.isBlank()) {
            phone = System.getenv("SMS4J_TEST_PHONE");
        }
        Assume.assumeNotNull("Set -Dsms4j.test.port=COMx to run", port);
        Assume.assumeNotNull("Set -Dsms4j.test.phone=+55... or SMS4J_TEST_PHONE to run", phone);

        Sms4j modem = new Sms4j(port);
        try {
            modem.open();
            SmsSendResult result = modem.sendSms(phone.trim(), "SMS4J integration test");
            Assume.assumeTrue("Send failed: " + result.getErrorMessage(), result.isSuccess());
        } finally {
            modem.close();
        }
    }
}
