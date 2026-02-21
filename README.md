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

```java
import balbucio.sms4j.Sms4j;
import balbucio.sms4j.SmsSendResult;
import balbucio.sms4j.Sms4jException;
import balbucio.sms4j.PortInUseException;

// One instance = one modem; pass the port name (e.g. "COM3" or "/dev/ttyUSB0")
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

## AT commands and ZTE MF710

The library uses standard 3GPP/ZTE SMS commands:

- **AT** – handshake
- **AT+CMEE=1** – verbose error codes (optional; some MF710 return "unknown", which is ignored)
- **AT+CMGF=1** – SMS in text mode
- **AT+CMGS="+55..."** – send SMS (then message body + Ctrl+Z)

Some AT commands on the MF710 return `"unknown"` (e.g. proprietary ones like `AT+ZCDRUN`). The AT layer treats such responses as unsupported and does not break the flow for optional commands. Only **AT** and **AT+CMGF=1** are required for sending; if they fail, `open()` or `sendSms()` will fail.

Reference documentation (may or may not match MF710 exactly):

- [ZTE AT Command Set (PDF)](https://d1.amobbs.com/bbs_upload782111/files_44/ourdev_665123DHORCA.pdf)
- [ZTE MG2639 AT Manual (SMS set often similar) (PDF)](https://www.mouser.com/datasheet/2/813/AT_Command_Manual_For_ZTE_Corporation_MG2639_V2_Mo-2489961.pdf)

## Encoding

SMS body is sent in **UTF-8**. The modem may use GSM 7-bit or other encoding depending on the character set; for extended characters, refer to your modem’s documentation.

## License

This project is under the **MIT License**. See [license.txt](license.txt).

**Dependencies:**

- [jSerialComm](https://github.com/Fazecast/jSerialComm) – LGPL (or commercial license). See the project for terms.
