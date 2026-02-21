package balbucio.sms4j.modem;

import balbucio.sms4j.Sms4jException;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;

public class DriverRegistryTest {

    @Test
    public void resolve_returnsFactory_forMf710Line() throws Sms4jException {
        DriverRegistry registry = DriverRegistry.getDefault();
        ModemDriverFactory factory = registry.resolve(List.of("MF710", "OK"));
        assertNotNull(factory);
    }

    @Test
    public void resolve_returnsFactory_whenModelLineContainsMf710() throws Sms4jException {
        DriverRegistry registry = DriverRegistry.getDefault();
        ModemDriverFactory factory = registry.resolve(List.of("ZTE MF710", "OK"));
        assertNotNull(factory);
    }

    @Test(expected = Sms4jException.class)
    public void resolve_throws_whenModelUnknown() throws Sms4jException {
        DriverRegistry registry = DriverRegistry.getDefault();
        registry.resolve(List.of("UNKNOWN_MODEM_XYZ", "OK"));
    }

    @Test(expected = Sms4jException.class)
    public void resolve_throws_whenLinesEmpty() throws Sms4jException {
        DriverRegistry registry = DriverRegistry.getDefault();
        registry.resolve(List.of("OK"));
    }
}
