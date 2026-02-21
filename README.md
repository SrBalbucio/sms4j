# SMS4J

Library for sending SMS via USB modem (AT commands over serial) in Java 17. One instance manages one modem on a given port. Currently supports **ZTE MF710**.

## Requirements

- **Java 17**
- **jSerialComm** (included as Maven dependency) for serial communication

## Finding the modem port

- **Windows**: Device Manager → Ports (COM & LPT), or use a port enumerator. Typical name: `COM3`, `COM4`, etc.
- **Linux**: Often `/dev/ttyUSB0` or `/dev/ttyACM0` when the modem is plugged in. List with `ls /dev/tty*`.

You can also list available ports in code with jSerialComm:

```java
import com.fazecast.jSerialComm.SerialPort;

for (SerialPort p : SerialPort.getCommPorts()) {
    System.out.println(p.getSystemPortName() + " - " + p.getPortDescription());
}
```

## Usage

**Auto-detect driver (default):** When you only pass the port, the library sends **AT** then **ATI** to detect the modem model and selects the driver from a registry (e.g. MF710 → ZTE MF710 driver).

```java
import balbucio.sms4j.Sms4j;
import balbucio.sms4j.SmsSendResult;
import balbucio.sms4j.Sms4jException;
import balbucio.sms4j.PortInUseException;

// One instance = one modem; driver is selected automatically via ATI
Sms4j modem = new Sms4j("COM3");

try {
    modem.open();
    SmsSendResult result = modem.sendSms("+5511999999999", "Hello from SMS4J");
    if (result.isSuccess()) {
        System.out.println("Sent. Ref: " + result.getMessageRef());
    } else {
        System.out.println("Failed: " + result.getErrorMessage());
        System.out.println("Raw: " + result.getRawResponse());
    }
} catch (PortInUseException e) {
    System.err.println("Port in use: " + e.getMessage());
} catch (Sms4jException e) {
    System.err.println("Error: " + e.getMessage());
} finally {
    modem.close();
}
```

**Manual driver:** If auto-detect fails or you want to force a specific driver, pass a factory:

```java
import balbucio.sms4j.Sms4j;
import balbucio.sms4j.modem.zte.ZteMf710Driver;

Sms4j modem = new Sms4j("COM3", ZteMf710Driver::new);
modem.open();
// ...
modem.close();
```

## Integration tests

Some tests require a modem and are skipped unless configured:

| Config | Source | Example |
|--------|--------|--------|
| Port | `sms4j.test.port` (system property) | `-Dsms4j.test.port=COM3` |
| Phone (for send test) | `sms4j.test.phone` (property) or `SMS4J_TEST_PHONE` (env) | `-Dsms4j.test.phone=+5511999999999` |

Example with real modem and SMS send:

```bash
mvn test -Dsms4j.test.port=COM3 -Dsms4j.test.phone=+5511999999999
```

Or set `SMS4J_TEST_PHONE` in the environment and only pass the port to Maven.

## Encoding

SMS body is sent in **UTF-8**. The modem may use GSM 7-bit or other encoding depending on the character set; for extended characters, refer to your modem’s documentation.

## License

This project is under the **MIT License**. See [license.txt](license.txt).

**Dependencies:**

- [jSerialComm](https://github.com/Fazecast/jSerialComm) – LGPL (or commercial license). See the project for terms.
