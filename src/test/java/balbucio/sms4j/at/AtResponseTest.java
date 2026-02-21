package balbucio.sms4j.at;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class AtResponseTest {

    @Test
    public void statusOk_isOkReturnsTrue() {
        AtResponse r = new AtResponse(AtResponse.Status.OK, List.of("OK"));
        assertTrue(r.isOk());
        assertFalse(r.isError());
        assertFalse(r.isUnknown());
    }

    @Test
    public void statusError_isErrorReturnsTrue() {
        AtResponse r = new AtResponse(AtResponse.Status.ERROR, List.of("ERROR"));
        assertFalse(r.isOk());
        assertTrue(r.isError());
    }

    @Test
    public void linesAreUnmodifiable() {
        AtResponse r = new AtResponse(AtResponse.Status.OK, List.of("a", "b"));
        assertEquals(2, r.getLines().size());
        try {
            r.getLines().add("c");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }
}
