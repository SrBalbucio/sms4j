package balbucio.sms4j.at;

import balbucio.sms4j.serial.FakeSerialPortAccess;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for AtChannel using piped streams to simulate modem responses.
 */
public class AtChannelTest {

    private PipedInputStream modemReceives;
    private PipedOutputStream appWritesToModem;
    private PipedInputStream appReadsFromModem;
    private PipedOutputStream modemWritesToApp;
    private FakeSerialPortAccess fakeConnection;
    private AtChannel atChannel;
    private ExecutorService executor;

    @Before
    public void setUp() throws IOException {
        modemReceives = new PipedInputStream();
        appWritesToModem = new PipedOutputStream(modemReceives);
        appReadsFromModem = new PipedInputStream();
        modemWritesToApp = new PipedOutputStream(appReadsFromModem);
        fakeConnection = new FakeSerialPortAccess(appReadsFromModem, appWritesToModem);
        atChannel = new AtChannel(fakeConnection);
        executor = Executors.newSingleThreadExecutor();
    }

    private void startModemResponder(String response) {
        executor.submit(() -> {
            try {
                byte[] buf = new byte[256];
                modemReceives.read(buf);
                modemWritesToApp.write(response.getBytes(StandardCharsets.US_ASCII));
                modemWritesToApp.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void sendCommand_returnsOk_whenModemRepliesOk() throws Exception {
        startModemResponder("\r\nOK\r\n");

        AtResponse response = atChannel.sendCommand("AT");

        assertTrue(response.isOk());
        assertEquals(AtResponse.Status.OK, response.getStatus());
        assertTrue(response.getLines().stream().anyMatch("OK"::equals));
    }

    @Test
    public void sendCommand_returnsError_whenModemRepliesError() throws Exception {
        startModemResponder("\r\nERROR\r\n");

        AtResponse response = atChannel.sendCommand("AT+FOO=1");

        assertTrue(response.isError());
        assertEquals(AtResponse.Status.ERROR, response.getStatus());
    }

    @Test
    public void sendCommand_returnsUnknown_whenModemRepliesUnknown() throws Exception {
        startModemResponder("\r\nunknown\r\n");

        AtResponse response = atChannel.sendCommand("AT+ZCDRUN");

        assertTrue(response.isUnknown());
        assertEquals(AtResponse.Status.UNKNOWN, response.getStatus());
    }

    @Test
    public void sendCommand_capturesIntermediateLines() throws Exception {
        startModemResponder("\r\n+FOO: 1,2\r\nOK\r\n");

        AtResponse response = atChannel.sendCommand("AT+FOO?");

        assertTrue(response.isOk());
        assertTrue(response.getLines().stream().anyMatch(l -> l.contains("+FOO:")));
    }

    @org.junit.After
    public void tearDown() throws Exception {
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
        appWritesToModem.close();
        modemWritesToApp.close();
    }
}
