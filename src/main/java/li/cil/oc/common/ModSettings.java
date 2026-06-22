package li.cil.oc.common;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModSettings {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue MFU_RANGE;
    public static final ModConfigSpec.BooleanValue INPUT_USERNAME;
    public static final ModConfigSpec.BooleanValue CAN_COMPUTERS_BE_OWNED;
    public static final ModConfigSpec.IntValue MAX_USERS;
    public static final ModConfigSpec.IntValue MAX_USERNAME_LENGTH;
    public static final ModConfigSpec.DoubleValue COMPUTER_TIMEOUT;
    public static final ModConfigSpec.BooleanValue ALLOW_BYTECODE;
    public static final ModConfigSpec.BooleanValue ALLOW_GC;
    public static final ModConfigSpec.IntValue TMP_SIZE;
    public static final ModConfigSpec.IntValue MAX_HANDLES;
    public static final ModConfigSpec.IntValue MAX_READ_BUFFER;
    public static final ModConfigSpec.DoubleValue MFU_RELAY_COST;
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
        MAX_HANDLES = builder
            .comment("Maximum number of file handles any single computer may have open per filesystem. OpenComputers upstream default is 16.")
            .defineInRange("maxHandles", 16, 0, Integer.MAX_VALUE);
        MAX_READ_BUFFER = builder
            .comment("Maximum block size read by one filesystem read call. OpenComputers upstream default is 2048.")
            .defineInRange("maxReadBuffer", 2048, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.push("power");
        SOLAR_GENERATOR_EFFICIENCY = builder
            .comment("Energy produced per tick by solar generator upgrades. OpenComputers upstream default is 0.2.")
            .defineInRange("solarGeneratorEfficiency", 0.2D, 0D, Double.MAX_VALUE);
        MFU_TICK_FREQUENCY = builder
            .comment("Tick interval for periodic power costs. OpenComputers upstream default is 10.")
            .defineInRange("tickFrequency", 10, 1, Integer.MAX_VALUE);
        builder.push("cost");
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

    public static int tmpSize() {
        return intValue(TMP_SIZE);
    }

    public static int maxHandles() {
        return intValue(MAX_HANDLES);
    }

    public static int maxReadBuffer() {
        return intValue(MAX_READ_BUFFER);
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
}
