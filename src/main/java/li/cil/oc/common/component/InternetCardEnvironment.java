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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class InternetCardEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "internet";
    private static final ScheduledExecutorService HTTP_EXECUTOR = Executors.newScheduledThreadPool(ModSettings.internetThreads(), runnable -> {
        final Thread thread = new Thread(runnable, "NeoOpenComputers Internet");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Communication,
        DeviceInfo.DeviceAttribute.Description, "Internet modem",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
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
        if (!internetAccessAllowed()) {
            return new Object[]{null, "internet access is unavailable"};
        }
        if (!ModSettings.enableHttp()) {
            return new Object[]{null, "http requests are unavailable"};
        }
        final String url = checkHttpUrl(args.checkString(0));
        final byte[] postData = args.isString(1) || args.isByteArray(1) ? args.checkByteArray(1) : null;
        final Map<String, String> headers = args.isTable(2) ? headers(args.checkTable(2)) : Map.of();
        if (!ModSettings.enableHttpHeaders() && !headers.isEmpty()) {
            return new Object[]{null, "http request headers are unavailable"};
        }
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
        if (!internetAccessAllowed()) {
            return new Object[]{null, "internet access is unavailable"};
        }
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
        if (context != null && (owner == null || context.node() != owner.node())) {
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

    private void emitInternetReady(final UUID id) {
        if (node() != null) {
            node().sendToReachable("computer.signal", "internet_ready", id.toString());
        }
    }

    private static String checkHttpUrl(final String address) throws FileNotFoundException {
        try {
            final URL url = new URL(address);
            final String protocol = url.getProtocol();
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                throw new FileNotFoundException("unsupported protocol");
            }
            return address;
        } catch (FileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new FileNotFoundException("invalid address");
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
                final URL parsed = new URL(url);
                checkAddressAllowed(InetAddress.getByName(parsed.getHost()), parsed.getHost());
                connection = (HttpURLConnection) parsed.openConnection();
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
                final Map<String, List<String>> responseHeaders = responseHeaders(connection.getHeaderFields());
                try (InputStream in = connection.getInputStream()) {
                    return new HttpResponse(code, message, responseHeaders, in.readAllBytes());
                } catch (IOException e) {
                    return new HttpResponse(code, message, responseHeaders, new byte[0], e);
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

    private static Map<String, List<String>> responseHeaders(final Map<String, List<String>> headers) {
        final Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() != null) {
                result.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
        }
        return result;
    }

    private static void checkAddressAllowed(final InetAddress address, final String host) throws IOException {
        if (!isAddressAllowed(address, host)) {
            throw new IOException("address is not allowed");
        }
    }

    private static boolean internetAccessAllowed() {
        return (ModSettings.enableHttp() || ModSettings.enableTcp()) && internetFilteringRulesValid();
    }

    private static boolean internetFilteringRulesValid() {
        try {
            for (final String rule : ModSettings.internetFilteringRules()) {
                InternetFilteringRule.parse(rule);
            }
            return true;
        } catch (final IllegalArgumentException ignored) {
            return false;
        }
    }

    private static boolean isAddressAllowed(final InetAddress address, final String host) {
        if (!internetAccessAllowed()) {
            return false;
        }
        try {
            final InetAddress embeddedIpv4 = InternetFilteringRule.embeddedIpv4ClientAddress(address);
            if (embeddedIpv4 != null && Boolean.FALSE.equals(firstMatchingFilteringRule(embeddedIpv4, host))) {
                return false;
            }
            return Boolean.TRUE.equals(firstMatchingFilteringRule(address, host));
        } catch (final IllegalArgumentException ignored) {
            return false;
        }
    }

    private static Boolean firstMatchingFilteringRule(final InetAddress address, final String host) {
        for (final String rule : ModSettings.internetFilteringRules()) {
            final Boolean result = InternetFilteringRule.parse(rule).apply(address, host);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @FunctionalInterface
    public interface HttpTransport {
        CompletableFuture<HttpResponse> request(String url, byte[] postData, Map<String, String> headers, String method);
    }

    public record HttpResponse(int code, String message, Map<String, List<String>> headers, byte[] body, IOException bodyFailure) {
        public HttpResponse(final int code, final String message, final Map<String, List<String>> headers, final byte[] body) {
            this(code, message, headers, body, null);
        }
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
            if (!response.isDone()) {
                return new Object[]{false};
            }
            throwBodyFailure(response.join());
            return new Object[]{true};
        }

        @Callback(direct = true, doc = "function():number, string, table -- Get response code, message and headers.")
        public Object[] response(final Context context, final Arguments args) {
            if (!response.isDone()) {
                return new Object[]{null};
            }
            final HttpResponse value;
            try {
                value = response.join();
            } catch (CompletionException e) {
                return new Object[]{null};
            }
            return new Object[]{value.code(), value.message(), value.headers()};
        }

        @Callback(doc = "function([n:number]):string -- Tries to read data from the response.")
        public Object[] read(final Context context, final Arguments args) {
            if (!response.isDone()) {
                return new Object[]{new byte[0]};
            }
            final HttpResponse value = response.join();
            throwBodyFailure(value);
            final byte[] body = value.body();
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

        private static void throwBodyFailure(final HttpResponse response) {
            if (response.bodyFailure() != null) {
                throw new CompletionException(response.bodyFailure());
            }
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
        private volatile ScheduledFuture<?> readinessTask;
        private volatile boolean readinessArmed = true;

        private TcpSocket(final String host, final int port, final InternetCardEnvironment owner) {
            this.owner = owner;
            connection = CompletableFuture.supplyAsync(() -> {
                try {
                    final InetAddress resolved = InetAddress.getByName(host);
                    checkAddressAllowed(resolved, host);
                    final Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(resolved, port), 10_000);
                    socket.setTcpNoDelay(true);
                    return socket;
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }, HTTP_EXECUTOR);
            connection.thenRun(this::startReadinessWatcher);
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
            readinessArmed = true;
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

        private void startReadinessWatcher() {
            readinessTask = HTTP_EXECUTOR.scheduleWithFixedDelay(() -> {
                try {
                    final Socket socket = socket();
                    if (socket.isClosed()) {
                        cancelReadinessWatcher();
                        return;
                    }
                    if (readinessArmed && socket.getInputStream().available() > 0) {
                        readinessArmed = false;
                        owner.emitInternetReady(id);
                    }
                } catch (IOException | RuntimeException ignored) {
                    cancelReadinessWatcher();
                }
            }, 0, 50, TimeUnit.MILLISECONDS);
        }

        private void cancelReadinessWatcher() {
            final ScheduledFuture<?> task = readinessTask;
            if (task != null) {
                task.cancel(false);
                readinessTask = null;
            }
        }

        private void close() {
            cancelReadinessWatcher();
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

    private record InternetFilteringRule(boolean allow, List<RulePredicate> predicates) {
        private static final List<String> BOGON_RANGES = List.of(
            "0.0.0.0/8",
            "10.0.0.0/8",
            "100.64.0.0/10",
            "127.0.0.0/8",
            "169.254.0.0/16",
            "172.16.0.0/12",
            "192.0.0.0/24",
            "192.0.2.0/24",
            "192.168.0.0/16",
            "198.18.0.0/15",
            "198.51.100.0/24",
            "203.0.113.0/24",
            "224.0.0.0/3",
            "::/128",
            "::1/128",
            "::ffff:0:0/96",
            "::/96",
            "100::/64",
            "2001:10::/28",
            "2001:db8::/32",
            "fc00::/7",
            "fe80::/10",
            "fec0::/10",
            "ff00::/8"
        );
        private static final List<InetAddressRange> BOGONS = BOGON_RANGES.stream()
            .map(range -> range.split("/", 2))
            .map(parts -> InetAddressRange.parse(parts[0], Integer.parseInt(parts[1])))
            .toList();

        private Boolean apply(final InetAddress address, final String host) {
            for (final RulePredicate predicate : predicates) {
                if (!predicate.matches(address, host)) {
                    return null;
                }
            }
            return allow;
        }

        private static InternetFilteringRule parse(final String rule) {
            final String[] parts = rule.trim().split("\\s+");
            if (parts.length == 0 || parts[0].isEmpty() || "removeme".equals(parts[0])) {
                return new InternetFilteringRule(false, List.of((address, host) -> false));
            }
            if (!"allow".equals(parts[0]) && !"deny".equals(parts[0])) {
                throw new IllegalArgumentException("invalid filtering rule action");
            }
            final boolean allow = "allow".equals(parts[0]);
            final List<RulePredicate> predicates = new java.util.ArrayList<>();
            for (int i = 1; i < parts.length; i++) {
                final String filter = parts[i];
                if ("all".equals(filter)) {
                    continue;
                }
                if ("default".equals(filter)) {
                    predicates.add((address, host) -> allow && defaultAllow(address, host));
                } else if ("private".equals(filter)) {
                    predicates.add((address, host) -> isPrivate(address));
                } else if ("bogon".equals(filter)) {
                    predicates.add((address, host) -> BOGONS.stream().anyMatch(range -> range.matches(address)));
                } else if ("ipv4".equals(filter)) {
                    predicates.add((address, host) -> address instanceof Inet4Address);
                } else if ("ipv6".equals(filter)) {
                    predicates.add((address, host) -> address instanceof Inet6Address);
                } else if ("ipv4-embedded-ipv6".equals(filter)) {
                    predicates.add((address, host) -> hasEmbeddedIpv4ClientAddress(address));
                } else if (filter.startsWith("ip:")) {
                    final String value = filter.substring("ip:".length());
                    final String[] range = value.split("/", 2);
                    if (range.length == 2) {
                        final InetAddressRange addressRange = InetAddressRange.parse(range[0], Integer.parseInt(range[1]));
                        predicates.add((address, host) -> addressRange.matches(address));
                    } else {
                        final InetAddress exact = parseAddress(value);
                        predicates.add((address, host) -> exact.equals(address));
                    }
                } else if (filter.startsWith("domain:")) {
                    final String domain = filter.substring("domain:".length());
                    final List<InetAddress> addresses = List.of(resolveAll(domain));
                    predicates.add((address, host) -> host.equals(domain) || addresses.contains(address));
                } else {
                    throw new IllegalArgumentException("invalid filtering rule filter");
                }
            }
            return new InternetFilteringRule(allow, predicates);
        }

        private static boolean defaultAllow(final InetAddress address, final String host) {
            return !isPrivate(address) && BOGONS.stream().noneMatch(range -> range.matches(address));
        }

        private static boolean isPrivate(final InetAddress address) {
            return address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress();
        }

        private static boolean hasEmbeddedIpv4ClientAddress(final InetAddress address) {
            return embeddedIpv4ClientAddress(address) != null;
        }

        private static InetAddress embeddedIpv4ClientAddress(final InetAddress address) {
            if (!(address instanceof Inet6Address)) {
                return null;
            }
            final byte[] bytes = address.getAddress();
            final byte[] embedded = new byte[4];
            if (isIpv4CompatibleAddress(bytes)) {
                System.arraycopy(bytes, 12, embedded, 0, embedded.length);
            } else if (is6to4Address(bytes)) {
                System.arraycopy(bytes, 2, embedded, 0, embedded.length);
            } else if (isTeredoAddress(bytes)) {
                for (int i = 0; i < embedded.length; i++) {
                    embedded[i] = (byte) ~bytes[12 + i];
                }
            } else {
                return null;
            }
            try {
                return InetAddress.getByAddress(embedded);
            } catch (final IOException e) {
                throw new IllegalArgumentException("invalid embedded IPv4 address", e);
            }
        }

        private static boolean isIpv4CompatibleAddress(final byte[] bytes) {
            for (int i = 0; i < 12; i++) {
                if (bytes[i] != 0) {
                    return false;
                }
            }
            for (int i = 12; i < 15; i++) {
                if (bytes[i] != 0) {
                    return true;
                }
            }
            return bytes[15] != 0 && bytes[15] != 1;
        }

        private static boolean is6to4Address(final byte[] bytes) {
            return Byte.toUnsignedInt(bytes[0]) == 0x20 && Byte.toUnsignedInt(bytes[1]) == 0x02;
        }

        private static boolean isTeredoAddress(final byte[] bytes) {
            return Byte.toUnsignedInt(bytes[0]) == 0x20 && Byte.toUnsignedInt(bytes[1]) == 0x01 && bytes[2] == 0 && bytes[3] == 0;
        }

        private static InetAddress parseAddress(final String value) {
            try {
                return InetAddress.getByName(value);
            } catch (final IOException e) {
                throw new IllegalArgumentException("invalid IP address", e);
            }
        }

        private static InetAddress[] resolveAll(final String domain) {
            try {
                return InetAddress.getAllByName(domain);
            } catch (final IOException e) {
                throw new IllegalArgumentException("invalid domain", e);
            }
        }
    }

    @FunctionalInterface
    private interface RulePredicate {
        boolean matches(InetAddress address, String host);
    }

    private record InetAddressRange(byte[] min, byte[] max) {
        private boolean matches(final InetAddress address) {
            final byte[] value = address.getAddress();
            if (value.length != min.length) {
                return false;
            }
            for (int i = 0; i < value.length; i++) {
                final int unsigned = Byte.toUnsignedInt(value[i]);
                if (unsigned < Byte.toUnsignedInt(min[i]) || unsigned > Byte.toUnsignedInt(max[i])) {
                    return false;
                }
            }
            return true;
        }

        private static InetAddressRange parse(final String address, final int prefixSize) {
            final byte[] min = InternetFilteringRule.parseAddress(address).getAddress();
            final byte[] max = min.clone();
            int remaining = prefixSize;
            for (int i = 0; i < min.length; i++) {
                if (remaining <= 0) {
                    min[i] = 0;
                    max[i] = (byte) 0xFF;
                } else if (remaining < 8) {
                    final int mask = 0xFF << (8 - remaining);
                    min[i] = (byte) (min[i] & mask);
                    max[i] = (byte) (Byte.toUnsignedInt(max[i]) | ~mask);
                }
                remaining -= 8;
            }
            return new InetAddressRange(min, max);
        }
    }
}
