package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.internal.TextBuffer;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ModSettings {
    private static final List<Integer> DEFAULT_HDD_SIZES = List.of(1024, 2048, 4096);
    private static final List<Integer> DEFAULT_HDD_PLATTER_COUNTS = List.of(2, 4, 8);
    private static final List<Integer> DEFAULT_CPU_COMPONENT_COUNT = List.of(8, 12, 16, 1024);
    private static final List<Integer> DEFAULT_DEVICE_COMPLEXITY_BY_TIER = List.of(12, 24, 32, 9001);
    private static final List<Double> DEFAULT_CALL_BUDGETS = List.of(0.5D, 1.0D, 1.5D);
    private static final List<Double> DEFAULT_BATTERY_UPGRADE_BUFFERS = List.of(10_000D, 15_000D, 20_000D);
    private static final List<Integer> DEFAULT_SCREEN_WIDTHS_BY_TIER = List.of(50, 80, 160);
    private static final List<Integer> DEFAULT_SCREEN_HEIGHTS_BY_TIER = List.of(16, 25, 50);
    private static final List<Integer> DEFAULT_SCREEN_DEPTHS_BY_TIER = List.of(1, 4, 8);
    private static final List<Double> DEFAULT_GPU_VRAM_SIZES = List.of(1D, 2D, 3D);
    private static final List<Integer> DEFAULT_MAX_OPEN_PORTS = List.of(16, 1, 16);
    private static final List<Double> DEFAULT_MAX_WIRELESS_RANGE = List.of(16D, 400D);
    private static final List<Double> DEFAULT_WIRELESS_COST_PER_RANGE = List.of(0.05D, 0.05D);
    private static final List<Double> DEFAULT_HOLOGRAM_MAX_SCALE = List.of(3D, 4D);
    private static final List<Double> DEFAULT_HOLOGRAM_MAX_TRANSLATION = List.of(1D, 2D);
    private static final List<Double> DEFAULT_NANOMACHINE_HUD_POS = List.of(-1D, -1D);
    private static final List<String> DEFAULT_FILTERING_RULES = List.of("removeme", "deny private", "deny bogon", "allow default");
    private static final List<String> DEFAULT_DEBUG_CARD_WHITELIST = List.of();
    private static final List<Object> DEFAULT_NANOMACHINES_POTION_WHITELIST = List.of(
        "speed",
        "haste",
        "strength",
        "jump_boost",
        "resistance",
        "fire_resistance",
        "water_breathing",
        "night_vision",
        "absorption",
        "blindness",
        "nausea",
        "mining_fatigue",
        "instant_damage",
        "hunger",
        "slowness",
        "poison",
        "weakness",
        "wither");

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue MFU_RANGE;
    public static final ModConfigSpec.BooleanValue INPUT_USERNAME;
    public static final ModConfigSpec.BooleanValue CAN_COMPUTERS_BE_OWNED;
    public static final ModConfigSpec.IntValue MAX_USERS;
    public static final ModConfigSpec.IntValue MAX_USERNAME_LENGTH;
    public static final ModConfigSpec.DoubleValue COMPUTER_TIMEOUT;
    public static final ModConfigSpec.DoubleValue STARTUP_DELAY;
    public static final ModConfigSpec.IntValue EEPROM_SIZE;
    public static final ModConfigSpec.IntValue EEPROM_DATA_SIZE;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> CPU_COMPONENT_COUNT;
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> CALL_BUDGETS;
    public static final ModConfigSpec.BooleanValue ERASE_TMP_ON_REBOOT;
    public static final ModConfigSpec.IntValue EXECUTION_DELAY;
    public static final ModConfigSpec.BooleanValue ALLOW_BYTECODE;
    public static final ModConfigSpec.BooleanValue ALLOW_GC;
    public static final ModConfigSpec.IntValue INITIAL_NETWORK_PACKET_TTL;
    public static final ModConfigSpec.IntValue MAX_NETWORK_PACKET_SIZE;
    public static final ModConfigSpec.IntValue MAX_NETWORK_PACKET_PARTS;
    public static final ModConfigSpec.IntValue DATA_CARD_SOFT_LIMIT;
    public static final ModConfigSpec.IntValue DATA_CARD_HARD_LIMIT;
    public static final ModConfigSpec.DoubleValue DATA_CARD_TIMEOUT;
    public static final ModConfigSpec.DoubleValue DISASSEMBLER_BREAK_CHANCE;
    public static final ModConfigSpec.DoubleValue REDSTONE_DELAY;
    public static final ModConfigSpec.IntValue DISK_ACTIVITY_SOUND_DELAY;
    public static final ModConfigSpec.IntValue GEOLYZER_RANGE;
    public static final ModConfigSpec.IntValue TRANSPOSER_FLUID_TRANSFER_RATE;
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
    public static final ModConfigSpec.DoubleValue HDD_READ;
    public static final ModConfigSpec.DoubleValue HDD_WRITE;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> HDD_SIZES;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> HDD_PLATTER_COUNTS;
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
    public static final ModConfigSpec.BooleanValue ENABLE_NANOMACHINE_PFX;
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> NANOMACHINE_HUD_POS;
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> HOLOGRAM_MAX_SCALE;
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> HOLOGRAM_MAX_TRANSLATION;
    public static final ModConfigSpec.DoubleValue HOLOGRAM_SET_RAW_DELAY;
    public static final ModConfigSpec.BooleanValue IGNORE_POWER;
    public static final ModConfigSpec.DoubleValue MFU_RELAY_COST;
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> WIRELESS_COST_PER_RANGE;
    public static final ModConfigSpec.DoubleValue EEPROM_WRITE_COST;
    public static final ModConfigSpec.DoubleValue DATA_CARD_TRIVIAL;
    public static final ModConfigSpec.DoubleValue DATA_CARD_TRIVIAL_BYTE;
    public static final ModConfigSpec.DoubleValue DATA_CARD_SIMPLE;
    public static final ModConfigSpec.DoubleValue DATA_CARD_SIMPLE_BYTE;
    public static final ModConfigSpec.DoubleValue DATA_CARD_COMPLEX;
    public static final ModConfigSpec.DoubleValue DATA_CARD_COMPLEX_BYTE;
    public static final ModConfigSpec.DoubleValue DATA_CARD_ASYMMETRIC;
    public static final ModConfigSpec.DoubleValue SCREEN_COST;
    public static final ModConfigSpec.DoubleValue HOLOGRAM_COST;
    public static final ModConfigSpec.DoubleValue GPU_SET_COST;
    public static final ModConfigSpec.DoubleValue GPU_FILL_COST;
    public static final ModConfigSpec.DoubleValue GPU_CLEAR_COST;
    public static final ModConfigSpec.DoubleValue GPU_COPY_COST;
    public static final ModConfigSpec.DoubleValue GEOLYZER_SCAN_COST;
    public static final ModConfigSpec.DoubleValue TRANSPOSER_COST;
    public static final ModConfigSpec.DoubleValue DISASSEMBLER_ITEM_COST;
    public static final ModConfigSpec.IntValue MFU_TICK_FREQUENCY;
    public static final ModConfigSpec.DoubleValue GENERATOR_EFFICIENCY;
    public static final ModConfigSpec.DoubleValue SOLAR_GENERATOR_EFFICIENCY;
    public static final ModConfigSpec.DoubleValue ASSEMBLER_TICK_AMOUNT;
    public static final ModConfigSpec.DoubleValue DISASSEMBLER_TICK_AMOUNT;
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> BATTERY_UPGRADE_BUFFERS;
    public static final ModConfigSpec.DoubleValue POWER_DISTRIBUTOR_BUFFER;
    public static final ModConfigSpec.DoubleValue TABLET_BUFFER;
    public static final ModConfigSpec.DoubleValue TABLET_ASSEMBLY_BASE_COST;
    public static final ModConfigSpec.DoubleValue TABLET_ASSEMBLY_COMPLEXITY_COST;
    public static final ModConfigSpec.DoubleValue CONVERTER_BUFFER;
    public static final ModConfigSpec.DoubleValue COMPUTER_BUFFER;
    public static final ModConfigSpec.DoubleValue ACCESS_POINT_BUFFER;
    public static final ModConfigSpec.DoubleValue NANOMACHINES_BUFFER;
    public static final ModConfigSpec.DoubleValue NANOMACHINES_INPUT_COST;
    public static final ModConfigSpec.DoubleValue NANOMACHINES_RECONFIGURE_COST;
    public static final ModConfigSpec.DoubleValue NANOMACHINES_TRIGGER_QUOTA;
    public static final ModConfigSpec.DoubleValue NANOMACHINES_CONNECTOR_QUOTA;
    public static final ModConfigSpec.IntValue NANOMACHINE_MAX_INPUTS;
    public static final ModConfigSpec.IntValue NANOMACHINE_MAX_OUTPUTS;
    public static final ModConfigSpec.IntValue NANOMACHINES_SAFE_INPUTS_ACTIVE;
    public static final ModConfigSpec.IntValue NANOMACHINES_MAX_INPUTS_ACTIVE;
    public static final ModConfigSpec.DoubleValue NANOMACHINES_COMMAND_DELAY;
    public static final ModConfigSpec.DoubleValue NANOMACHINES_COMMAND_RANGE;
    public static final ModConfigSpec.DoubleValue NANOMACHINES_HUNGRY_DAMAGE;
    public static final ModConfigSpec.DoubleValue NANOMACHINES_HUNGRY_ENERGY_RESTORED;
    public static final ModConfigSpec.DoubleValue NANOMACHINES_MAGNET_RANGE;
    public static final ModConfigSpec.DoubleValue NANOMACHINES_DISINTEGRATION_RANGE;
    public static final ModConfigSpec.ConfigValue<List<? extends Object>> NANOMACHINES_POTION_WHITELIST;
    public static final ModConfigSpec.DoubleValue EXPERIENCE_BUFFER_PER_LEVEL;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> SCREEN_WIDTHS_BY_TIER;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> SCREEN_HEIGHTS_BY_TIER;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> SCREEN_DEPTHS_BY_TIER;
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> GPU_VRAM_SIZES;
    public static final ModConfigSpec.DoubleValue GPU_BITBLT_COST;
    public static final ModConfigSpec.ConfigValue<String> DEBUG_CARD_ACCESS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DEBUG_CARD_WHITELIST;

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
        DATA_CARD_SOFT_LIMIT = builder
            .comment("Data Card input size in bytes above which operations pause. OpenComputers upstream default is 8192.")
            .defineInRange("dataCardSoftLimit", 8192, 0, Integer.MAX_VALUE);
        DATA_CARD_HARD_LIMIT = builder
            .comment("Maximum Data Card input size in bytes. OpenComputers upstream default is 1048576.")
            .defineInRange("dataCardHardLimit", 1_048_576, 0, Integer.MAX_VALUE);
        DATA_CARD_TIMEOUT = builder
            .comment("Pause in seconds for Data Card operations above the soft limit. OpenComputers upstream default is 1.0.")
            .defineInRange("dataCardTimeout", 1D, 0D, Double.MAX_VALUE);
        DISASSEMBLER_BREAK_CHANCE = builder
            .comment("Chance for each disassembler output item to break. OpenComputers upstream default is 0.05.")
            .defineInRange("disassemblerBreakChance", 0.05D, 0D, 1D);
        REDSTONE_DELAY = builder
            .comment("Seconds redstone output changes pause the calling context. OpenComputers upstream default is 0.1.")
            .defineInRange("redstoneDelay", 0.1D, 0D, Double.MAX_VALUE);
        DISK_ACTIVITY_SOUND_DELAY = builder
            .comment("Minimum milliseconds between filesystem access sound/activity packets per filesystem sound. Negative disables activity packets. OpenComputers upstream default is 500.")
            .defineInRange("diskActivitySoundDelay", 500, -1, Integer.MAX_VALUE);
        GEOLYZER_RANGE = builder
            .comment("Maximum absolute Geolyzer scan offset. OpenComputers upstream default is 32.")
            .defineInRange("geolyzerRange", 32, 0, Integer.MAX_VALUE);
        TRANSPOSER_FLUID_TRANSFER_RATE = builder
            .comment("Transposer fluid transfer rate in millibuckets per second. OpenComputers upstream default is 4000.")
            .defineInRange("transposerFluidTransferRate", 4000, 1, Integer.MAX_VALUE);
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

        builder.push("client");
        ENABLE_NANOMACHINE_PFX = builder
            .comment("Emit nanomachine particle effects around players. OpenComputers upstream default is true.")
            .define("enableNanomachinePfx", true);
        NANOMACHINE_HUD_POS = builder
            .comment("Position of the nanomachines power HUD indicator. OpenComputers upstream default is [-1, -1].")
            .defineList("nanomachineHudPos", DEFAULT_NANOMACHINE_HUD_POS, value -> value instanceof Double);
        builder.pop();

        builder.push("debug");
        DEBUG_CARD_ACCESS = builder
            .comment("Debug card access mode. Allowed values: allow, deny, whitelist. OpenComputers upstream default is allow.")
            .define("debugCardAccess", "allow");
        DEBUG_CARD_WHITELIST = builder
            .comment("Extra debug card whitelist entries as '<player> <nonce>'. Upstream-compatible file entries live in config/opencomputers/debug_card_whitelist.txt and can be managed with /oc_debugWhitelist.")
            .defineList("debugCardWhitelist", DEFAULT_DEBUG_CARD_WHITELIST, value -> value instanceof String);
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
        STARTUP_DELAY = builder
            .comment("Seconds a loaded computer waits before resuming after world load. OpenComputers upstream default is 0.25 and minimum is 0.05.")
            .defineInRange("startupDelay", 0.25D, 0.05D, Double.MAX_VALUE);
        EEPROM_SIZE = builder
            .comment("EEPROM code storage size in bytes. OpenComputers upstream default is 4096.")
            .defineInRange("eepromSize", 4096, 0, Integer.MAX_VALUE);
        EEPROM_DATA_SIZE = builder
            .comment("EEPROM data storage size in bytes. OpenComputers upstream default is 256.")
            .defineInRange("eepromDataSize", 256, 0, Integer.MAX_VALUE);
        CPU_COMPONENT_COUNT = builder
            .comment("Supported component counts for tier-one, tier-two, tier-three, and creative CPUs. OpenComputers upstream default is [8, 12, 16, 1024].")
            .defineList("cpuComponentCount", DEFAULT_CPU_COMPONENT_COUNT, value -> value instanceof Integer && (Integer) value >= 0);
        CALL_BUDGETS = builder
            .comment("Direct-call budgets for tier-one, tier-two, and tier-three CPUs. OpenComputers upstream default is [0.5, 1.0, 1.5].")
            .defineList("callBudgets", DEFAULT_CALL_BUDGETS, value -> value instanceof Double && (Double) value >= 0D);
        ERASE_TMP_ON_REBOOT = builder
            .comment("Erase the temporary filesystem when a computer reboots. OpenComputers upstream default is false.")
            .define("eraseTmpOnReboot", false);
        EXECUTION_DELAY = builder
            .comment("Milliseconds computers wait before resuming after an immediate yield. OpenComputers upstream default is 12.")
            .defineInRange("executionDelay", 12, 0, 50);
        builder.push("lua");
        ALLOW_BYTECODE = builder
            .comment("Allow loading Lua bytecode directly. OpenComputers upstream default is false.")
            .define("allowBytecode", false);
        ALLOW_GC = builder
            .comment("Allow custom Lua __gc callbacks. OpenComputers upstream default is false.")
            .define("allowGC", false);
        builder.pop();
        builder.pop();

        builder.push("robot");
        builder.push("xp");
        EXPERIENCE_BUFFER_PER_LEVEL = builder
            .comment("Additional energy buffer per robot experience level. OpenComputers upstream default is 5000.")
            .defineInRange("bufferPerLevel", 5_000D, 0D, Double.MAX_VALUE);
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
        HDD_READ = builder
            .comment("Energy cost to read one kilobyte from a filesystem. OpenComputers upstream default is 0.1.")
            .defineInRange("hddRead", 0.1D, 0D, Double.MAX_VALUE);
        HDD_WRITE = builder
            .comment("Energy cost to write one kilobyte to a filesystem. OpenComputers upstream default is 0.25.")
            .defineInRange("hddWrite", 0.25D, 0D, Double.MAX_VALUE);
        HDD_SIZES = builder
            .comment("Sizes of the three hard drive tiers in kilobytes. OpenComputers upstream default is [1024, 2048, 4096].")
            .defineList("hddSizes", DEFAULT_HDD_SIZES, value -> value instanceof Integer && (Integer) value >= 0);
        HDD_PLATTER_COUNTS = builder
            .comment("Physical platter counts for the three hard drive tiers in unmanaged mode. OpenComputers upstream default is [2, 4, 8].")
            .defineList("hddPlatterCounts", DEFAULT_HDD_PLATTER_COUNTS, value -> value instanceof Integer && (Integer) value >= 1);
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

        builder.push("hologram");
        HOLOGRAM_MAX_SCALE = builder
            .comment("Maximum hologram render scale for tier one and tier two. OpenComputers upstream default is [3.0, 4.0].")
            .defineList("maxScale", DEFAULT_HOLOGRAM_MAX_SCALE, value -> value instanceof Double && (Double) value >= 0D);
        HOLOGRAM_MAX_TRANSLATION = builder
            .comment("Maximum hologram translation for tier one and tier two. OpenComputers upstream default is [1.0, 2.0].")
            .defineList("maxTranslation", DEFAULT_HOLOGRAM_MAX_TRANSLATION, value -> value instanceof Double && (Double) value >= 0D);
        HOLOGRAM_SET_RAW_DELAY = builder
            .comment("Pause in seconds after setting the full raw hologram buffer. OpenComputers upstream default is 0.2.")
            .defineInRange("setRawDelay", 0.2D, 0D, Double.MAX_VALUE);
        builder.pop();

        builder.push("power");
        IGNORE_POWER = builder
            .comment("Disable energy requirements. OpenComputers upstream default is false.")
            .define("ignorePower", false);
        GENERATOR_EFFICIENCY = builder
            .comment("Energy produced per tick by generator upgrades. OpenComputers upstream default is 0.8.")
            .defineInRange("generatorEfficiency", 0.8D, 0D, Double.MAX_VALUE);
        SOLAR_GENERATOR_EFFICIENCY = builder
            .comment("Energy produced per tick by solar generator upgrades. OpenComputers upstream default is 0.2.")
            .defineInRange("solarGeneratorEfficiency", 0.2D, 0D, Double.MAX_VALUE);
        ASSEMBLER_TICK_AMOUNT = builder
            .comment("Energy the assembler can apply per tick. OpenComputers upstream default is 50.")
            .defineInRange("assemblerTickAmount", 50D, 1D, Double.MAX_VALUE);
        DISASSEMBLER_TICK_AMOUNT = builder
            .comment("Energy the disassembler can apply per tick. OpenComputers upstream default is 25.")
            .defineInRange("disassemblerTickAmount", 25D, 1D, Double.MAX_VALUE);
        MFU_TICK_FREQUENCY = builder
            .comment("Tick interval for periodic power costs. OpenComputers upstream default is 10.")
            .defineInRange("tickFrequency", 10, 1, Integer.MAX_VALUE);
        builder.push("buffer");
        BATTERY_UPGRADE_BUFFERS = builder
            .comment("Energy stored by battery upgrade tiers one, two, and three. OpenComputers upstream default is [10000, 15000, 20000].")
            .defineList("batteryUpgrades", DEFAULT_BATTERY_UPGRADE_BUFFERS, value -> value instanceof Double && (Double) value >= 0D);
        POWER_DISTRIBUTOR_BUFFER = builder
            .comment("Energy each face of a power distributor can store. OpenComputers upstream default is 500.")
            .defineInRange("distributor", 500D, 0D, Double.MAX_VALUE);
        TABLET_BUFFER = builder
            .comment("Energy a tablet can store. OpenComputers upstream default is 10000.")
            .defineInRange("tablet", 10_000D, 0D, Double.MAX_VALUE);
        CONVERTER_BUFFER = builder
            .comment("Energy buffer for converter-like powered blocks such as the assembler. OpenComputers upstream default is 1000.")
            .defineInRange("converter", 1_000D, 0D, Double.MAX_VALUE);
        COMPUTER_BUFFER = builder
            .comment("Energy a computer can store. OpenComputers upstream default is 500.")
            .defineInRange("computer", 500D, 0D, Double.MAX_VALUE);
        ACCESS_POINT_BUFFER = builder
            .comment("Energy an access point can store. OpenComputers upstream default is 600.")
            .defineInRange("accessPoint", 600D, 0D, Double.MAX_VALUE);
        NANOMACHINES_BUFFER = builder
            .comment("Nanomachines local energy buffer. OpenComputers upstream default is 100000.")
            .defineInRange("nanomachines", 100_000D, 0D, Double.MAX_VALUE);
        builder.pop();
        builder.push("cost");
        WIRELESS_COST_PER_RANGE = builder
            .comment("Wireless card energy cost per block of signal strength for tier one and tier two. OpenComputers upstream default is [0.05, 0.05].")
            .defineList("wirelessCostPerRange", DEFAULT_WIRELESS_COST_PER_RANGE, value -> value instanceof Double && (Double) value >= 0D);
        EEPROM_WRITE_COST = builder
            .comment("Energy consumed when writing EEPROM code or data. OpenComputers upstream default is 50.")
            .defineInRange("eepromWrite", 50D, 0D, Double.MAX_VALUE);
        TABLET_ASSEMBLY_BASE_COST = builder
            .comment("Base energy consumed when assembling a tablet. OpenComputers upstream default is 20000.")
            .defineInRange("tabletAssemblyBase", 20_000D, 0D, Double.MAX_VALUE);
        TABLET_ASSEMBLY_COMPLEXITY_COST = builder
            .comment("Energy consumed per tablet assembly complexity point. OpenComputers upstream default is 5000.")
            .defineInRange("tabletAssemblyComplexity", 5_000D, 0D, Double.MAX_VALUE);
        DATA_CARD_TRIVIAL = builder
            .comment("Base energy cost for trivial Data Card operations. OpenComputers upstream default is 0.2.")
            .defineInRange("dataCardTrivial", 0.2D, 0D, Double.MAX_VALUE);
        DATA_CARD_TRIVIAL_BYTE = builder
            .comment("Per-byte energy cost for trivial Data Card operations. OpenComputers upstream default is 0.005.")
            .defineInRange("dataCardTrivialByte", 0.005D, 0D, Double.MAX_VALUE);
        DATA_CARD_SIMPLE = builder
            .comment("Base energy cost for simple Data Card operations. OpenComputers upstream default is 1.0.")
            .defineInRange("dataCardSimple", 1D, 0D, Double.MAX_VALUE);
        DATA_CARD_SIMPLE_BYTE = builder
            .comment("Per-byte energy cost for simple Data Card operations. OpenComputers upstream default is 0.01.")
            .defineInRange("dataCardSimpleByte", 0.01D, 0D, Double.MAX_VALUE);
        DATA_CARD_COMPLEX = builder
            .comment("Base energy cost for complex Data Card operations. OpenComputers upstream default is 6.0.")
            .defineInRange("dataCardComplex", 6D, 0D, Double.MAX_VALUE);
        DATA_CARD_COMPLEX_BYTE = builder
            .comment("Per-byte energy cost for complex Data Card operations. OpenComputers upstream default is 0.1.")
            .defineInRange("dataCardComplexByte", 0.1D, 0D, Double.MAX_VALUE);
        DATA_CARD_ASYMMETRIC = builder
            .comment("Base energy cost for asymmetric Data Card operations. OpenComputers upstream default is 10.0.")
            .defineInRange("dataCardAsymmetric", 10D, 0D, Double.MAX_VALUE);
        SCREEN_COST = builder
            .comment("Energy consumed per tick by a basic screen when fully lit. OpenComputers upstream default is 0.05.")
            .defineInRange("screen", 0.05D, 0D, Double.MAX_VALUE);
        HOLOGRAM_COST = builder
            .comment("Energy consumed per tick by a Hologram projector when every column is lit. OpenComputers upstream default is 0.2.")
            .defineInRange("hologram", 0.2D, 0D, Double.MAX_VALUE);
        GPU_SET_COST = builder
            .comment("Energy to change every character on a basic screen with gpu.set. OpenComputers upstream default is 2.0 and is applied per affected cell.")
            .defineInRange("gpuSet", 2D, 0D, Double.MAX_VALUE);
        GPU_FILL_COST = builder
            .comment("Energy to fill every character on a basic screen with a non-space character. OpenComputers upstream default is 1.0 and is applied per affected cell.")
            .defineInRange("gpuFill", 1D, 0D, Double.MAX_VALUE);
        GPU_CLEAR_COST = builder
            .comment("Energy to fill every character on a basic screen with space. OpenComputers upstream default is 0.1 and is applied per affected cell.")
            .defineInRange("gpuClear", 0.1D, 0D, Double.MAX_VALUE);
        GPU_COPY_COST = builder
            .comment("Energy to copy every character on a basic screen. OpenComputers upstream default is 0.25 and is applied per affected cell.")
            .defineInRange("gpuCopy", 0.25D, 0D, Double.MAX_VALUE);
        GEOLYZER_SCAN_COST = builder
            .comment("Energy consumed per Geolyzer scan/analyze/store operation. OpenComputers upstream default is 10.")
            .defineInRange("geolyzerScan", 10D, 0D, Double.MAX_VALUE);
        TRANSPOSER_COST = builder
            .comment("Energy consumed per Transposer item/fluid transfer. OpenComputers upstream default is 1.")
            .defineInRange("transposer", 1D, 0D, Double.MAX_VALUE);
        DISASSEMBLER_ITEM_COST = builder
            .comment("Energy consumed for each disassembler output item. OpenComputers upstream default is 2000.")
            .defineInRange("disassemblerPerItem", 2000D, 0D, Double.MAX_VALUE);
        MFU_RELAY_COST = builder
            .comment("MFU relay energy cost per block and tick-frequency interval.")
            .defineInRange("mfuRelay", 1D, 0D, Double.MAX_VALUE);
        NANOMACHINES_INPUT_COST = builder
            .comment("Energy consumed per tick per active nanomachine input. OpenComputers upstream default is 0.5.")
            .defineInRange("nanomachineInput", 0.5D, 0D, Double.MAX_VALUE);
        NANOMACHINES_RECONFIGURE_COST = builder
            .comment("Energy consumed when reconfiguring nanomachines. OpenComputers upstream default is 5000.")
            .defineInRange("nanomachinesReconfigure", 5000D, 0D, Double.MAX_VALUE);
        builder.pop();
        builder.pop();

        builder.push("screen");
        SCREEN_WIDTHS_BY_TIER = builder
            .comment("Maximum screen widths for tiers one, two, and three. OpenComputers upstream default is [50, 80, 160].")
            .defineList("widthsByTier", DEFAULT_SCREEN_WIDTHS_BY_TIER, value -> value instanceof Integer && (Integer) value >= 1);
        SCREEN_HEIGHTS_BY_TIER = builder
            .comment("Maximum screen heights for tiers one, two, and three. OpenComputers upstream default is [16, 25, 50].")
            .defineList("heightsByTier", DEFAULT_SCREEN_HEIGHTS_BY_TIER, value -> value instanceof Integer && (Integer) value >= 1);
        SCREEN_DEPTHS_BY_TIER = builder
            .comment("Maximum screen color depths in bits for tiers one, two, and three. OpenComputers upstream default is [1, 4, 8].")
            .defineList("depthsByTier", DEFAULT_SCREEN_DEPTHS_BY_TIER, value -> value instanceof Integer && ((Integer) value == 1 || (Integer) value == 4 || (Integer) value == 8));
        builder.pop();

        builder.push("gpu");
        GPU_VRAM_SIZES = builder
            .comment("Video RAM multiplier per GPU tier. Total VRAM is max screen cells times this value. OpenComputers upstream default is [1, 2, 3].")
            .defineList("vramSizes", DEFAULT_GPU_VRAM_SIZES, value -> value instanceof Double && (Double) value >= 0D);
        GPU_BITBLT_COST = builder
            .comment("Direct-call budget cost for blitting one full tier-one GPU page to a screen. OpenComputers upstream default is 0.5 and scales by GPU tier.")
            .defineInRange("bitbltCost", 0.5D, 0D, Double.MAX_VALUE);
        builder.pop();

        builder.push("nanomachines");
        NANOMACHINES_TRIGGER_QUOTA = builder
            .comment("Relative nanomachine trigger input count based on behavior count. OpenComputers upstream default is 0.4.")
            .defineInRange("triggerQuota", 0.4D, 0D, Double.MAX_VALUE);
        NANOMACHINES_CONNECTOR_QUOTA = builder
            .comment("Relative nanomachine connector count based on behavior count. OpenComputers upstream default is 0.2.")
            .defineInRange("connectorQuota", 0.2D, 0D, Double.MAX_VALUE);
        NANOMACHINE_MAX_INPUTS = builder
            .comment("Maximum number of inputs per nanomachine neural-network node. OpenComputers upstream default is 2.")
            .defineInRange("maxInputs", 2, 1, Integer.MAX_VALUE);
        NANOMACHINE_MAX_OUTPUTS = builder
            .comment("Maximum number of outputs per nanomachine neural-network node. OpenComputers upstream default is 2.")
            .defineInRange("maxOutputs", 2, 1, Integer.MAX_VALUE);
        NANOMACHINES_SAFE_INPUTS_ACTIVE = builder
            .comment("Number of active nanomachine inputs considered safe. OpenComputers upstream default is 2.")
            .defineInRange("safeInputsActive", 2, 0, Integer.MAX_VALUE);
        NANOMACHINES_MAX_INPUTS_ACTIVE = builder
            .comment("Maximum number of simultaneously active nanomachine inputs. OpenComputers upstream default is 4.")
            .defineInRange("maxInputsActive", 4, 0, Integer.MAX_VALUE);
        NANOMACHINES_COMMAND_DELAY = builder
            .comment("Delay between nanomachine wireless commands in seconds. OpenComputers upstream default is 1.")
            .defineInRange("commandDelay", 1D, 0D, Double.MAX_VALUE);
        NANOMACHINES_COMMAND_RANGE = builder
            .comment("Range for nanomachine wireless commands in blocks. OpenComputers upstream default is 2.")
            .defineInRange("commandRange", 2D, 0D, Double.MAX_VALUE);
        NANOMACHINES_HUNGRY_DAMAGE = builder
            .comment("Damage applied when hungry nanomachine behaviors run out of energy. OpenComputers upstream default is 5.")
            .defineInRange("hungryDamage", 5D, 0D, Double.MAX_VALUE);
        NANOMACHINES_HUNGRY_ENERGY_RESTORED = builder
            .comment("Energy restored after hungry nanomachine out-of-energy damage. OpenComputers upstream default is 50.")
            .defineInRange("hungryEnergyRestored", 50D, 0D, Double.MAX_VALUE);
        NANOMACHINES_MAGNET_RANGE = builder
            .comment("Range of the item magnet behavior added for each active input. OpenComputers upstream default is 8.")
            .defineInRange("magnetRange", 8D, 0D, Double.MAX_VALUE);
        NANOMACHINES_DISINTEGRATION_RANGE = builder
            .comment("Range of the block disintegration behavior added for each active input. OpenComputers upstream default is 1.")
            .defineInRange("disintegrationRange", 1D, 0D, Double.MAX_VALUE);
        NANOMACHINES_POTION_WHITELIST = builder
            .comment("Allowed nanomachine potion effect IDs. Entries may be strings or numeric registry IDs, matching upstream.")
            .defineList("potionWhitelist", DEFAULT_NANOMACHINES_POTION_WHITELIST, value -> value instanceof String || value instanceof Number);
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

    public static boolean ignorePower() {
        return booleanValue(IGNORE_POWER);
    }

    public static int mfuTickFrequency() {
        return intValue(MFU_TICK_FREQUENCY);
    }

    public static double solarGeneratorEfficiency() {
        return doubleValue(SOLAR_GENERATOR_EFFICIENCY);
    }

    public static double generatorEfficiency() {
        return doubleValue(GENERATOR_EFFICIENCY);
    }

    public static double assemblerTickAmount() {
        return Math.max(1D, doubleValue(ASSEMBLER_TICK_AMOUNT));
    }

    public static double disassemblerTickAmount() {
        return Math.max(1D, doubleValue(DISASSEMBLER_TICK_AMOUNT));
    }

    public static double disassemblerItemCost() {
        return Math.max(0D, doubleValue(DISASSEMBLER_ITEM_COST));
    }

    public static List<Double> batteryUpgradeBuffers() {
        final List<Double> buffers = doubleListValue(BATTERY_UPGRADE_BUFFERS);
        if (buffers.size() != DEFAULT_BATTERY_UPGRADE_BUFFERS.size()) {
            return DEFAULT_BATTERY_UPGRADE_BUFFERS;
        }
        return buffers;
    }

    public static double batteryUpgradeBuffer(final int tier) {
        final List<Double> buffers = batteryUpgradeBuffers();
        return buffers.get(clampIndex(tier, buffers.size()));
    }

    public static double powerDistributorBuffer() {
        return Math.max(0D, doubleValue(POWER_DISTRIBUTOR_BUFFER));
    }

    public static double tabletBuffer() {
        return Math.max(0D, doubleValue(TABLET_BUFFER));
    }

    public static double tabletAssemblyBaseCost() {
        return Math.max(0D, doubleValue(TABLET_ASSEMBLY_BASE_COST));
    }

    public static double tabletAssemblyComplexityCost() {
        return Math.max(0D, doubleValue(TABLET_ASSEMBLY_COMPLEXITY_COST));
    }

    public static double converterBuffer() {
        return Math.max(0D, doubleValue(CONVERTER_BUFFER));
    }

    public static double computerBuffer() {
        return Math.max(0D, doubleValue(COMPUTER_BUFFER));
    }

    public static double accessPointBuffer() {
        return Math.max(0D, doubleValue(ACCESS_POINT_BUFFER));
    }

    public static double experienceBufferPerLevel() {
        return Math.max(0D, doubleValue(EXPERIENCE_BUFFER_PER_LEVEL));
    }

    public static List<Integer> screenWidthsByTier() {
        return positiveTierList(SCREEN_WIDTHS_BY_TIER, DEFAULT_SCREEN_WIDTHS_BY_TIER);
    }

    public static int screenWidthByTier(final int tier) {
        final List<Integer> widths = screenWidthsByTier();
        return widths.get(clampIndex(tier, widths.size()));
    }

    public static List<Integer> screenHeightsByTier() {
        return positiveTierList(SCREEN_HEIGHTS_BY_TIER, DEFAULT_SCREEN_HEIGHTS_BY_TIER);
    }

    public static int screenHeightByTier(final int tier) {
        final List<Integer> heights = screenHeightsByTier();
        return heights.get(clampIndex(tier, heights.size()));
    }

    public static List<TextBuffer.ColorDepth> screenDepthsByTier() {
        return screenDepthBitsByTier().stream().map(ModSettings::screenDepth).toList();
    }

    public static TextBuffer.ColorDepth screenDepthByTier(final int tier) {
        final List<TextBuffer.ColorDepth> depths = screenDepthsByTier();
        return depths.get(clampIndex(tier, depths.size()));
    }

    public static double nanomachinesBuffer() {
        return Math.max(0D, doubleValue(NANOMACHINES_BUFFER));
    }

    public static double nanomachinesInputCost() {
        return Math.max(0D, doubleValue(NANOMACHINES_INPUT_COST));
    }

    public static double nanomachinesReconfigureCost() {
        return Math.max(0D, doubleValue(NANOMACHINES_RECONFIGURE_COST));
    }

    public static double nanomachineTriggerQuota() {
        return Math.max(0D, doubleValue(NANOMACHINES_TRIGGER_QUOTA));
    }

    public static double nanomachineConnectorQuota() {
        return Math.max(0D, doubleValue(NANOMACHINES_CONNECTOR_QUOTA));
    }

    public static int nanomachineMaxInputs() {
        return Math.max(1, intValue(NANOMACHINE_MAX_INPUTS));
    }

    public static int nanomachineMaxOutputs() {
        return Math.max(1, intValue(NANOMACHINE_MAX_OUTPUTS));
    }

    public static int nanomachinesSafeInputsActive() {
        return Math.max(0, intValue(NANOMACHINES_SAFE_INPUTS_ACTIVE));
    }

    public static int nanomachinesMaxInputsActive() {
        return Math.max(0, intValue(NANOMACHINES_MAX_INPUTS_ACTIVE));
    }

    public static double nanomachinesCommandDelay() {
        return Math.max(0D, doubleValue(NANOMACHINES_COMMAND_DELAY));
    }

    public static double nanomachinesCommandRange() {
        return Math.max(0D, doubleValue(NANOMACHINES_COMMAND_RANGE));
    }

    public static double nanomachinesHungryDamage() {
        return Math.max(0D, doubleValue(NANOMACHINES_HUNGRY_DAMAGE));
    }

    public static double nanomachinesHungryEnergyRestored() {
        return Math.max(0D, doubleValue(NANOMACHINES_HUNGRY_ENERGY_RESTORED));
    }

    public static double nanomachinesMagnetRange() {
        return Math.max(0D, doubleValue(NANOMACHINES_MAGNET_RANGE));
    }

    public static double nanomachinesDisintegrationRange() {
        return Math.max(0D, doubleValue(NANOMACHINES_DISINTEGRATION_RANGE));
    }

    public static List<Object> nanomachinesPotionWhitelist() {
        return objectListValue(NANOMACHINES_POTION_WHITELIST);
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

    public static double startupDelay() {
        return Math.max(0.05D, doubleValue(STARTUP_DELAY));
    }

    public static double redstoneDelay() {
        return Math.max(0D, doubleValue(REDSTONE_DELAY));
    }

    public static int eepromSize() {
        return intValue(EEPROM_SIZE);
    }

    public static int eepromDataSize() {
        return intValue(EEPROM_DATA_SIZE);
    }

    public static double eepromWriteCost() {
        return doubleValue(EEPROM_WRITE_COST);
    }

    public static double dataCardTrivialCost() {
        return doubleValue(DATA_CARD_TRIVIAL);
    }

    public static double dataCardTrivialByteCost() {
        return doubleValue(DATA_CARD_TRIVIAL_BYTE);
    }

    public static double dataCardSimpleCost() {
        return doubleValue(DATA_CARD_SIMPLE);
    }

    public static double dataCardSimpleByteCost() {
        return doubleValue(DATA_CARD_SIMPLE_BYTE);
    }

    public static double dataCardComplexCost() {
        return doubleValue(DATA_CARD_COMPLEX);
    }

    public static double dataCardComplexByteCost() {
        return doubleValue(DATA_CARD_COMPLEX_BYTE);
    }

    public static double dataCardAsymmetricCost() {
        return doubleValue(DATA_CARD_ASYMMETRIC);
    }

    public static double screenCost() {
        return Math.max(0D, doubleValue(SCREEN_COST));
    }

    public static double hologramCost() {
        return Math.max(0D, doubleValue(HOLOGRAM_COST));
    }

    public static double geolyzerScanCost() {
        return doubleValue(GEOLYZER_SCAN_COST);
    }

    public static double transposerCost() {
        return doubleValue(TRANSPOSER_COST);
    }

    public static List<Integer> cpuComponentCount() {
        final List<Integer> counts = listValue(CPU_COMPONENT_COUNT);
        if (counts.size() != DEFAULT_CPU_COMPONENT_COUNT.size()) {
            return DEFAULT_CPU_COMPONENT_COUNT;
        }
        return counts;
    }

    public static int cpuComponentCount(final int tier) {
        final List<Integer> counts = cpuComponentCount();
        return counts.get(clampIndex(tier, counts.size()));
    }

    public static List<Integer> deviceComplexityByTier() {
        return DEFAULT_DEVICE_COMPLEXITY_BY_TIER;
    }

    public static int deviceComplexityByTier(final int tier) {
        return DEFAULT_DEVICE_COMPLEXITY_BY_TIER.get(clampIndex(tier, DEFAULT_DEVICE_COMPLEXITY_BY_TIER.size()));
    }

    public static List<Double> callBudgets() {
        final List<Double> budgets = doubleListValue(CALL_BUDGETS);
        if (budgets.size() != DEFAULT_CALL_BUDGETS.size()) {
            return DEFAULT_CALL_BUDGETS;
        }
        return budgets;
    }

    public static double callBudget(final int tier) {
        final List<Double> budgets = callBudgets();
        return budgets.get(clampIndex(tier, budgets.size()));
    }

    public static boolean eraseTmpOnReboot() {
        return booleanValue(ERASE_TMP_ON_REBOOT);
    }

    public static int executionDelay() {
        return intValue(EXECUTION_DELAY);
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

    public static int dataCardSoftLimit() {
        return intValue(DATA_CARD_SOFT_LIMIT);
    }

    public static int dataCardHardLimit() {
        return intValue(DATA_CARD_HARD_LIMIT);
    }

    public static double dataCardTimeout() {
        return doubleValue(DATA_CARD_TIMEOUT);
    }

    public static double disassemblerBreakChance() {
        return Math.max(0D, Math.min(1D, doubleValue(DISASSEMBLER_BREAK_CHANCE)));
    }

    public static int geolyzerRange() {
        return intValue(GEOLYZER_RANGE);
    }

    public static int transposerFluidTransferRate() {
        return Math.max(1, intValue(TRANSPOSER_FLUID_TRANSFER_RATE));
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

    public static double hddReadCost() {
        return doubleValue(HDD_READ) / 1024.0D;
    }

    public static double hddWriteCost() {
        return doubleValue(HDD_WRITE) / 1024.0D;
    }

    public static int diskActivitySoundDelay() {
        return Math.max(-1, intValue(DISK_ACTIVITY_SOUND_DELAY));
    }

    public static double gpuSetCost() {
        return doubleValue(GPU_SET_COST) / basicScreenPixels();
    }

    public static double gpuFillCost() {
        return doubleValue(GPU_FILL_COST) / basicScreenPixels();
    }

    public static double gpuClearCost() {
        return doubleValue(GPU_CLEAR_COST) / basicScreenPixels();
    }

    public static double gpuCopyCost() {
        return doubleValue(GPU_COPY_COST) / basicScreenPixels();
    }

    public static double gpuBitbltCost() {
        return Math.max(0D, doubleValue(GPU_BITBLT_COST));
    }

    public static double gpuVramSizeByTier(final int tier) {
        final List<Double> values = doubleListValue(GPU_VRAM_SIZES);
        if (values.size() != DEFAULT_GPU_VRAM_SIZES.size()) {
            return DEFAULT_GPU_VRAM_SIZES.get(clampIndex(tier, DEFAULT_GPU_VRAM_SIZES.size()));
        }
        return Math.max(0D, values.get(clampIndex(tier, values.size())));
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
        return sizes.get(clampIndex(tier, sizes.size()));
    }

    public static List<Integer> hddPlatterCounts() {
        final List<Integer> counts = listValue(HDD_PLATTER_COUNTS);
        if (counts.size() != DEFAULT_HDD_PLATTER_COUNTS.size()) {
            return DEFAULT_HDD_PLATTER_COUNTS;
        }
        return counts;
    }

    public static int hddPlatterCount(final int tier) {
        final List<Integer> counts = hddPlatterCounts();
        return counts.get(clampIndex(tier, counts.size()));
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

    public static boolean enableNanomachinePfx() {
        return booleanValue(ENABLE_NANOMACHINE_PFX);
    }

    public static List<Double> nanomachineHudPos() {
        final List<Double> position = doubleListValue(NANOMACHINE_HUD_POS);
        if (position.size() != DEFAULT_NANOMACHINE_HUD_POS.size()) {
            return DEFAULT_NANOMACHINE_HUD_POS;
        }
        return position;
    }

    public static List<Double> hologramMaxScale() {
        final List<Double> scales = doubleListValue(HOLOGRAM_MAX_SCALE);
        if (scales.size() != DEFAULT_HOLOGRAM_MAX_SCALE.size()) {
            return DEFAULT_HOLOGRAM_MAX_SCALE;
        }
        return scales;
    }

    public static double hologramMaxScale(final int tier) {
        final List<Double> scales = hologramMaxScale();
        return scales.get(clampIndex(tier, scales.size()));
    }

    public static List<Double> hologramMaxTranslation() {
        final List<Double> translations = doubleListValue(HOLOGRAM_MAX_TRANSLATION);
        if (translations.size() != DEFAULT_HOLOGRAM_MAX_TRANSLATION.size()) {
            return DEFAULT_HOLOGRAM_MAX_TRANSLATION;
        }
        return translations;
    }

    public static double hologramMaxTranslation(final int tier) {
        final List<Double> translations = hologramMaxTranslation();
        return translations.get(clampIndex(tier, translations.size()));
    }

    public static double hologramSetRawDelay() {
        return doubleValue(HOLOGRAM_SET_RAW_DELAY);
    }

    public static String debugCardAccess() {
        final String value = stringValue(DEBUG_CARD_ACCESS).trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "true", "allow" -> "allow";
            case "false", "deny" -> "deny";
            case "whitelist" -> "whitelist";
            default -> "deny";
        };
    }

    public static Optional<String> debugCardWhitelistNonce(final String player) {
        if (player == null || player.isBlank()) {
            return Optional.empty();
        }
        final Optional<String> fileNonce = DebugCardWhitelist.instance().nonce(player);
        if (fileNonce.isPresent()) {
            return fileNonce;
        }
        final String normalizedPlayer = player.toLowerCase(Locale.ROOT);
        for (final String entry : stringListValue(DEBUG_CARD_WHITELIST)) {
            final String[] parts = entry.split(" ", 2);
            if (parts.length == 2 && normalizedPlayer.equals(parts[0].toLowerCase(Locale.ROOT))) {
                return Optional.of(parts[1]);
            }
        }
        return Optional.empty();
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

    private static List<Integer> positiveTierList(final ModConfigSpec.ConfigValue<List<? extends Integer>> value, final List<Integer> defaults) {
        final List<Integer> values = listValue(value);
        if (values.size() != defaults.size()) {
            return defaults;
        }
        return values.stream().map(entry -> Math.max(1, entry)).toList();
    }

    private static List<Integer> screenDepthBitsByTier() {
        final List<Integer> depths = listValue(SCREEN_DEPTHS_BY_TIER);
        if (depths.size() != DEFAULT_SCREEN_DEPTHS_BY_TIER.size() || depths.stream().anyMatch(depth -> depth != 1 && depth != 4 && depth != 8)) {
            return DEFAULT_SCREEN_DEPTHS_BY_TIER;
        }
        return depths;
    }

    private static TextBuffer.ColorDepth screenDepth(final int bits) {
        return switch (bits) {
            case 4 -> TextBuffer.ColorDepth.FourBit;
            case 8 -> TextBuffer.ColorDepth.EightBit;
            default -> TextBuffer.ColorDepth.OneBit;
        };
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

    private static List<Object> objectListValue(final ModConfigSpec.ConfigValue<List<? extends Object>> value) {
        try {
            return List.copyOf(value.get());
        } catch (final IllegalStateException ignored) {
            return List.copyOf(value.getDefault());
        }
    }

    private static double basicScreenPixels() {
        return (double) screenWidthByTier(0) * (double) screenHeightByTier(0);
    }

    private static int clampIndex(final int index, final int size) {
        return Math.max(0, Math.min(size - 1, index));
    }
}
