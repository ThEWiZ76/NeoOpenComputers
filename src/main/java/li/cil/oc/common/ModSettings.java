package li.cil.oc.common;

import li.cil.oc.api.API;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class ModSettings {
    private static final List<Integer> DEFAULT_HDD_SIZES = List.of(1024, 2048, 4096);
    private static final List<Integer> DEFAULT_MAX_OPEN_PORTS = List.of(16, 1, 16);
    private static final List<Double> DEFAULT_MAX_WIRELESS_RANGE = List.of(16D, 400D);
    private static final List<Double> DEFAULT_WIRELESS_COST_PER_RANGE = List.of(0.05D, 0.05D);
    private static final List<String> DEFAULT_FILTERING_RULES = List.of("removeme", "deny private", "deny bogon", "allow default");

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue MFU_RANGE;
    public static final ModConfigSpec.BooleanValue INPUT_USERNAME;
    public static final ModConfigSpec.BooleanValue CAN_COMPUTERS_BE_OWNED;
    public static final ModConfigSpec.IntValue MAX_USERS;
    public static final ModConfigSpec.IntValue MAX_USERNAME_LENGTH;
    public static final ModConfigSpec.DoubleValue COMPUTER_TIMEOUT;
    public static final ModConfigSpec.BooleanValue ALLOW_BYTECODE;
    public static final ModConfigSpec.BooleanValue ALLOW_GC;
    public static final ModConfigSpec.IntValue INITIAL_NETWORK_PACKET_TTL;
    public static final ModConfigSpec.IntValue MAX_NETWORK_PACKET_SIZE;
    public static final ModConfigSpec.IntValue MAX_NETWORK_PACKET_PARTS;
    public static final ModConfigSpec.IntValue DEFAULT_RELAY_DELAY;
    public static final ModConfigSpec.DoubleValue RELAY_DELAY_UPGRADE;
    public static final ModConfigSpec.IntValue DEFAULT_MAX_QUEUE_SIZE;
    public static final ModConfigSpec.IntValue QUEUE_SIZE_UPGRADE;
    public static final ModConfigSpec.IntValue DEFAULT_RELAY_AMOUNT;
    public static final ModConfigSpec.IntValue RELAY_AMOUNT_UPGRADE;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> MAX_OPEN_PORTS;
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> MAX_WIRELESS_RANGE;
    public static final ModConfigSpec.IntValue TMP_SIZE;
    public static final ModConfigSpec.IntValue FILE_COST;
    public static final ModConfigSpec.IntValue FLOPPY_SIZE;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> HDD_SIZES;
    public static final ModConfigSpec.IntValue MAX_HANDLES;
    public static final ModConfigSpec.IntValue MAX_READ_BUFFER;
    public static final ModConfigSpec.BooleanValue ENABLE_HTTP;
    public static final ModConfigSpec.BooleanValue ENABLE_HTTP_HEADERS;
    public static final ModConfigSpec.BooleanValue ENABLE_TCP;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FILTERING_RULES;
    public static final ModConfigSpec.IntValue REQUEST_TIMEOUT;
    public static final ModConfigSpec.IntValue INTERNET_THREADS;
    public static final ModConfigSpec.IntValue MAX_TCP_CONNECTIONS;
    public static final ModConfigSpec.ConfigValue<String> HTTP_USER_AGENT;
    public static final ModConfigSpec.DoubleValue MFU_RELAY_COST;
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> WIRELESS_COST_PER_RANGE;
    public static final ModConfigSpec.IntValue MFU_TICK_FREQUENCY;
    public static final ModConfigSpec.DoubleValue SOLAR_GENERATOR_EFFICIENCY;

    static {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("misc");
        MFU_RANGE = builder
            .comment("Radius the MFU is able to operate in.")
            .defineInRange("mfuRange", 3D, 0D, 128D);
        INPUT_USERNAME = builder
            .comment("Include player and entity names in input-related signals. OpenComputers upstream default is true.")
            .define("inputUsername", true);
        INITIAL_NETWORK_PACKET_TTL = builder
            .comment("Initial network packet TTL. OpenComputers upstream default and minimum is 5.")
            .defineInRange("initialNetworkPacketTTL", 5, 5, Integer.MAX_VALUE);
        MAX_NETWORK_PACKET_SIZE = builder
            .comment("Maximum network packet size in bytes. OpenComputers upstream default is 8192.")
            .defineInRange("maxNetworkPacketSize", 8192, 0, Integer.MAX_VALUE);
        MAX_NETWORK_PACKET_PARTS = builder
            .comment("Maximum number of data parts in one network packet. OpenComputers upstream default is 8 and minimum is 4.")
            .defineInRange("maxNetworkPacketParts", 8, 4, Integer.MAX_VALUE);
        DEFAULT_RELAY_DELAY = builder
            .comment("Base delay in ticks before a relay forwards queued packets. OpenComputers upstream default is 5.")
            .defineInRange("defaultRelayDelay", 5, 1, Integer.MAX_VALUE);
        RELAY_DELAY_UPGRADE = builder
            .comment("Delay reduction per CPU tier installed in a relay. OpenComputers upstream default is 1.5.")
            .defineInRange("relayDelayUpgrade", 1.5D, 0D, Double.MAX_VALUE);
        DEFAULT_MAX_QUEUE_SIZE = builder
            .comment("Base maximum queued packet count for relays. OpenComputers upstream default is 20.")
            .defineInRange("defaultMaxQueueSize", 20, 1, Integer.MAX_VALUE);
        QUEUE_SIZE_UPGRADE = builder
            .comment("Additional relay queue slots per hard-drive tier. OpenComputers upstream default is 10.")
            .defineInRange("queueSizeUpgrade", 10, 0, Integer.MAX_VALUE);
        DEFAULT_RELAY_AMOUNT = builder
            .comment("Base number of packets relayed per cycle. OpenComputers upstream default is 1.")
            .defineInRange("defaultRelayAmount", 1, 1, Integer.MAX_VALUE);
        RELAY_AMOUNT_UPGRADE = builder
            .comment("Additional packets relayed per memory tier. OpenComputers upstream default is 1.")
            .defineInRange("relayAmountUpgrade", 1, 0, Integer.MAX_VALUE);
        MAX_OPEN_PORTS = builder
            .comment("Maximum open ports for wired, tier-one wireless, and tier-two wireless cards. OpenComputers upstream default is [16, 1, 16].")
            .defineList("maxOpenPorts", DEFAULT_MAX_OPEN_PORTS, value -> value instanceof Integer && (Integer) value >= 0);
        MAX_WIRELESS_RANGE = builder
            .comment("Maximum wireless card ranges for tier one and tier two. OpenComputers upstream default is [16.0, 400.0].")
            .defineList("maxWirelessRange", DEFAULT_MAX_WIRELESS_RANGE, value -> value instanceof Double && (Double) value >= 0D);
        builder.pop();

        builder.push("computer");
        CAN_COMPUTERS_BE_OWNED = builder
            .comment("Allow computer user lists to restrict interaction. OpenComputers upstream default is true.")
            .define("canComputersBeOwned", true);
        MAX_USERS = builder
            .comment("Maximum number of users registered with one computer. OpenComputers upstream default is 16.")
            .defineInRange("maxUsers", 16, 0, Integer.MAX_VALUE);
        MAX_USERNAME_LENGTH = builder
            .comment("Maximum registered computer username length. OpenComputers upstream default is 32.")
            .defineInRange("maxUsernameLength", 32, 0, Integer.MAX_VALUE);
        COMPUTER_TIMEOUT = builder
            .comment("Seconds programs may run without yielding before they are stopped. OpenComputers upstream default is 5.0.")
            .defineInRange("timeout", 5D, 0D, Double.MAX_VALUE);
        builder.push("lua");
        ALLOW_BYTECODE = builder
            .comment("Allow loading Lua bytecode directly. OpenComputers upstream default is false.")
            .define("allowBytecode", false);
        ALLOW_GC = builder
            .comment("Allow custom Lua __gc callbacks. OpenComputers upstream default is false.")
            .define("allowGC", false);
        builder.pop();
        builder.pop();

        builder.push("filesystem");
        TMP_SIZE = builder
            .comment("Size of the free /tmp filesystem in kilobytes. OpenComputers upstream default is 64.")
            .defineInRange("tmpSize", 64, 0, Integer.MAX_VALUE);
        FILE_COST = builder
            .comment("Base byte cost charged for each file or directory on limited filesystems. OpenComputers upstream default is 512.")
            .defineInRange("fileCost", 512, 0, Integer.MAX_VALUE);
        FLOPPY_SIZE = builder
            .comment("Size of writable floppy disks in kilobytes. OpenComputers upstream default is 512.")
            .defineInRange("floppySize", 512, 0, Integer.MAX_VALUE);
        HDD_SIZES = builder
            .comment("Sizes of the three hard drive tiers in kilobytes. OpenComputers upstream default is [1024, 2048, 4096].")
            .defineList("hddSizes", DEFAULT_HDD_SIZES, value -> value instanceof Integer && (Integer) value >= 0);
        MAX_HANDLES = builder
            .comment("Maximum number of file handles any single computer may have open per filesystem. OpenComputers upstream default is 16.")
            .defineInRange("maxHandles", 16, 0, Integer.MAX_VALUE);
        MAX_READ_BUFFER = builder
            .comment("Maximum block size read by one filesystem read call. OpenComputers upstream default is 2048.")
            .defineInRange("maxReadBuffer", 2048, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.push("internet");
        ENABLE_HTTP = builder
            .comment("Allow internet cards to make HTTP requests. OpenComputers upstream default is true.")
            .define("enableHttp", true);
        ENABLE_HTTP_HEADERS = builder
            .comment("Allow internet cards to send custom HTTP request headers. OpenComputers upstream default is true.")
            .define("enableHttpHeaders", true);
        ENABLE_TCP = builder
            .comment("Allow internet cards to make TCP connections. OpenComputers upstream default is true.")
            .define("enableTcp", true);
        FILTERING_RULES = builder
            .comment("Internet card address filtering rules. Rules are processed in order; no match denies access.")
            .defineList("filteringRules", DEFAULT_FILTERING_RULES, value -> value instanceof String);
        REQUEST_TIMEOUT = builder
            .comment("HTTP request timeout in seconds. Zero disables timeouts, matching OpenComputers upstream.")
            .defineInRange("requestTimeout", 0, 0, Integer.MAX_VALUE / 1000);
        INTERNET_THREADS = builder
            .comment("Number of background threads for internet-card DNS and HTTP requests. OpenComputers upstream default is 4.")
            .defineInRange("threads", 4, 1, Integer.MAX_VALUE);
        MAX_TCP_CONNECTIONS = builder
            .comment("Maximum open internet-card HTTP/TCP connections. OpenComputers upstream default is 4.")
            .defineInRange("maxTcpConnections", 4, 0, Integer.MAX_VALUE);
        HTTP_USER_AGENT = builder
            .comment("HTTP User-Agent for internet-card requests. $version is replaced with the mod version.")
            .define("httpUserAgent", "opencomputers/$version");
        builder.pop();

        builder.push("power");
        SOLAR_GENERATOR_EFFICIENCY = builder
            .comment("Energy produced per tick by solar generator upgrades. OpenComputers upstream default is 0.2.")
            .defineInRange("solarGeneratorEfficiency", 0.2D, 0D, Double.MAX_VALUE);
        MFU_TICK_FREQUENCY = builder
            .comment("Tick interval for periodic power costs. OpenComputers upstream default is 10.")
            .defineInRange("tickFrequency", 10, 1, Integer.MAX_VALUE);
        builder.push("cost");
        WIRELESS_COST_PER_RANGE = builder
            .comment("Wireless card energy cost per block of signal strength for tier one and tier two. OpenComputers upstream default is [0.05, 0.05].")
            .defineList("wirelessCostPerRange", DEFAULT_WIRELESS_COST_PER_RANGE, value -> value instanceof Double && (Double) value >= 0D);
        MFU_RELAY_COST = builder
            .comment("MFU relay energy cost per block and tick-frequency interval.")
            .defineInRange("mfuRelay", 1D, 0D, Double.MAX_VALUE);
        builder.pop();
        builder.pop();

        SPEC = builder.build();
    }

    private ModSettings() {
    }

    public static double mfuRange() {
        return doubleValue(MFU_RANGE);
    }

    public static double mfuRelayCost() {
        return doubleValue(MFU_RELAY_COST);
    }

    public static int mfuTickFrequency() {
        return intValue(MFU_TICK_FREQUENCY);
    }

    public static double solarGeneratorEfficiency() {
        return doubleValue(SOLAR_GENERATOR_EFFICIENCY);
    }

    public static boolean inputUsername() {
        return booleanValue(INPUT_USERNAME);
    }

    public static boolean canComputersBeOwned() {
        return booleanValue(CAN_COMPUTERS_BE_OWNED);
    }

    public static int maxUsers() {
        return intValue(MAX_USERS);
    }

    public static int maxUsernameLength() {
        return intValue(MAX_USERNAME_LENGTH);
    }

    public static double computerTimeout() {
        return doubleValue(COMPUTER_TIMEOUT);
    }

    public static boolean allowBytecode() {
        return booleanValue(ALLOW_BYTECODE);
    }

    public static boolean allowGc() {
        return booleanValue(ALLOW_GC);
    }

    public static int initialNetworkPacketTtl() {
        return Math.max(5, intValue(INITIAL_NETWORK_PACKET_TTL));
    }

    public static int maxNetworkPacketSize() {
        return intValue(MAX_NETWORK_PACKET_SIZE);
    }

    public static int maxNetworkPacketParts() {
        return Math.max(4, intValue(MAX_NETWORK_PACKET_PARTS));
    }

    public static int defaultRelayDelay() {
        return Math.max(1, intValue(DEFAULT_RELAY_DELAY));
    }

    public static double relayDelayUpgrade() {
        return doubleValue(RELAY_DELAY_UPGRADE);
    }

    public static int defaultMaxQueueSize() {
        return Math.max(1, intValue(DEFAULT_MAX_QUEUE_SIZE));
    }

    public static int queueSizeUpgrade() {
        return intValue(QUEUE_SIZE_UPGRADE);
    }

    public static int defaultRelayAmount() {
        return Math.max(1, intValue(DEFAULT_RELAY_AMOUNT));
    }

    public static int relayAmountUpgrade() {
        return intValue(RELAY_AMOUNT_UPGRADE);
    }

    public static List<Integer> maxOpenPorts() {
        final List<Integer> ports = listValue(MAX_OPEN_PORTS);
        if (ports.size() != DEFAULT_MAX_OPEN_PORTS.size()) {
            return DEFAULT_MAX_OPEN_PORTS;
        }
        return ports;
    }

    public static int maxOpenPorts(final int tier) {
        final List<Integer> ports = maxOpenPorts();
        return ports.get(clampIndex(tier, ports.size()));
    }

    public static List<Double> maxWirelessRange() {
        final List<Double> ranges = doubleListValue(MAX_WIRELESS_RANGE);
        if (ranges.size() != DEFAULT_MAX_WIRELESS_RANGE.size()) {
            return DEFAULT_MAX_WIRELESS_RANGE;
        }
        return ranges;
    }

    public static double maxWirelessRange(final int tier) {
        final List<Double> ranges = maxWirelessRange();
        return ranges.get(clampIndex(tier, ranges.size()));
    }

    public static List<Double> wirelessCostPerRange() {
        final List<Double> costs = doubleListValue(WIRELESS_COST_PER_RANGE);
        if (costs.size() != DEFAULT_WIRELESS_COST_PER_RANGE.size()) {
            return DEFAULT_WIRELESS_COST_PER_RANGE;
        }
        return costs;
    }

    public static double wirelessCostPerRange(final int tier) {
        final List<Double> costs = wirelessCostPerRange();
        return costs.get(clampIndex(tier, costs.size()));
    }

    public static int tmpSize() {
        return intValue(TMP_SIZE);
    }

    public static int fileCost() {
        return intValue(FILE_COST);
    }

    public static int floppySize() {
        return intValue(FLOPPY_SIZE);
    }

    public static int maxHandles() {
        return intValue(MAX_HANDLES);
    }

    public static int maxReadBuffer() {
        return intValue(MAX_READ_BUFFER);
    }

    public static boolean enableHttp() {
        return booleanValue(ENABLE_HTTP);
    }

    public static boolean enableHttpHeaders() {
        return booleanValue(ENABLE_HTTP_HEADERS);
    }

    public static boolean enableTcp() {
        return booleanValue(ENABLE_TCP);
    }

    public static List<String> internetFilteringRules() {
        return stringListValue(FILTERING_RULES);
    }

    public static int httpRequestTimeout() {
        return intValue(REQUEST_TIMEOUT) * 1000;
    }

    public static int internetThreads() {
        return Math.max(1, intValue(INTERNET_THREADS));
    }

    public static int maxTcpConnections() {
        return intValue(MAX_TCP_CONNECTIONS);
    }

    public static String httpUserAgent() {
        return stringValue(HTTP_USER_AGENT).replace("$version", API.VERSION);
    }

    public static List<Integer> hddSizes() {
        final List<Integer> sizes = listValue(HDD_SIZES);
        if (sizes.size() != DEFAULT_HDD_SIZES.size()) {
            return DEFAULT_HDD_SIZES;
        }
        return sizes;
    }

    public static int hddSize(final int tier) {
        final List<Integer> sizes = hddSizes();
        return sizes.get(Math.max(0, Math.min(sizes.size() - 1, tier)));
    }

    private static boolean booleanValue(final ModConfigSpec.BooleanValue value) {
        try {
            return value.get();
        } catch (final IllegalStateException ignored) {
            return value.getDefault();
        }
    }

    private static double doubleValue(final ModConfigSpec.DoubleValue value) {
        try {
            return value.getAsDouble();
        } catch (final IllegalStateException ignored) {
            return value.getDefault();
        }
    }

    private static int intValue(final ModConfigSpec.IntValue value) {
        try {
            return value.getAsInt();
        } catch (final IllegalStateException ignored) {
            return value.getDefault();
        }
    }

    private static List<Integer> listValue(final ModConfigSpec.ConfigValue<List<? extends Integer>> value) {
        try {
            return List.copyOf(value.get());
        } catch (final IllegalStateException ignored) {
            return List.copyOf(value.getDefault());
        }
    }

    private static List<Double> doubleListValue(final ModConfigSpec.ConfigValue<List<? extends Double>> value) {
        try {
            return List.copyOf(value.get());
        } catch (final IllegalStateException ignored) {
            return List.copyOf(value.getDefault());
        }
    }

    private static String stringValue(final ModConfigSpec.ConfigValue<String> value) {
        try {
            return value.get();
        } catch (final IllegalStateException ignored) {
            return value.getDefault();
        }
    }

    private static List<String> stringListValue(final ModConfigSpec.ConfigValue<List<? extends String>> value) {
        try {
            return List.copyOf(value.get());
        } catch (final IllegalStateException ignored) {
            return List.copyOf(value.getDefault());
        }
    }

    private static int clampIndex(final int index, final int size) {
        return Math.max(0, Math.min(size - 1, index));
    }
}
