package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Value;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ModSettings;
import net.minecraft.nbt.CompoundTag;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InternetCardEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "internet";
    private static final ExecutorService HTTP_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        final Thread thread = new Thread(runnable, "NeoOpenComputers Internet");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Communication,
        DeviceInfo.DeviceAttribute.Description, "Internet modem",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "SuperLink X-D4NK"
    );

    private final HttpTransport transport;
    private final Set<Object> connections = Collections.newSetFromMap(new IdentityHashMap<>());
    private Context owner;

    public InternetCardEnvironment() {
        this(InternetCardEnvironment::openUrl);
    }

    public InternetCardEnvironment(final HttpTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME, Visibility.Neighbors).create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether HTTP requests can be made.")
    public Object[] isHttpEnabled(final Context context, final Arguments args) {
        return new Object[]{ModSettings.enableHttp()};
    }

    @Callback(doc = "function(url:string[, postData:string[, headers:table[, method:string]]]):userdata -- Starts an HTTP request.")
    public synchronized Object[] request(final Context context, final Arguments args) throws IOException {
        checkOwner(context);
        if (!ModSettings.enableHttp()) {
            return new Object[]{null, "http requests are unavailable"};
        }
        final String url = checkHttpUrl(args.checkString(0));
        final byte[] postData = args.count() > 1 && args.checkAny(1) != null ? args.checkByteArray(1) : null;
        final Map<String, String> headers = args.isTable(2) ? headers(args.checkTable(2)) : Map.of();
        final String method = args.count() > 3 && args.checkAny(3) != null ? args.checkString(3) : (postData == null ? "GET" : "POST");
        ensureConnectionSlot();
        final HttpRequest request = new HttpRequest(transport.request(url, postData, headers, method), this);
        connections.add(request);
        return new Object[]{request};
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether TCP connections can be made.")
    public Object[] isTcpEnabled(final Context context, final Arguments args) {
        return new Object[]{ModSettings.enableTcp()};
    }

    @Callback(doc = "function(address:string[, port:number]):userdata -- Opens a new TCP connection.")
    public synchronized Object[] connect(final Context context, final Arguments args) throws IOException {
        checkOwner(context);
        if (!ModSettings.enableTcp()) {
            return new Object[]{null, "tcp connections are unavailable"};
        }
        final TcpAddress address = checkTcpAddress(args.checkString(0), args.optInteger(1, -1));
        ensureConnectionSlot();
        final TcpSocket socket = new TcpSocket(address.host(), address.port(), this);
        connections.add(socket);
        return new Object[]{socket};
    }

    private void ensureConnectionSlot() throws IOException {
        if (connections.size() >= ModSettings.maxTcpConnections()) {
            throw new IOException("too many open connections");
        }
    }

    private synchronized void unregisterConnection(final Object connection) {
        connections.remove(connection);
    }

    private void checkOwner(final Context context) {
        if (owner == null && context != null) {
            owner = context;
        }
        if (owner != null && context != null && context.node() != owner.node()) {
            throw new IllegalArgumentException("can only be used by the owning computer");
        }
    }

    @Override
    public synchronized void onConnect(final Node node) {
        super.onConnect(node);
        if (owner == null && node.host() instanceof Context context) {
            owner = context;
        }
    }

    @Override
    public synchronized void onDisconnect(final Node node) {
        super.onDisconnect(node);
        if (node == node() || owner != null && node.host() == owner) {
            owner = null;
            closeConnections();
        }
    }

    @Override
    public synchronized void onMessage(final Message message) {
        super.onMessage(message);
        if (owner != null && message.source() == owner.node() && ("computer.stopped".equals(message.name()) || "computer.started".equals(message.name()))) {
            closeConnections();
        }
    }

    private void closeConnections() {
        final Object[] handles = connections.toArray();
        for (final Object handle : handles) {
            if (handle instanceof HttpRequest request) {
                request.close();
            } else if (handle instanceof TcpSocket socket) {
                socket.close();
            }
        }
        connections.clear();
    }

    private static String checkHttpUrl(final String address) {
        try {
            final URL url = new URL(address);
            final String protocol = url.getProtocol();
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                throw new IllegalArgumentException("unsupported protocol");
            }
            return address;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid address", e);
        }
    }

    private static Map<String, String> headers(final Map<?, ?> table) {
        final Map<String, String> headers = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : table.entrySet()) {
            if (entry.getKey() instanceof String key && entry.getValue() != null) {
                headers.put(key, entry.getValue().toString());
            }
        }
        return headers;
    }

    private static TcpAddress checkTcpAddress(final String address, final int port) {
        try {
            final URI parsed = new URI(address);
            if (parsed.getHost() != null && (parsed.getPort() > 0 || port > 0)) {
                return new TcpAddress(parsed.getHost(), parsed.getPort() > 0 ? parsed.getPort() : port);
            }
        } catch (Exception ignored) {
        }

        try {
            final URI simple = new URI("oc://" + address);
            if (simple.getHost() != null && (simple.getPort() > 0 || port > 0)) {
                return new TcpAddress(simple.getHost(), simple.getPort() > 0 ? simple.getPort() : port);
            }
        } catch (Exception ignored) {
        }

        throw new IllegalArgumentException("address could not be parsed or no valid port given");
    }

    private static CompletableFuture<HttpResponse> openUrl(
        final String url,
        final byte[] postData,
        final Map<String, String> headers,
        final String method
    ) {
        return CompletableFuture.supplyAsync(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setDoInput(true);
                connection.setConnectTimeout(ModSettings.httpRequestTimeout());
                connection.setReadTimeout(ModSettings.httpRequestTimeout());
                connection.setRequestMethod(method);
                connection.setRequestProperty("User-Agent", ModSettings.httpUserAgent());
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
                if (postData != null) {
                    connection.setDoOutput(true);
                    try (OutputStream out = connection.getOutputStream()) {
                        out.write(postData);
                    }
                }
                final int code = connection.getResponseCode();
                final String message = connection.getResponseMessage();
                try (InputStream in = inputStream(connection)) {
                    return new HttpResponse(code, message, responseHeaders(connection.getHeaderFields()), in.readAllBytes());
                }
            } catch (IOException e) {
                throw new CompletionException(e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }, HTTP_EXECUTOR);
    }

    private static InputStream inputStream(final HttpURLConnection connection) throws IOException {
        final InputStream error = connection.getErrorStream();
        return error != null ? error : connection.getInputStream();
    }

    private static Map<String, List<String>> responseHeaders(final Map<String, List<String>> headers) {
        final Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() != null) {
                result.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
        }
        return result;
    }

    @FunctionalInterface
    public interface HttpTransport {
        CompletableFuture<HttpResponse> request(String url, byte[] postData, Map<String, String> headers, String method);
    }

    public record HttpResponse(int code, String message, Map<String, List<String>> headers, byte[] body) {
    }

    private record TcpAddress(String host, int port) {
    }

    public static final class HttpRequest implements Value {
        private final CompletableFuture<HttpResponse> response;
        private final InternetCardEnvironment owner;
        private int offset;

        private HttpRequest(final CompletableFuture<HttpResponse> response, final InternetCardEnvironment owner) {
            this.response = response;
            this.owner = owner;
        }

        @Callback(doc = "function():boolean -- Ensures a response is available.")
        public Object[] finishConnect(final Context context, final Arguments args) {
            return new Object[]{response.isDone() && !response.isCompletedExceptionally()};
        }

        @Callback(direct = true, doc = "function():number, string, table -- Get response code, message and headers.")
        public Object[] response(final Context context, final Arguments args) {
            if (!response.isDone()) {
                return new Object[]{null};
            }
            final HttpResponse value = response.join();
            return new Object[]{value.code(), value.message(), value.headers()};
        }

        @Callback(doc = "function([n:number]):string -- Tries to read data from the response.")
        public Object[] read(final Context context, final Arguments args) {
            if (!response.isDone()) {
                return new Object[]{new byte[0]};
            }
            final byte[] body = response.join().body();
            if (offset >= body.length) {
                return new Object[]{null};
            }
            final int maxReadBuffer = ModSettings.maxReadBuffer();
            final int count = Math.min(Math.max(0, args.optInteger(0, maxReadBuffer)), maxReadBuffer);
            final int length = Math.min(count, body.length - offset);
            final byte[] data = new byte[length];
            System.arraycopy(body, offset, data, 0, length);
            offset += length;
            return new Object[]{data};
        }

        @Callback(direct = true, doc = "function() -- Closes an open HTTP stream.")
        public Object[] close(final Context context, final Arguments args) {
            close();
            return null;
        }

        @Override
        public Object apply(final Context context, final Arguments arguments) {
            return this;
        }

        @Override
        public void unapply(final Context context, final Arguments arguments) {
        }

        @Override
        public Object[] call(final Context context, final Arguments arguments) {
            return read(context, arguments);
        }

        @Override
        public void dispose(final Context context) {
            close();
        }

        private void close() {
            response.cancel(true);
            owner.unregisterConnection(this);
        }

        @Override
        public void load(final CompoundTag nbt) {
        }

        @Override
        public void save(final CompoundTag nbt) {
        }
    }

    public static final class TcpSocket implements Value {
        private final UUID id = UUID.randomUUID();
        private final CompletableFuture<Socket> connection;
        private final InternetCardEnvironment owner;

        private TcpSocket(final String host, final int port, final InternetCardEnvironment owner) {
            this.owner = owner;
            connection = CompletableFuture.supplyAsync(() -> {
                try {
                    final Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(host, port), 10_000);
                    socket.setTcpNoDelay(true);
                    return socket;
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }, HTTP_EXECUTOR);
        }

        @Callback(doc = "function():boolean -- Ensures a socket is connected.")
        public Object[] finishConnect(final Context context, final Arguments args) {
            if (!connection.isDone()) {
                return new Object[]{false};
            }
            socket();
            return new Object[]{true};
        }

        @Callback(doc = "function([n:number]):string -- Tries to read data from the socket stream.")
        public Object[] read(final Context context, final Arguments args) throws IOException {
            if (!connection.isDone()) {
                return new Object[]{new byte[0]};
            }
            final Socket socket = socket();
            final InputStream input = socket.getInputStream();
            final int available = input.available();
            if (available <= 0) {
                return socket.isClosed() ? new Object[]{null} : new Object[]{new byte[0]};
            }
            final int maxReadBuffer = ModSettings.maxReadBuffer();
            final int count = Math.min(Math.max(0, args.optInteger(0, maxReadBuffer)), Math.min(maxReadBuffer, available));
            final byte[] data = input.readNBytes(count);
            if (data.length == 0) {
                return new Object[]{null};
            }
            return new Object[]{data};
        }

        @Callback(doc = "function(data:string):number -- Tries to write data to the socket stream.")
        public Object[] write(final Context context, final Arguments args) throws IOException {
            if (!connection.isDone()) {
                return new Object[]{0};
            }
            final byte[] data = args.checkByteArray(0);
            final OutputStream output = socket().getOutputStream();
            output.write(data);
            output.flush();
            return new Object[]{data.length};
        }

        @Callback(direct = true, doc = "function() -- Closes an open socket stream.")
        public Object[] close(final Context context, final Arguments args) {
            close();
            return null;
        }

        @Callback(direct = true, doc = "function():string -- Returns connection ID.")
        public Object[] id(final Context context, final Arguments args) {
            return new Object[]{id.toString()};
        }

        private Socket socket() {
            return connection.join();
        }

        private void close() {
            connection.thenAccept(socket -> {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            });
            connection.cancel(true);
            owner.unregisterConnection(this);
        }

        @Override
        public Object apply(final Context context, final Arguments arguments) {
            return this;
        }

        @Override
        public void unapply(final Context context, final Arguments arguments) {
        }

        @Override
        public Object[] call(final Context context, final Arguments arguments) {
            return new Object[]{this};
        }

        @Override
        public void dispose(final Context context) {
            close();
        }

        @Override
        public void load(final CompoundTag nbt) {
        }

        @Override
        public void save(final CompoundTag nbt) {
        }
    }
}
