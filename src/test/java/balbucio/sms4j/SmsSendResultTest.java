package balbucio.sms4j;

import org.junit.Test;

import static org.junit.Assert.*;

public class SmsSendResultTest {

    @Test
    public void success_hasRefAndRaw() {
        SmsSendResult r = SmsSendResult.success("123", "OK");
        assertTrue(r.isSuccess());
        assertEquals("123", r.getMessageRef());
        assertEquals("OK", r.getRawResponse());
        assertNull(r.getErrorMessage());
    }

    @Test
    public void failure_hasMessageAndRaw() {
        SmsSendResult r = SmsSendResult.failure("Network error", "ERROR");
        assertFalse(r.isSuccess());
        assertNull(r.getMessageRef());
        assertEquals("Network error", r.getErrorMessage());
        assertEquals("ERROR", r.getRawResponse());
    }
}
