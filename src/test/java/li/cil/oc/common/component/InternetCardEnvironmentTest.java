package li.cil.oc.common.component;

import li.cil.oc.api.API;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.Network;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void httpRequestReadUsesConfiguredMaxReadBuffer() throws Exception {
        OpenComputersApi.initialize();
        byte[] body = "abcdef".getBytes(StandardCharsets.UTF_8);
        InternetCardEnvironment card = new InternetCardEnvironment((url, postData, headers, method) ->
            CompletableFuture.completedFuture(new InternetCardEnvironment.HttpResponse(200, "OK", Map.of(), body)));
        InternetCardEnvironment.HttpRequest request = assertInstanceOf(
            InternetCardEnvironment.HttpRequest.class,
            card.request(null, new TestArguments("https://example.test/buffer"))[0]);

        withCachedConfig(ModSettings.MAX_READ_BUFFER, 3, () -> {
            assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), (byte[]) request.read(null, new TestArguments(10))[0]);
            assertArrayEquals("def".getBytes(StandardCharsets.UTF_8), (byte[]) request.read(null, new TestArguments())[0]);
        });
    }

    @Test
    void httpRequestUsesUpstreamDefaultUserAgent() throws Exception {
        OpenComputersApi.initialize();
        InternetCardEnvironment card = new InternetCardEnvironment();
        CompletableFuture<String> userAgent = new CompletableFuture<>();
        ExecutorService serverThread = Executors.newSingleThreadExecutor();

        try (ServerSocket server = new ServerSocket(0)) {
            serverThread.submit(() -> {
                try (Socket socket = server.accept();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1))) {
                    String header;
                    while ((header = reader.readLine()) != null && !header.isEmpty()) {
                        if (header.regionMatches(true, 0, "User-Agent:", 0, "User-Agent:".length())) {
                            userAgent.complete(header.substring("User-Agent:".length()).trim());
                        }
                    }
                    socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
                    socket.getOutputStream().flush();
                } catch (IOException e) {
                    userAgent.completeExceptionally(e);
                }
                return null;
            });

            Object handle = card.request(null, new TestArguments("http://127.0.0.1:" + server.getLocalPort() + "/headers"))[0];
            InternetCardEnvironment.HttpRequest request = assertInstanceOf(InternetCardEnvironment.HttpRequest.class, handle);
            awaitHttpConnected(request);

            assertEquals("opencomputers/" + API.VERSION, userAgent.get(2, TimeUnit.SECONDS));
        } finally {
            serverThread.shutdownNow();
            assertTrue(serverThread.awaitTermination(2, TimeUnit.SECONDS));
        }
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
    void limitsOpenConnectionsAndReleasesClosedHandles() throws Exception {
        OpenComputersApi.initialize();
        CompletableFuture<InternetCardEnvironment.HttpResponse> pending = new CompletableFuture<>();
        InternetCardEnvironment card = new InternetCardEnvironment((url, postData, headers, method) -> pending);
        InternetCardEnvironment.HttpRequest[] handles = new InternetCardEnvironment.HttpRequest[4];

        for (int i = 0; i < handles.length; i++) {
            Object handle = card.request(null, new TestArguments("https://example.test/" + i))[0];
            handles[i] = assertInstanceOf(InternetCardEnvironment.HttpRequest.class, handle);
        }

        IOException error = assertThrows(IOException.class, () -> card.request(null, new TestArguments("https://example.test/overflow")));
        assertEquals("too many open connections", error.getMessage());

        handles[0].close(null, new TestArguments());

        Object handle = card.request(null, new TestArguments("https://example.test/reopened"))[0];
        assertInstanceOf(InternetCardEnvironment.HttpRequest.class, handle);
    }

    @Test
    void usesConfiguredMaxTcpConnections() throws Exception {
        OpenComputersApi.initialize();
        CompletableFuture<InternetCardEnvironment.HttpResponse> pending = new CompletableFuture<>();
        InternetCardEnvironment card = new InternetCardEnvironment((url, postData, headers, method) -> pending);

        withCachedConfig(ModSettings.MAX_TCP_CONNECTIONS, 1, () -> {
            Object handle = card.request(null, new TestArguments("https://example.test/one"))[0];
            assertInstanceOf(InternetCardEnvironment.HttpRequest.class, handle);

            IOException error = assertThrows(IOException.class, () -> card.request(null, new TestArguments("https://example.test/two")));
            assertEquals("too many open connections", error.getMessage());
        });
    }

    @Test
    void rejectsCallsFromNonOwnerContextWhenOwned() throws Exception {
        OpenComputersApi.initialize();
        CompletableFuture<InternetCardEnvironment.HttpResponse> pending = new CompletableFuture<>();
        InternetCardEnvironment card = new InternetCardEnvironment((url, postData, headers, method) -> pending);
        TestComputerContext owner = new TestComputerContext();
        TestComputerContext intruder = new TestComputerContext();
        owner.node().connect(card.node());

        Object handle = card.request(owner, new TestArguments("https://example.test/owner"))[0];
        assertInstanceOf(InternetCardEnvironment.HttpRequest.class, handle);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> card.request(intruder, new TestArguments("https://example.test/intruder")));
        assertEquals("can only be used by the owning computer", error.getMessage());
    }

    @Test
    void tcpSocketConnectsWritesAndReadsLoopbackData() throws Exception {
        OpenComputersApi.initialize();
        InternetCardEnvironment card = new InternetCardEnvironment();
        ExecutorService serverThread = Executors.newSingleThreadExecutor();

        try (ServerSocket server = new ServerSocket(0)) {
            serverThread.submit(() -> {
                try (Socket socket = server.accept()) {
                    byte[] request = socket.getInputStream().readNBytes(4);
                    if (!Arrays.equals("ping".getBytes(StandardCharsets.UTF_8), request)) {
                        throw new AssertionError("unexpected request data");
                    }
                    socket.getOutputStream().write("pong".getBytes(StandardCharsets.UTF_8));
                    socket.getOutputStream().flush();
                }
                return null;
            });

            assertArrayEquals(new Object[]{true}, card.isTcpEnabled(null, new TestArguments()));
            Object handle = card.connect(null, new TestArguments("127.0.0.1", server.getLocalPort()))[0];
            InternetCardEnvironment.TcpSocket socket = assertInstanceOf(InternetCardEnvironment.TcpSocket.class, handle);

            awaitConnected(socket);
            assertArrayEquals(new Object[]{4}, socket.write(null, new TestArguments("ping".getBytes(StandardCharsets.UTF_8))));
            assertArrayEquals("pong".getBytes(StandardCharsets.UTF_8), awaitRead(socket, 4));
            socket.close(null, new TestArguments());
        } finally {
            serverThread.shutdownNow();
            assertTrue(serverThread.awaitTermination(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void tcpSocketReadUsesConfiguredMaxReadBuffer() throws Exception {
        OpenComputersApi.initialize();
        InternetCardEnvironment card = new InternetCardEnvironment();
        ExecutorService serverThread = Executors.newSingleThreadExecutor();

        try (ServerSocket server = new ServerSocket(0)) {
            serverThread.submit(() -> {
                try (Socket socket = server.accept()) {
                    socket.getOutputStream().write("abcdef".getBytes(StandardCharsets.UTF_8));
                    socket.getOutputStream().flush();
                    Thread.sleep(200);
                }
                return null;
            });

            InternetCardEnvironment.TcpSocket socket = assertInstanceOf(
                InternetCardEnvironment.TcpSocket.class,
                card.connect(null, new TestArguments("127.0.0.1", server.getLocalPort()))[0]);
            awaitConnected(socket);

            withCachedConfig(ModSettings.MAX_READ_BUFFER, 3, () ->
                assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), awaitRead(socket, 10)));
            socket.close(null, new TestArguments());
        } finally {
            serverThread.shutdownNow();
            assertTrue(serverThread.awaitTermination(2, TimeUnit.SECONDS));
        }
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = InternetCardEnvironment.class.getMethod(methodName, li.cil.oc.api.machine.Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
    }

    private static void awaitConnected(final InternetCardEnvironment.TcpSocket socket) throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            if (Boolean.TRUE.equals(socket.finishConnect(null, new TestArguments())[0])) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("socket did not connect");
    }

    private static void awaitHttpConnected(final InternetCardEnvironment.HttpRequest request) throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            if (Boolean.TRUE.equals(request.finishConnect(null, new TestArguments())[0])) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("HTTP request did not connect");
    }

    private static byte[] awaitRead(final InternetCardEnvironment.TcpSocket socket, final int length) throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            Object value = socket.read(null, new TestArguments(length))[0];
            if (value instanceof byte[] bytes && bytes.length > 0) {
                return bytes;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("socket did not read data");
    }

    private static <T> void withCachedConfig(final ModConfigSpec.ConfigValue<T> value, final T override, final ThrowingRunnable action) throws Exception {
        final Field cachedValue = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        cachedValue.setAccessible(true);
        final Object previous = cachedValue.get(value);
        cachedValue.set(value, override);
        try {
            action.run();
        } finally {
            cachedValue.set(value, previous);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
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

    private static final class TestComputerContext implements Context, Environment {
        private final Node node;

        private TestComputerContext() {
            node = Network.newNode(this, Visibility.Network).create();
        }

        @Override public Node node() { return node; }
        @Override public boolean canInteract(final String player) { return true; }
        @Override public boolean isRunning() { return true; }
        @Override public boolean isPaused() { return false; }
        @Override public boolean start() { return true; }
        @Override public boolean pause(final double seconds) { return true; }
        @Override public boolean stop() { return true; }
        @Override public void consumeCallBudget(final double callCost) { }
        @Override public boolean signal(final String name, final Object... args) { return true; }
        @Override public void onConnect(final Node node) { }
        @Override public void onDisconnect(final Node node) { }
        @Override public void onMessage(final Message message) { }
    }
}
