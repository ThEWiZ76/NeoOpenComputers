package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.prefab.AbstractValue;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.network.DebugClipboardPayload;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DebugCardEnvironment extends AbstractManagedEnvironment {
    private static final String COMPONENT_NAME = "debug";
    private static final String DATA_TAG = "oc:data";
    private static final String PLAYER_TAG = "oc:player";
    private static final String ACCESS_NONCE_TAG = "oc:accessNonce";
    private static final Map<String, DebugCardEnvironment> ENDPOINTS = new ConcurrentHashMap<>();

    private final EnvironmentHost host;
    private AccessContext access;
    private Node remoteNode;

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

    @Callback(doc = "function(command:string|table):number, table -- Runs arbitrary command(s) using a server command source.")
    public Object[] runCommand(final Context context, final Arguments args) throws Exception {
        checkAccess();
        if (!(host != null && host.world() instanceof ServerLevel serverLevel) || serverLevel.getServer() == null) {
            return new Object[]{0, null};
        }
        final CommandSourceStack source = serverLevel.getServer()
            .createCommandSourceStack()
            .withLevel(serverLevel)
            .withPosition(new Vec3(host.xPosition(), host.yPosition(), host.zPosition()))
            .withPermission(4)
            .withSuppressedOutput();
        int value = 0;
        for (final Object command : commands(args)) {
            value = serverLevel.getServer().getCommands().getDispatcher().execute(normalizedCommand(command), source);
        }
        return new Object[]{value, null};
    }

    @Callback(doc = "function():userdata -- Get the world object for the container's world.")
    public Object[] getWorld(final Context context, final Arguments args) throws Exception {
        checkAccess();
        return new Object[]{new WorldValue(host == null ? null : host.world(), access)};
    }

    @Callback(doc = "function(name:string):userdata -- Get the entity of a player.")
    public Object[] getPlayer(final Context context, final Arguments args) throws Exception {
        checkAccess();
        return new Object[]{new PlayerValue(host == null ? null : host.world(), access, args.checkString(0))};
    }

    @Callback(doc = "function():table -- Get a list of currently logged-in players.")
    public Object[] getPlayers(final Context context, final Arguments args) throws Exception {
        checkAccess();
        if (!(host != null && host.world() instanceof ServerLevel serverLevel) || serverLevel.getServer() == null) {
            return new Object[]{new String[0]};
        }
        return new Object[]{serverLevel.getServer().getPlayerList().getPlayerNamesArray()};
    }

    @Callback(doc = "function():userdata -- Get the scoreboard object for the container's world.")
    public Object[] getScoreboard(final Context context, final Arguments args) throws Exception {
        checkAccess();
        return new Object[]{new ScoreboardValue(host == null ? null : host.world(), access)};
    }

    @Callback(doc = "function(player:string, text:string) -- Sends text to the specified player's clipboard if possible.")
    public Object[] sendToClipboard(final Context context, final Arguments args) throws Exception {
        checkAccess();
        final String playerName = args.checkString(0);
        final String value = args.checkString(1);
        if (host != null && host.world() instanceof ServerLevel serverLevel && serverLevel.getServer() != null) {
            final ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayerByName(playerName);
            if (player != null && player.connection != null) {
                PacketDistributor.sendToPlayer(player, new DebugClipboardPayload(value));
            }
        }
        return new Object[0];
    }

    @Callback(doc = "function(address:string, data...) -- Sends data to the debug card with the specified address.")
    public Object[] sendToDebugCard(final Context context, final Arguments args) throws Exception {
        checkAccess();
        final String destination = args.checkString(0);
        final DebugCardEnvironment endpoint = ENDPOINTS.get(destination);
        if (endpoint != null && endpoint != this && node() != null) {
            endpoint.receiveDebugPacket(Network.newPacket(node().address(), destination, 0, Arrays.copyOfRange(args.toArray(), 1, args.count())));
        }
        return new Object[0];
    }

    @Callback(doc = "function(x:number, y:number, z:number[, worldId:number]):boolean, string, table -- Returns contents at the location.")
    public Object[] scanContentsAt(final Context context, final Arguments args) throws Exception {
        checkAccess();
        final Level level = host == null ? null : host.world();
        if (level == null) {
            return new Object[]{false, "air", Blocks.AIR};
        }
        final BlockPos pos = new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2));
        if (!level.isLoaded(pos)) {
            return new Object[]{false, "air", Blocks.AIR};
        }

        for (final Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(pos), Entity::isAlive)) {
            if (entity instanceof LivingEntity) {
                return new Object[]{true, "EntityLivingBase", entity};
            }
            if (entity instanceof AbstractMinecart) {
                return new Object[]{true, "EntityMinecart", entity};
            }
        }

        final BlockState state = level.getBlockState(pos);
        final Block block = state.getBlock();
        if (state.isAir()) {
            return new Object[]{false, "air", block};
        }
        if (!state.getFluidState().isEmpty()) {
            return new Object[]{false, "liquid", block};
        }
        if (state.canBeReplaced()) {
            return new Object[]{false, "replaceable", block};
        }
        if (state.getCollisionShape(level, pos).isEmpty()) {
            return new Object[]{true, "passable", block};
        }
        return new Object[]{true, "solid", block};
    }

    @Callback(doc = "function(x:number, y:number, z:number):boolean -- Add a component block at the specified coordinates to the computer network.")
    public Object[] connectToBlock(final Context context, final Arguments args) throws Exception {
        checkAccess();
        final BlockPos pos = new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2));
        final Node other = findNode(pos);
        if (other == null) {
            return new Object[]{null, "no node found at this position"};
        }
        if (remoteNode != null) {
            node().disconnect(remoteNode);
        }
        remoteNode = other;
        node().connect(other);
        return new Object[]{true};
    }

    @Override
    public void onConnect(final Node node) {
        super.onConnect(node);
        if (node == node() && node != null && node.address() != null) {
            ENDPOINTS.put(node.address(), this);
        }
    }

    @Override
    public void onDisconnect(final Node node) {
        super.onDisconnect(node);
        if (node == node() && node != null && node.address() != null) {
            ENDPOINTS.remove(node.address(), this);
        }
    }

    private void receiveDebugPacket(final Packet packet) {
        if (packet == null || node() == null) {
            return;
        }
        final Object[] data = packet.data() == null ? new Object[0] : packet.data();
        final Object[] signal = new Object[data.length + 4];
        signal[0] = "debug_message";
        signal[1] = packet.source();
        signal[2] = packet.port();
        signal[3] = 0D;
        System.arraycopy(data, 0, signal, 4, data.length);
        node().sendToReachable("computer.signal", signal);
    }

    private Node findNode(final BlockPos pos) {
        if (host == null || host.world() == null || pos == null) {
            return null;
        }
        final Level level = host.world();
        if (!level.isLoaded(pos)) {
            return null;
        }
        Network.joinOrCreateNetwork(level, pos);
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SidedEnvironment sidedEnvironment) {
            for (final Direction direction : Direction.values()) {
                final Node sidedNode = sidedEnvironment.sidedNode(direction);
                if (sidedNode != null) {
                    return sidedNode;
                }
            }
        }
        if (blockEntity instanceof Environment environment) {
            return environment.node();
        }
        return null;
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

    private static List<Object> commands(final Arguments args) {
        if (!args.isTable(0)) {
            return List.of(args.checkString(0));
        }
        final List<Object> result = new ArrayList<>();
        for (final Object command : args.checkTable(0).values()) {
            result.add(command);
        }
        return result;
    }

    private static String normalizedCommand(final Object command) {
        final String value = String.valueOf(command);
        return value.startsWith("/") ? value.substring(1) : value;
    }

    public static final class PlayerValue extends AbstractValue {
        private final Level level;
        private final AccessContext access;
        private final String name;

        private PlayerValue(final Level level, final AccessContext access, final String name) {
            this.level = level;
            this.access = access;
            this.name = name;
        }

        @Callback(doc = "function():userdata -- Get the player's world object.")
        public Object[] getWorld(final Context context, final Arguments args) throws Exception {
            return withPlayer(player -> new Object[]{new WorldValue(player.level(), access)});
        }

        @Callback(doc = "function():string -- Get the player's game type.")
        public Object[] getGameType(final Context context, final Arguments args) throws Exception {
            return withPlayer(player -> new Object[]{player.gameMode.getGameModeForPlayer().getName()});
        }

        @Callback(doc = "function(gametype:string) -- Set the player's game type.")
        public Object[] setGameType(final Context context, final Arguments args) throws Exception {
            return withPlayer(player -> {
                player.setGameMode(GameType.byName(args.checkString(0), GameType.SURVIVAL));
                return null;
            });
        }

        @Callback(doc = "function():number, number, number -- Get the player's position.")
        public Object[] getPosition(final Context context, final Arguments args) throws Exception {
            return withPlayer(player -> new Object[]{player.getX(), player.getY(), player.getZ()});
        }

        @Callback(doc = "function(x:number, y:number, z:number) -- Set the player's position.")
        public Object[] setPosition(final Context context, final Arguments args) throws Exception {
            return withPlayer(player -> {
                player.teleportTo(args.checkDouble(0), args.checkDouble(1), args.checkDouble(2));
                return null;
            });
        }

        @Callback(doc = "function():number -- Get the player's health.")
        public Object[] getHealth(final Context context, final Arguments args) throws Exception {
            return withPlayer(player -> new Object[]{player.getHealth()});
        }

        @Callback(doc = "function():number -- Get the player's max health.")
        public Object[] getMaxHealth(final Context context, final Arguments args) throws Exception {
            return withPlayer(player -> new Object[]{player.getMaxHealth()});
        }

        @Callback(doc = "function(health:number) -- Set the player's health.")
        public Object[] setHealth(final Context context, final Arguments args) throws Exception {
            return withPlayer(player -> {
                player.setHealth((float) args.checkDouble(0));
                return null;
            });
        }

        @Callback(doc = "function():number -- Get the player's level.")
        public Object[] getLevel(final Context context, final Arguments args) throws Exception {
            return withPlayer(player -> new Object[]{player.experienceLevel});
        }

        @Callback(doc = "function():number -- Get the player's total experience.")
        public Object[] getExperienceTotal(final Context context, final Arguments args) throws Exception {
            return withPlayer(player -> new Object[]{player.totalExperience});
        }

        @Callback(doc = "function(level:number) -- Add a level to the player's experience level.")
        public Object[] addExperienceLevel(final Context context, final Arguments args) throws Exception {
            return withPlayer(player -> {
                player.giveExperienceLevels(args.checkInteger(0));
                return null;
            });
        }

        @Callback(doc = "function(level:number) -- Remove a level from the player's experience level.")
        public Object[] removeExperienceLevel(final Context context, final Arguments args) throws Exception {
            return withPlayer(player -> {
                player.giveExperienceLevels(-args.checkInteger(0));
                return null;
            });
        }

        @Callback(doc = "function() -- Clear the player's inventory.")
        public Object[] clearInventory(final Context context, final Arguments args) throws Exception {
            return withPlayer(player -> {
                player.getInventory().clearContent();
                return null;
            });
        }

        @Callback(doc = "function(id:string, amount:number, meta:number[, nbt:string]):number -- Adds the item stack to the player's inventory.")
        public Object[] insertItem(final Context context, final Arguments args) throws Exception {
            return withPlayer(player -> {
                final Item item = item(args.checkString(0));
                final int amount = args.checkInteger(1);
                args.checkInteger(2); // Legacy metadata, kept for upstream signature compatibility.
                final ItemStack stack = new ItemStack(item, amount);
                final String tagJson = args.optString(3, "");
                if (!tagJson.isEmpty()) {
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(TagParser.parseTag(tagJson)));
                }
                final int remaining = addToPlayerInventory(stack, player);
                return new Object[]{remaining};
            });
        }

        private Object[] withPlayer(final PlayerOperation operation) throws Exception {
            checkAccess(access);
            final ServerPlayer player = player();
            if (player == null) {
                return new Object[]{null, "player is offline"};
            }
            return operation.apply(player);
        }

        private ServerPlayer player() {
            if (!(level instanceof ServerLevel serverLevel) || serverLevel.getServer() == null) {
                return null;
            }
            return serverLevel.getServer().getPlayerList().getPlayerByName(name);
        }

        private static Item item(final String id) {
            final ResourceLocation key = id.indexOf(':') >= 0 ? ResourceLocation.parse(id) : ResourceLocation.withDefaultNamespace(id);
            if (!BuiltInRegistries.ITEM.containsKey(key)) {
                throw new IllegalArgumentException("invalid item id");
            }
            return BuiltInRegistries.ITEM.get(key);
        }

        private static int addToPlayerInventory(final ItemStack stack, final ServerPlayer player) {
            final int remaining = insertIntoInventory(player.getInventory(), stack);
            if (remaining > 0) {
                final ItemStack dropped = stack.copy();
                dropped.setCount(remaining);
                final ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getY() + 0.5D, player.getZ(), dropped);
                entity.setPickUpDelay(40);
                player.level().addFreshEntity(entity);
            }
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            return remaining;
        }

        private static int insertIntoInventory(final Container inventory, final ItemStack stack) {
            int remaining = stack.getCount();
            final int size = inventory.getContainerSize();
            for (int slot = 0; slot < size && remaining > 0; slot++) {
                final ItemStack existing = inventory.getItem(slot);
                if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, stack)) {
                    continue;
                }
                final int limit = Math.min(existing.getMaxStackSize(), inventory.getMaxStackSize());
                final int inserted = Math.min(remaining, limit - existing.getCount());
                if (inserted > 0 && inventory.canPlaceItem(slot, stack)) {
                    existing.grow(inserted);
                    remaining -= inserted;
                }
            }
            for (int slot = 0; slot < size && remaining > 0; slot++) {
                if (!inventory.getItem(slot).isEmpty()) {
                    continue;
                }
                final int inserted = Math.min(remaining, Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize()));
                final ItemStack insertedStack = stack.copy();
                insertedStack.setCount(inserted);
                if (inventory.canPlaceItem(slot, insertedStack)) {
                    inventory.setItem(slot, insertedStack);
                    remaining -= inserted;
                }
            }
            return remaining;
        }
    }

    private interface PlayerOperation {
        Object[] apply(ServerPlayer player) throws Exception;
    }

    public static final class ScoreboardValue extends AbstractValue {
        private final Scoreboard scoreboard;
        private final AccessContext access;

        private ScoreboardValue(final Level level, final AccessContext access) {
            this.scoreboard = level == null ? null : level.getScoreboard();
            this.access = access;
        }

        @Callback(doc = "function(team:string) -- Add a team to the scoreboard.")
        public Object[] addTeam(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            scoreboard().addPlayerTeam(args.checkString(0));
            return null;
        }

        @Callback(doc = "function(teamName:string) -- Remove a team from the scoreboard.")
        public Object[] removeTeam(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            final PlayerTeam team = scoreboard().getPlayerTeam(args.checkString(0));
            if (team != null) {
                scoreboard().removePlayerTeam(team);
            }
            return null;
        }

        @Callback(doc = "function(player:string, team:string):boolean -- Add a player to a team.")
        public Object[] addPlayerToTeam(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            final PlayerTeam team = scoreboard().getPlayerTeam(args.checkString(1));
            return new Object[]{team != null && scoreboard().addPlayerToTeam(args.checkString(0), team)};
        }

        @Callback(doc = "function(player:string):boolean -- Remove a player from their team.")
        public Object[] removePlayerFromTeams(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            return new Object[]{scoreboard().removePlayerFromTeam(args.checkString(0))};
        }

        @Callback(doc = "function(player:string, team:string):boolean -- Remove a player from a specific team.")
        public Object[] removePlayerFromTeam(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            final PlayerTeam team = scoreboard().getPlayerTeam(args.checkString(1));
            if (team != null) {
                scoreboard().removePlayerFromTeam(args.checkString(0), team);
            }
            return null;
        }

        @Callback(doc = "function(objectiveName:string, objectiveCriteria:string) -- Create a new objective for the scoreboard.")
        public Object[] addObjective(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            final String name = args.checkString(0);
            final ObjectiveCriteria criteria = ObjectiveCriteria.byName(args.checkString(1))
                .orElseThrow(() -> new IllegalArgumentException("Unknown objective criteria."));
            scoreboard().addObjective(name, criteria, Component.literal(name), criteria.getDefaultRenderType(), false, null);
            return null;
        }

        @Callback(doc = "function(objectiveName:string) -- Remove an objective from the scoreboard.")
        public Object[] removeObjective(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            final Objective objective = objective(args.checkString(0));
            scoreboard().removeObjective(objective);
            return null;
        }

        @Callback(doc = "function(playerName:string, objectiveName:string, score:number) -- Sets the score of a player for a certain objective.")
        public Object[] setPlayerScore(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            score(args.checkString(0), args.checkString(1)).set(args.checkInteger(2));
            return null;
        }

        @Callback(doc = "function(playerName:string, objectiveName:string):number -- Gets the score of a player for a certain objective.")
        public Object[] getPlayerScore(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            return new Object[]{score(args.checkString(0), args.checkString(1)).get()};
        }

        @Callback(doc = "function(playerName:string, objectiveName:string, score:number) -- Increases the score of a player for a certain objective.")
        public Object[] increasePlayerScore(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            score(args.checkString(0), args.checkString(1)).add(args.checkInteger(2));
            return null;
        }

        @Callback(doc = "function(playerName:string, objectiveName:string, score:number) -- Decrease the score of a player for a certain objective.")
        public Object[] decreasePlayerScore(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            score(args.checkString(0), args.checkString(1)).add(-args.checkInteger(2));
            return null;
        }

        private ScoreAccess score(final String playerName, final String objectiveName) throws Exception {
            return scoreboard().getOrCreatePlayerScore(ScoreHolder.forNameOnly(playerName), objective(objectiveName));
        }

        private Objective objective(final String name) throws Exception {
            final Objective objective = scoreboard().getObjective(name);
            if (objective == null) {
                throw new IllegalArgumentException("Unknown objective '" + name + "'.");
            }
            return objective;
        }

        private Scoreboard scoreboard() throws Exception {
            if (scoreboard == null) {
                throw new IllegalStateException("No scoreboard available.");
            }
            return scoreboard;
        }
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

        @Callback(doc = "function(x:number, y:number, z:number, sound:string, range:number) -- Play a sound at the specified coordinates.")
        public Object[] playSoundAt(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            if (level != null) {
                final BlockPos pos = blockPos(args);
                final ResourceLocation soundId = ResourceLocation.parse(args.checkString(3));
                final float range = args.checkInteger(4);
                level.playSound(null, pos, SoundEvent.createFixedRangeEvent(soundId, range), SoundSource.MASTER, 1F, 1F);
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
            return new Object[]{tagToTypedMap(blockEntity.saveWithFullMetadata(level.registryAccess()))};
        }

        @Callback(doc = "function(x:number, y:number, z:number, nbt:table):boolean -- Set the NBT of the block entity at the specified coordinates.")
        public Object[] setTileNBT(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            if (level == null) {
                return new Object[]{null, "no tile entity"};
            }
            final BlockPos pos = blockPos(args);
            final BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity == null) {
                return new Object[]{null, "no tile entity"};
            }
            final Tag tag = typedMapToTag(args.checkTable(3));
            if (!(tag instanceof CompoundTag compoundTag)) {
                return new Object[]{null, "nbt tag compound expected, got '" + tagTypeName(tag.getId()) + "'"};
            }
            blockEntity.loadWithComponents(compoundTag, level.registryAccess());
            blockEntity.setChanged();
            final BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, 3);
            return new Object[]{true};
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

        @Callback(doc = "function(id:string, count:number, damage:number, nbt:string, x:number, y:number, z:number, side:number):boolean -- Insert an item stack into the inventory at the specified location.")
        public Object[] insertItem(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            final Item item = item(args.checkString(0));
            final int count = args.checkInteger(1);
            args.checkInteger(2); // Legacy metadata, kept for upstream signature compatibility.
            final String tagJson = args.optString(3, "");
            final ItemStack stack = new ItemStack(item, count);
            if (!tagJson.isEmpty()) {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(TagParser.parseTag(tagJson)));
            }
            final BlockPos pos = new BlockPos(args.checkInteger(4), args.checkInteger(5), args.checkInteger(6));
            final Direction side = Direction.from3DDataValue(args.checkInteger(7));
            final IItemHandler handler = itemHandler(pos, side);
            if (handler != null) {
                return new Object[]{insertIntoHandler(handler, stack).isEmpty()};
            }
            final Container container = container(pos);
            if (container != null) {
                return new Object[]{insertIntoContainer(container, stack).isEmpty()};
            }
            return new Object[]{null, "no inventory"};
        }

        @Callback(doc = "function(x:number, y:number, z:number, slot:number[, count:number]):number -- Reduce the size of an item stack in the inventory at the specified location.")
        public Object[] removeItem(final Context context, final Arguments args) throws Exception {
            checkAccess(access);
            final BlockPos pos = blockPos(args);
            final int count = Math.max(0, args.optInteger(4, 64));
            final IItemHandler handler = itemHandler(pos, null);
            if (handler != null) {
                final int slot = checkSlot(handler.getSlots(), args.checkInteger(3));
                return new Object[]{handler.extractItem(slot, count, false).getCount()};
            }
            final Container container = container(pos);
            if (container != null) {
                final int slot = checkSlot(container.getContainerSize(), args.checkInteger(3));
                return new Object[]{container.removeItem(slot, count).getCount()};
            }
            return new Object[]{null, "no inventory"};
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

        private IItemHandler itemHandler(final BlockPos pos, final Direction side) {
            if (level == null || !level.isLoaded(pos)) {
                return null;
            }
            return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
        }

        private Container container(final BlockPos pos) {
            if (level == null || !level.isLoaded(pos)) {
                return null;
            }
            return level.getBlockEntity(pos) instanceof Container container ? container : null;
        }

        private static Block block(final Arguments args, final int index) {
            if (args.isInteger(index)) {
                return BuiltInRegistries.BLOCK.byId(args.checkInteger(index));
            }
            return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(args.checkString(index)));
        }

        private static Item item(final String id) {
            final ResourceLocation key = id.indexOf(':') >= 0 ? ResourceLocation.parse(id) : ResourceLocation.withDefaultNamespace(id);
            if (!BuiltInRegistries.ITEM.containsKey(key)) {
                throw new IllegalArgumentException("invalid item id");
            }
            return BuiltInRegistries.ITEM.get(key);
        }

        private static boolean isAir(final Arguments args, final int index) {
            return args.isInteger(index)
                ? args.checkInteger(index) == BuiltInRegistries.BLOCK.getId(Blocks.AIR)
                : "minecraft:air".equals(args.checkString(index)) || "air".equals(args.checkString(index));
        }

        private static ItemStack insertIntoHandler(final IItemHandler handler, final ItemStack stack) {
            ItemStack remaining = stack.copy();
            for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
                remaining = handler.insertItem(slot, remaining, false);
            }
            return remaining;
        }

        private static ItemStack insertIntoContainer(final Container container, final ItemStack stack) {
            final ItemStack remaining = stack.copy();
            for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
                final ItemStack existing = container.getItem(slot);
                if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, remaining)) {
                    continue;
                }
                final int limit = Math.min(existing.getMaxStackSize(), container.getMaxStackSize());
                final int inserted = Math.min(remaining.getCount(), limit - existing.getCount());
                if (inserted > 0 && container.canPlaceItem(slot, remaining)) {
                    existing.grow(inserted);
                    remaining.shrink(inserted);
                }
            }
            for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
                if (!container.getItem(slot).isEmpty()) {
                    continue;
                }
                final int inserted = Math.min(remaining.getCount(), Math.min(remaining.getMaxStackSize(), container.getMaxStackSize()));
                final ItemStack insertedStack = remaining.copy();
                insertedStack.setCount(inserted);
                if (container.canPlaceItem(slot, insertedStack)) {
                    container.setItem(slot, insertedStack);
                    remaining.shrink(inserted);
                }
            }
            container.setChanged();
            return remaining;
        }

        private static int checkSlot(final int slots, final int slot) {
            final int index = slot - 1;
            if (index < 0 || index >= slots) {
                throw new IllegalArgumentException("slot index out of bounds");
            }
            return index;
        }

        private static Map<String, Object> compoundTagValue(final CompoundTag tag) {
            final Map<String, Object> result = new LinkedHashMap<>();
            for (final String key : tag.getAllKeys()) {
                result.put(key, tagToTypedMap(tag.get(key)));
            }
            return result;
        }

        private static Map<String, Object> tagToTypedMap(final Tag tag) {
            final Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", Integer.valueOf(tag.getId()));
            result.put("value", tagValue(tag));
            return result;
        }

        private static Object tagValue(final Tag tag) {
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
                return compoundTagValue(compoundTag);
            }
            return null;
        }

        private static List<Map<String, Object>> tagToList(final ListTag tag) {
            final List<Map<String, Object>> result = new ArrayList<>(tag.size());
            for (int index = 0; index < tag.size(); index++) {
                result.add(tagToTypedMap(tag.get(index)));
            }
            return result;
        }

        private static Tag typedMapToTag(final Map<?, ?> map) {
            final Object typeValue = map.get("type");
            if (!(typeValue instanceof Number type)) {
                throw new IllegalArgumentException(typeValue == null ? "Missing NBT type." : "Illegal NBT type '" + typeValue + "'.");
            }
            final Object value = map.get("value");
            return switch (type.intValue()) {
                case Tag.TAG_BYTE -> ByteTag.valueOf(requireNumber(value).byteValue());
                case Tag.TAG_SHORT -> ShortTag.valueOf(requireNumber(value).shortValue());
                case Tag.TAG_INT -> IntTag.valueOf(requireNumber(value).intValue());
                case Tag.TAG_LONG -> LongTag.valueOf(requireNumber(value).longValue());
                case Tag.TAG_FLOAT -> FloatTag.valueOf(requireNumber(value).floatValue());
                case Tag.TAG_DOUBLE -> DoubleTag.valueOf(requireNumber(value).doubleValue());
                case Tag.TAG_BYTE_ARRAY -> new ByteArrayTag(byteArray(value));
                case Tag.TAG_STRING -> StringTag.valueOf(stringValue(value));
                case Tag.TAG_LIST -> listTag(value);
                case Tag.TAG_COMPOUND -> compoundTag(value);
                case Tag.TAG_INT_ARRAY -> new IntArrayTag(intArray(value));
                case Tag.TAG_LONG_ARRAY -> new LongArrayTag(longArray(value));
                default -> throw new IllegalArgumentException("Unsupported NBT type '" + type + "'.");
            };
        }

        private static Number requireNumber(final Object value) {
            if (value instanceof Number number) {
                return number;
            }
            throw new IllegalArgumentException("Illegal or missing value.");
        }

        private static String stringValue(final Object value) {
            if (value instanceof String string) {
                return string;
            }
            if (value instanceof byte[] bytes) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
            throw new IllegalArgumentException("Illegal or missing value.");
        }

        private static byte[] byteArray(final Object value) {
            if (value instanceof byte[] bytes) {
                return bytes;
            }
            if (value instanceof String string) {
                return string.getBytes(StandardCharsets.UTF_8);
            }
            final List<Object> values = asIndexedList(value);
            final byte[] result = new byte[values.size()];
            for (int index = 0; index < values.size(); index++) {
                result[index] = requireNumber(values.get(index)).byteValue();
            }
            return result;
        }

        private static int[] intArray(final Object value) {
            final List<Object> values = asIndexedList(value);
            final int[] result = new int[values.size()];
            for (int index = 0; index < values.size(); index++) {
                result[index] = requireNumber(values.get(index)).intValue();
            }
            return result;
        }

        private static long[] longArray(final Object value) {
            final List<Object> values = asIndexedList(value);
            final long[] result = new long[values.size()];
            for (int index = 0; index < values.size(); index++) {
                result[index] = requireNumber(values.get(index)).longValue();
            }
            return result;
        }

        private static ListTag listTag(final Object value) {
            final ListTag result = new ListTag();
            for (final Object entry : asIndexedList(value)) {
                if (!(entry instanceof Map<?, ?> map)) {
                    throw new IllegalArgumentException("Illegal value.");
                }
                result.add(typedMapToTag(map));
            }
            return result;
        }

        private static CompoundTag compoundTag(final Object value) {
            if (!(value instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException("Illegal value.");
            }
            final CompoundTag result = new CompoundTag();
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String name)) {
                    continue;
                }
                try {
                    if (!(entry.getValue() instanceof Map<?, ?> typed)) {
                        throw new IllegalArgumentException("Illegal value.");
                    }
                    result.put(name, typedMapToTag(typed));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Error converting entry '" + name + "': " + e.getMessage(), e);
                }
            }
            return result;
        }

        private static List<Object> asIndexedList(final Object value) {
            final List<Object> result = new ArrayList<>();
            if (value instanceof Collection<?> collection) {
                result.addAll(collection);
                return result;
            }
            if (value instanceof Object[] array) {
                result.addAll(List.of(array));
                return result;
            }
            if (value instanceof Map<?, ?> map) {
                map.entrySet().stream()
                    .filter(entry -> entry.getKey() instanceof Number)
                    .sorted(Comparator.comparingInt(entry -> ((Number) entry.getKey()).intValue()))
                    .map(Map.Entry::getValue)
                    .forEach(result::add);
                return result;
            }
            throw new IllegalArgumentException("Illegal or missing value.");
        }

        private static String tagTypeName(final int id) {
            return switch (id) {
                case Tag.TAG_END -> "TAG_End";
                case Tag.TAG_BYTE -> "TAG_Byte";
                case Tag.TAG_SHORT -> "TAG_Short";
                case Tag.TAG_INT -> "TAG_Int";
                case Tag.TAG_LONG -> "TAG_Long";
                case Tag.TAG_FLOAT -> "TAG_Float";
                case Tag.TAG_DOUBLE -> "TAG_Double";
                case Tag.TAG_BYTE_ARRAY -> "TAG_Byte_Array";
                case Tag.TAG_STRING -> "TAG_String";
                case Tag.TAG_LIST -> "TAG_List";
                case Tag.TAG_COMPOUND -> "TAG_Compound";
                case Tag.TAG_INT_ARRAY -> "TAG_Int_Array";
                case Tag.TAG_LONG_ARRAY -> "TAG_Long_Array";
                default -> "UNKNOWN";
            };
        }
    }

    public record AccessContext(String player, String nonce) {
    }
}
