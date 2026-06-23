package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.prefab.AbstractValue;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.ModSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DebugCardEnvironment extends AbstractManagedEnvironment {
    private static final String COMPONENT_NAME = "debug";
    private static final String DATA_TAG = "oc:data";
    private static final String PLAYER_TAG = "oc:player";
    private static final String ACCESS_NONCE_TAG = "oc:accessNonce";

    private final EnvironmentHost host;
    private AccessContext access;

    public DebugCardEnvironment(final EnvironmentHost host) {
        this(host, null);
    }

    public DebugCardEnvironment(final EnvironmentHost host, final AccessContext access) {
        this.host = host;
        this.access = access;
        final var builder = Network.newNode(this, Visibility.Neighbors);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME).withConnector().create());
        }
    }

    @Callback(doc = "function(value:number):number -- Changes the component network's energy buffer by the specified delta.")
    public Object[] changeBuffer(final Context context, final Arguments args) throws Exception {
        checkAccess();
        if (node() instanceof Connector connector) {
            return new Object[]{connector.changeBuffer(args.checkDouble(0))};
        }
        return new Object[]{0D};
    }

    @Callback(doc = "function():number -- Get the container's X position in the world.")
    public Object[] getX(final Context context, final Arguments args) throws Exception {
        checkAccess();
        return new Object[]{host == null ? 0D : host.xPosition()};
    }

    @Callback(doc = "function():number -- Get the container's Y position in the world.")
    public Object[] getY(final Context context, final Arguments args) throws Exception {
        checkAccess();
        return new Object[]{host == null ? 0D : host.yPosition()};
    }

    @Callback(doc = "function():number -- Get the container's Z position in the world.")
    public Object[] getZ(final Context context, final Arguments args) throws Exception {
        checkAccess();
        return new Object[]{host == null ? 0D : host.zPosition()};
    }

    @Callback(doc = "function(name:string):boolean -- Get whether a mod or API is loaded.")
    public Object[] isModLoaded(final Context context, final Arguments args) throws Exception {
        checkAccess();
        return new Object[]{isLoaded(args.checkString(0))};
    }

    @Callback(doc = "function():userdata -- Get the world object for the container's world.")
    public Object[] getWorld(final Context context, final Arguments args) throws Exception {
        checkAccess();
        return new Object[]{new WorldValue(host == null ? null : host.world(), access)};
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        access = loadAccess(nbt);
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        saveAccess(nbt, access);
    }

    public static AccessContext loadAccess(final CompoundTag root) {
        if (root == null || !root.contains(DATA_TAG)) {
            return null;
        }
        final CompoundTag data = root.getCompound(DATA_TAG);
        if (!data.contains(PLAYER_TAG)) {
            return null;
        }
        return new AccessContext(data.getString(PLAYER_TAG), data.getString(ACCESS_NONCE_TAG));
    }

    public static void saveAccess(final CompoundTag root, final AccessContext access) {
        if (root == null) {
            return;
        }
        final CompoundTag data = root.contains(DATA_TAG) ? root.getCompound(DATA_TAG) : new CompoundTag();
        data.remove(PLAYER_TAG);
        data.remove(ACCESS_NONCE_TAG);
        if (access != null) {
            data.putString(PLAYER_TAG, access.player());
            data.putString(ACCESS_NONCE_TAG, access.nonce());
        }
        root.put(DATA_TAG, data);
    }

    private void checkAccess() throws Exception {
        checkAccess(access);
    }

    private static void checkAccess(final AccessContext access) throws Exception {
        switch (ModSettings.debugCardAccess()) {
            case "allow" -> {
            }
            case "whitelist" -> checkWhitelistAccess(access);
            default -> throw new Exception("debug card is disabled");
        }
    }

    private static void checkWhitelistAccess(final AccessContext access) throws Exception {
        if (access == null) {
            throw new Exception("debug card is whitelisted, Shift+Click with it to bind card to yourself");
        }
        final var nonce = ModSettings.debugCardWhitelistNonce(access.player());
        if (nonce.isEmpty()) {
            throw new Exception("you are not whitelisted to use debug card");
        }
        if (!nonce.get().equals(access.nonce())) {
            throw new Exception("debug card is invalidated, please re-bind it to yourself");
        }
    }

    private static boolean isLoaded(final String name) {
        final String id = name.toLowerCase(Locale.ROOT);
        try {
            if (ModList.get().isLoaded(id)) {
                return true;
            }
        } catch (RuntimeException ignored) {
        }
        return switch (id) {
            case "minecraft", "neoforge", NeoOpenComputers.MODID, "opencomputers" -> true;
            default -> false;
        };
    }

    public static final class WorldValue extends AbstractValue {
        private static final int WEATHER_TIME = Integer.MAX_VALUE;

        private final Level level;
        private final AccessContext access;

        private WorldValue(final Level level, final AccessContext access) {
            this.level = level;
            this.access = access;
        }

        @Callback(doc = "function():string -- Get the name of the current dimension.")
        public Object[] getDimensionName(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            return new Object[]{level == null ? "" : level.dimension().location().toString()};
        }

        @Callback(doc = "function():number -- Get the seed of the world.")
        public Object[] getSeed(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            return new Object[]{level instanceof ServerLevel serverLevel ? serverLevel.getSeed() : 0L};
        }

        @Callback(doc = "function():number -- Get the current world time.")
        public Object[] getTime(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            return new Object[]{level == null ? 0L : level.getDayTime()};
        }

        @Callback(doc = "function(value:number) -- Set the current world time.")
        public Object[] setTime(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.setDayTime(args.checkLong(0));
            }
            return null;
        }

        @Callback(doc = "function():number, number, number -- Get the current spawn point coordinates.")
        public Object[] getSpawnPoint(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            final BlockPos pos = level == null ? BlockPos.ZERO : level.getLevelData().getSpawnPos();
            return new Object[]{pos.getX(), pos.getY(), pos.getZ()};
        }

        @Callback(doc = "function(x:number, y:number, z:number) -- Set the spawn point coordinates.")
        public Object[] setSpawnPoint(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.setDefaultSpawnPos(new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)), serverLevel.getSharedSpawnAngle());
            }
            return null;
        }

        @Callback(doc = "function(x:number, y:number, z:number):boolean -- Check whether the block at the specified coordinates is loaded.")
        public Object[] isLoaded(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            return new Object[]{level != null && level.isLoaded(blockPos(args))};
        }

        @Callback(doc = "function(x:number, y:number, z:number):boolean -- Check whether the block at the specified coordinates has a block entity.")
        public Object[] hasTileEntity(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            final BlockPos pos = blockPos(args);
            return new Object[]{level != null && level.isLoaded(pos) && level.getBlockState(pos).hasBlockEntity()};
        }

        @Callback(doc = "function(x:number, y:number, z:number):table -- Get the NBT of the block entity at the specified coordinates.")
        public Object[] getTileNBT(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            if (level == null) {
                return null;
            }
            final BlockEntity blockEntity = level.getBlockEntity(blockPos(args));
            if (blockEntity == null) {
                return null;
            }
            return new Object[]{tagToMap(blockEntity.saveWithFullMetadata(level.registryAccess()))};
        }

        @Callback(doc = "function(x:number, y:number, z:number):number -- Get the registry ID of the block at the specified coordinates.")
        public Object[] getBlockId(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            return new Object[]{BuiltInRegistries.BLOCK.getId(blockState(blockPos(args)).getBlock())};
        }

        @Callback(doc = "function(x:number, y:number, z:number):number -- Get the metadata of the block at the specified coordinates. Always zero on Minecraft 1.21.")
        public Object[] getMetadata(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            return new Object[]{0};
        }

        @Callback(doc = "function(x:number, y:number, z:number[, actualState:boolean=false]):userdata -- Get the block state for the block at the specified coordinates.")
        public Object[] getBlockState(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            return new Object[]{blockState(blockPos(args))};
        }

        @Callback(doc = "function(x:number, y:number, z:number):number -- Get the light opacity of the block at the specified coordinates.")
        public Object[] getLightOpacity(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            final BlockPos pos = blockPos(args);
            return new Object[]{level == null ? 0 : blockState(pos).getLightBlock(level, pos)};
        }

        @Callback(doc = "function(x:number, y:number, z:number):number -- Get the light value (emission) of the block at the specified coordinates.")
        public Object[] getLightValue(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            return new Object[]{blockState(blockPos(args)).getLightEmission()};
        }

        @Callback(doc = "function(x:number, y:number, z:number):boolean -- Get whether the block at the specified coordinates is directly under the sky.")
        public Object[] canSeeSky(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            return new Object[]{level != null && level.canSeeSky(blockPos(args))};
        }

        @Callback(doc = "function(x:number, y:number, z:number, id:number|string, meta:number):boolean -- Set the block at the specified coordinates.")
        public Object[] setBlock(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            if (level == null) {
                return new Object[]{false};
            }
            final Block block = block(args, 3);
            if (block == Blocks.AIR && args.count() > 3 && !isAir(args, 3)) {
                return new Object[]{false};
            }
            return new Object[]{level.setBlock(blockPos(args), block.defaultBlockState(), 3)};
        }

        @Callback(doc = "function(x1:number, y1:number, z1:number, x2:number, y2:number, z2:number, id:number|string, meta:number) -- Set all blocks in the area defined by two corner points.")
        public Object[] setBlocks(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            if (level == null) {
                return null;
            }
            final Block block = block(args, 6);
            if (block == Blocks.AIR && args.count() > 6 && !isAir(args, 6)) {
                return null;
            }
            final int xMin = Math.min(args.checkInteger(0), args.checkInteger(3));
            final int yMin = Math.min(args.checkInteger(1), args.checkInteger(4));
            final int zMin = Math.min(args.checkInteger(2), args.checkInteger(5));
            final int xMax = Math.max(args.checkInteger(0), args.checkInteger(3));
            final int yMax = Math.max(args.checkInteger(1), args.checkInteger(4));
            final int zMax = Math.max(args.checkInteger(2), args.checkInteger(5));
            for (int x = xMin; x <= xMax; x++) {
                for (int y = yMin; y <= yMax; y++) {
                    for (int z = zMin; z <= zMax; z++) {
                        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 3);
                    }
                }
            }
            return null;
        }

        @Callback(doc = "function():boolean -- Get whether it is raining.")
        public Object[] isRaining(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            return new Object[]{level != null && level.getLevelData().isRaining()};
        }

        @Callback(doc = "function(value:boolean) -- Set whether it is raining.")
        public Object[] setRaining(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            if (level instanceof ServerLevel serverLevel) {
                setWeather(serverLevel, args.checkBoolean(0), serverLevel.getLevelData().isThundering());
            }
            return null;
        }

        @Callback(doc = "function():boolean -- Get whether it is thundering.")
        public Object[] isThundering(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            return new Object[]{level != null && level.getLevelData().isThundering()};
        }

        @Callback(doc = "function(value:boolean) -- Set whether it is thundering.")
        public Object[] setThundering(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            if (level instanceof ServerLevel serverLevel) {
                setWeather(serverLevel, serverLevel.getLevelData().isRaining(), args.checkBoolean(0));
            }
            return null;
        }

        private static void setWeather(final ServerLevel level, final boolean raining, final boolean thundering) {
            level.setWeatherParameters(0, raining || thundering ? WEATHER_TIME : 0, raining, thundering);
        }

        private static BlockPos blockPos(final Arguments args) {
            return new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2));
        }

        private BlockState blockState(final BlockPos pos) {
            return level == null ? Blocks.AIR.defaultBlockState() : level.getBlockState(pos);
        }

        private static Block block(final Arguments args, final int index) {
            if (args.isInteger(index)) {
                return BuiltInRegistries.BLOCK.byId(args.checkInteger(index));
            }
            return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(args.checkString(index)));
        }

        private static boolean isAir(final Arguments args, final int index) {
            return args.isInteger(index)
                ? args.checkInteger(index) == BuiltInRegistries.BLOCK.getId(Blocks.AIR)
                : "minecraft:air".equals(args.checkString(index)) || "air".equals(args.checkString(index));
        }

        private static Map<String, Object> tagToMap(final CompoundTag tag) {
            final Map<String, Object> result = new LinkedHashMap<>();
            for (final String key : tag.getAllKeys()) {
                result.put(key, tagToObject(tag.get(key)));
            }
            return result;
        }

        private static Object tagToObject(final Tag tag) {
            if (tag instanceof NumericTag numericTag) {
                return numericTag.getAsNumber();
            }
            if (tag instanceof StringTag stringTag) {
                return stringTag.getAsString();
            }
            if (tag instanceof ByteArrayTag byteArrayTag) {
                return byteArrayTag.getAsByteArray();
            }
            if (tag instanceof IntArrayTag intArrayTag) {
                return intArrayTag.getAsIntArray();
            }
            if (tag instanceof LongArrayTag longArrayTag) {
                return longArrayTag.getAsLongArray();
            }
            if (tag instanceof ListTag listTag) {
                return tagToList(listTag);
            }
            if (tag instanceof CompoundTag compoundTag) {
                return tagToMap(compoundTag);
            }
            return null;
        }

        private static List<Object> tagToList(final ListTag tag) {
            final List<Object> result = new ArrayList<>(tag.size());
            for (int index = 0; index < tag.size(); index++) {
                result.add(tagToObject(tag.get(index)));
            }
            return result;
        }
    }

    public record AccessContext(String player, String nonce) {
    }
}
