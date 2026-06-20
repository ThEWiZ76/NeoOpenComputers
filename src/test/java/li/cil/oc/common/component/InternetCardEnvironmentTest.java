package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InternetCardEnvironmentTest {
    @Test
    void exposesInternetCallbacks() throws NoSuchMethodException {
        assertCallback("isHttpEnabled");
        assertCallback("request");
        assertCallback("isTcpEnabled");
        assertCallback("connect");
    }

    @Test
    void reportsInternetDeviceInfo() {
        OpenComputersApi.initialize();

        DeviceInfo info = assertInstanceOf(DeviceInfo.class, new InternetCardEnvironment());
        Map<String, String> metadata = info.getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Communication, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Internet modem", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("SuperLink X-D4NK", metadata.get(DeviceInfo.DeviceAttribute.Product));
    }

    @Test
    void httpRequestReadsTransportResponse() throws Exception {
        OpenComputersApi.initialize();
        InternetCardEnvironment card = new InternetCardEnvironment((url, postData, headers, method) -> {
            assertEquals("https://example.test/index.txt", url);
            assertArrayEquals("payload".getBytes(StandardCharsets.UTF_8), postData);
            assertEquals("POST", method);
            assertEquals("Test", headers.get("user-agent"));
            return CompletableFuture.completedFuture(new InternetCardEnvironment.HttpResponse(
                200,
                "OK",
                Map.of("content-type", List.of("text/plain")),
                "hello".getBytes(StandardCharsets.UTF_8)));
        });

        Object handle = card.request(null, new TestArguments(
            "https://example.test/index.txt",
            "payload".getBytes(StandardCharsets.UTF_8),
            Map.of("user-agent", "Test"),
            "POST"))[0];

        InternetCardEnvironment.HttpRequest request = assertInstanceOf(InternetCardEnvironment.HttpRequest.class, handle);
        assertArrayEquals(new Object[]{true}, request.finishConnect(null, new TestArguments()));
        assertArrayEquals(new Object[]{200, "OK", Map.of("content-type", List.of("text/plain"))}, request.response(null, new TestArguments()));
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), (byte[]) request.read(null, new TestArguments(32))[0]);
        assertArrayEquals(new Object[]{null}, request.read(null, new TestArguments(32)));
    }

    @Test
    void invalidHttpSchemeFailsLikeUpstream() {
        OpenComputersApi.initialize();
        InternetCardEnvironment card = new InternetCardEnvironment((url, postData, headers, method) -> {
            throw new AssertionError("transport should not be called for invalid scheme");
        });

        try {
            card.request(null, new TestArguments("file:///tmp/data.txt"));
        } catch (IllegalArgumentException e) {
            assertEquals("unsupported protocol", e.getMessage());
            return;
        } catch (Exception e) {
            throw new AssertionError(e);
        }

        throw new AssertionError("expected invalid scheme to fail");
    }

    @Test
    void tcpConnectReportsUnavailableUntilTcpPortIsImplemented() throws Exception {
        OpenComputersApi.initialize();
        InternetCardEnvironment card = new InternetCardEnvironment();

        assertArrayEquals(new Object[]{false}, card.isTcpEnabled(null, new TestArguments()));
        assertArrayEquals(new Object[]{null, "tcp connections are unavailable"}, card.connect(null, new TestArguments("example.test", 80)));
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = InternetCardEnvironment.class.getMethod(methodName, li.cil.oc.api.machine.Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
    }

    private record TestArguments(Object... values) implements Arguments {
        @Override public int count() { return values.length; }
        @Override public Object checkAny(final int index) { return values[index]; }
        @Override public boolean checkBoolean(final int index) { return (Boolean) values[index]; }
        @Override public int checkInteger(final int index) { return ((Number) values[index]).intValue(); }
        @Override public long checkLong(final int index) { return ((Number) values[index]).longValue(); }
        @Override public double checkDouble(final int index) { return ((Number) values[index]).doubleValue(); }
        @Override public String checkString(final int index) { return (String) values[index]; }
        @Override public byte[] checkByteArray(final int index) {
            Object value = values[index];
            return value instanceof byte[] bytes ? bytes : ((String) value).getBytes(StandardCharsets.UTF_8);
        }
        @Override public Map checkTable(final int index) { return (Map) values[index]; }
        @Override public ItemStack checkItemStack(final int index) { return (ItemStack) values[index]; }
        @Override public Object optAny(final int index, final Object def) { return index >= 0 && index < values.length ? values[index] : def; }
        @Override public boolean optBoolean(final int index, final boolean def) { return index >= 0 && index < values.length ? checkBoolean(index) : def; }
        @Override public int optInteger(final int index, final int def) { return index >= 0 && index < values.length ? checkInteger(index) : def; }
        @Override public long optLong(final int index, final long def) { return index >= 0 && index < values.length ? checkLong(index) : def; }
        @Override public double optDouble(final int index, final double def) { return index >= 0 && index < values.length ? checkDouble(index) : def; }
        @Override public String optString(final int index, final String def) { return index >= 0 && index < values.length ? checkString(index) : def; }
        @Override public byte[] optByteArray(final int index, final byte[] def) { return index >= 0 && index < values.length ? checkByteArray(index) : def; }
        @Override public Map optTable(final int index, final Map def) { return index >= 0 && index < values.length ? checkTable(index) : def; }
        @Override public ItemStack optItemStack(final int index, final ItemStack def) { return index >= 0 && index < values.length ? checkItemStack(index) : def; }
        @Override public boolean isBoolean(final int index) { return index >= 0 && index < values.length && values[index] instanceof Boolean; }
        @Override public boolean isInteger(final int index) { return index >= 0 && index < values.length && values[index] instanceof Integer; }
        @Override public boolean isLong(final int index) { return index >= 0 && index < values.length && values[index] instanceof Long; }
        @Override public boolean isDouble(final int index) { return index >= 0 && index < values.length && values[index] instanceof Double; }
        @Override public boolean isString(final int index) { return index >= 0 && index < values.length && values[index] instanceof String; }
        @Override public boolean isByteArray(final int index) { return index >= 0 && index < values.length && values[index] instanceof byte[]; }
        @Override public boolean isTable(final int index) { return index >= 0 && index < values.length && values[index] instanceof Map; }
        @Override public boolean isItemStack(final int index) { return index >= 0 && index < values.length && values[index] instanceof ItemStack; }
        @Override public Object[] toArray() { return Arrays.copyOf(values, values.length); }
        @Override public Iterator<Object> iterator() { return Arrays.asList(values).iterator(); }
    }
}
