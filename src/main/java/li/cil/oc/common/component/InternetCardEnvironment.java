package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Value;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.nbt.CompoundTag;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InternetCardEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "internet";
    private static final int MAX_READ_BUFFER = 8192;
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
        return new Object[]{true};
    }

    @Callback(doc = "function(url:string[, postData:string[, headers:table[, method:string]]]):userdata -- Starts an HTTP request.")
    public Object[] request(final Context context, final Arguments args) {
        final String url = checkHttpUrl(args.checkString(0));
        final byte[] postData = args.count() > 1 && args.checkAny(1) != null ? args.checkByteArray(1) : null;
        final Map<String, String> headers = args.isTable(2) ? headers(args.checkTable(2)) : Map.of();
        final String method = args.count() > 3 && args.checkAny(3) != null ? args.checkString(3) : (postData == null ? "GET" : "POST");
        return new Object[]{new HttpRequest(transport.request(url, postData, headers, method))};
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether TCP connections can be made.")
    public Object[] isTcpEnabled(final Context context, final Arguments args) {
        return new Object[]{false};
    }

    @Callback(doc = "function(address:string[, port:number]):userdata -- Opens a new TCP connection.")
    public Object[] connect(final Context context, final Arguments args) {
        return new Object[]{null, "tcp connections are unavailable"};
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
                connection.setConnectTimeout(10_000);
                connection.setReadTimeout(10_000);
                connection.setRequestMethod(method);
                connection.setRequestProperty("User-Agent", "NeoOpenComputers/1.0");
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

    public static final class HttpRequest implements Value {
        private final CompletableFuture<HttpResponse> response;
        private int offset;

        private HttpRequest(final CompletableFuture<HttpResponse> response) {
            this.response = response;
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
            final int count = Math.min(Math.max(0, args.optInteger(0, MAX_READ_BUFFER)), MAX_READ_BUFFER);
            final int length = Math.min(count, body.length - offset);
            final byte[] data = new byte[length];
            System.arraycopy(body, offset, data, 0, length);
            offset += length;
            return new Object[]{data};
        }

        @Callback(direct = true, doc = "function() -- Closes an open HTTP stream.")
        public Object[] close(final Context context, final Arguments args) {
            response.cancel(true);
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
            response.cancel(true);
        }

        @Override
        public void load(final CompoundTag nbt) {
        }

        @Override
        public void save(final CompoundTag nbt) {
        }
    }
}
