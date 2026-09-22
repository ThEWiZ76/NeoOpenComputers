package li.cil.oc.common.blockentity;

import com.mojang.authlib.GameProfile;
import li.cil.oc.api.Driver;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.Network;
import li.cil.oc.api.event.RobotBreakBlockEvent;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.event.RobotPlaceBlockEvent;
import li.cil.oc.api.event.RobotUsedToolEvent;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.internal.Keyboard;
import li.cil.oc.common.component.GraphicsCardEnvironment;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.ForgeEnergyStorageView;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.block.RobotBlock;
import li.cil.oc.common.menu.RobotMenu;
import li.cil.oc.mixin.BlockEntityPositionAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.IMenuProviderExtension;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.IFluidTank;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

public class RobotBlockEntity extends BlockEntity implements Robot, Container, WorldlyContainer, MenuProvider, IMenuProviderExtension, DeviceInfo, StateAware, Analyzable {
    public static final String TAG_TIER = "oc:tier";
    public static final String TAG_HARDWARE = "oc:hardware";
    public static final int TOOL_SLOT = 0;
    public static final int CARGO_SLOT_START = 4;
    public static final int CARGO_SLOT_COUNT = 16;
    public static final String SLOT_TYPE_TOOL = "tool";
    private static final String TAG_MACHINE = "oc:machine";
    private static final String TAG_ROBOT_NODE = "oc:robotNode";
    private static final String TAG_ROBOT_ROM = "oc:romRobot";
    private static final String TAG_SELECTED_SLOT = "oc:selectedSlot";
    private static final String TAG_SELECTED_TANK = "oc:selectedTank";
    private static final String TAG_NAME = "oc:name";
    private static final String TAG_LIGHT_COLOR = "oc:lightColor";
    private static final String TAG_RUNNING = "oc:running";
    private static final String TAG_TOOL = "oc:tool";
    private static final String TAG_OWNER_NAME = "oc:ownerName";
    private static final String TAG_OWNER_UUID = "oc:ownerUUID";
    private static final UUID NIL_UUID = new UUID(0L, 0L);
    private static final int TIER_ANY = Integer.MAX_VALUE;
    private static final String SLOT_TYPE_EEPROM = "eeprom";
    private static final int CONTAINER_RUNTIME_SLOT_START = 1;
    private static final int CONTAINER_RUNTIME_SLOT_COUNT = 3;
    private static final RobotSlot[][] CONTAINER_LAYOUTS = {
        {new RobotSlot(Slot.Container, 1), new RobotSlot(Slot.Container, 0), new RobotSlot(Slot.Container, 0)},
        {new RobotSlot(Slot.Container, 2), new RobotSlot(Slot.Container, 1), new RobotSlot(Slot.Container, 0)},
        {new RobotSlot(Slot.Container, 2), new RobotSlot(Slot.Container, 1), new RobotSlot(Slot.Container, 1)}
    };
    private static final RobotSlot[][] UPGRADE_LAYOUTS = {
        {
            new RobotSlot(Slot.Upgrade, 0),
            new RobotSlot(Slot.Upgrade, 0),
            new RobotSlot(Slot.Upgrade, 0)
        },
        {
            new RobotSlot(Slot.Upgrade, 1),
            new RobotSlot(Slot.Upgrade, 1),
            new RobotSlot(Slot.Upgrade, 1),
            new RobotSlot(Slot.Upgrade, 0),
            new RobotSlot(Slot.Upgrade, 0),
            new RobotSlot(Slot.Upgrade, 0)
        },
        {
            new RobotSlot(Slot.Upgrade, 2),
            new RobotSlot(Slot.Upgrade, 2),
            new RobotSlot(Slot.Upgrade, 2),
            new RobotSlot(Slot.Upgrade, 1),
            new RobotSlot(Slot.Upgrade, 1),
            new RobotSlot(Slot.Upgrade, 1),
            new RobotSlot(Slot.Upgrade, 0),
            new RobotSlot(Slot.Upgrade, 0),
            new RobotSlot(Slot.Upgrade, 0)
        }
    };
    private static final RobotSlot[][] COMPONENT_LAYOUTS = {
        {
            new RobotSlot(Slot.Card, 0),
            RobotSlot.NONE,
            RobotSlot.NONE,
            new RobotSlot(Slot.CPU, 0),
            new RobotSlot(Slot.Memory, 0),
            new RobotSlot(Slot.Memory, 0),
            new RobotSlot(SLOT_TYPE_EEPROM, TIER_ANY),
            new RobotSlot(Slot.HDD, 0)
        },
        {
            new RobotSlot(Slot.Card, 1),
            new RobotSlot(Slot.Card, 0),
            RobotSlot.NONE,
            new RobotSlot(Slot.CPU, 1),
            new RobotSlot(Slot.Memory, 1),
            new RobotSlot(Slot.Memory, 1),
            new RobotSlot(SLOT_TYPE_EEPROM, TIER_ANY),
            new RobotSlot(Slot.HDD, 1)
        },
        {
            new RobotSlot(Slot.Card, 2),
            new RobotSlot(Slot.Card, 1),
            new RobotSlot(Slot.Card, 1),
            new RobotSlot(Slot.CPU, 2),
            new RobotSlot(Slot.Memory, 2),
            new RobotSlot(Slot.Memory, 2),
            new RobotSlot(SLOT_TYPE_EEPROM, TIER_ANY),
            new RobotSlot(Slot.HDD, 2),
            new RobotSlot(Slot.HDD, 1)
        }
    };
    private static final int MUTABLE_SLOT_COUNT = CARGO_SLOT_START + CARGO_SLOT_COUNT;
    private static final int MAX_HARDWARE_SLOT_COUNT = slotCount(2);
    private final Machine machine;
    private boolean relocating;
    private final IEnergyStorage energyStorage = new ForgeEnergyStorageView(this::connectorNode, this::energyThroughput);
    private final MultiTank internalTanks = new MultiTank() {
        @Override
        public int tankCount() {
            return internalFluidTanks().size();
        }

        @Override
        public IFluidTank getFluidTank(final int index) {
            final List<IFluidTank> tanks = internalFluidTanks();
            return index >= 0 && index < tanks.size() ? tanks.get(index) : null;
        }
    };
    private NonNullList<ItemStack> items = NonNullList.withSize(MUTABLE_SLOT_COUNT, ItemStack.EMPTY);
    private NonNullList<ItemStack> hardwareItems = NonNullList.withSize(MAX_HARDWARE_SLOT_COUNT, ItemStack.EMPTY);
    private final Container equipmentInventory = new SimpleContainer(1);
    private final Map<String, Integer> componentSlots = new HashMap<>();
    private Node robotNode;
    private final ManagedEnvironment robotRom;
    private int pendingComponentSlot = -1;
    private volatile boolean pendingServerThreadChangeMark;
    private int tier;
    private int selectedSlot;
    private int selectedTank;
    private int lightColor = 0xF23030;
    private boolean clientRunning;
    private boolean lastSyncedRunning;
    private int lastSyncedLightColor;
    private int lastSyncedTier = -1;
    private ItemStack lastSyncedTool = ItemStack.EMPTY;
    private long animationStart;
    private int animationTicks;
    private BlockPos moveFrom;
    private int turnOffset;
    private boolean swingingTool;
    private static final java.util.concurrent.atomic.AtomicInteger NEXT_BREAKER_ID = new java.util.concurrent.atomic.AtomicInteger(-1);
    private final int breakerId = NEXT_BREAKER_ID.getAndDecrement();
    private DigTask digTask;
    private int digTicks;
    private int digStage = -1;

    private record DigTask(BlockPos origin, BlockPos target, BlockState state, ItemStack tool, int ticks) {}
    private String name = "Robot";
    private String ownerName = "";
    private UUID ownerUUID = NIL_UUID;

    public RobotBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.ROBOT.get(), pos, blockState);
        OpenComputersApi.initialize();
        machine = li.cil.oc.api.Machine.create(this);
        robotNode = createRobotNode();
        robotRom = FileSystem.asManagedEnvironment(FileSystem.fromClass(
            RobotBlockEntity.class, "neoopencomputers", "lua/component/robot"), "robot");
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final RobotBlockEntity blockEntity) {
        blockEntity.tickServer();
    }

    public boolean toggleMachine() {
        if (machine.isRunning() || machine.isPaused()) {
            return machine.stop();
        }
        if (!canStartMachine()) {
            machine.crash(missingRequirementsError());
            return false;
        }
        connectMachineNode();
        return machine.start();
    }

    @Override
    public int tier() {
        return tier;
    }

    public double energyThroughput() {
        return ModSettings.caseRate(tier);
    }

    public void setTier(final int tier) {
        this.tier = normalizeTier(tier);
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.neoopencomputers.robot.title");
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory playerInventory, final Player player) {
        return new RobotMenu(containerId, playerInventory, this);
    }

    @Override
    public void writeClientSideData(final AbstractContainerMenu menu, final RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(tier);
        buffer.writeBoolean(hasScreenHardware());
    }

    @Override
    public Machine machine() {
        return machine;
    }

    public boolean isRunningForRendering() {
        return level == null || level.isClientSide ? clientRunning : machine.isRunning() || machine.isPaused();
    }

    public int lightColor() {
        return lightColor;
    }

    public ItemStack createRobotDrop(final HolderLookup.Provider registries) {
        final ItemStack stack = new ItemStack(li.cil.oc.common.ModItems.ROBOT.get());
        final CompoundTag data = saveWithoutMetadata(registries);
        // Runtime inventory drops separately. Hardware and its saved component data stay in the chassis.
        data.remove("Items");
        final CompoundTag savedMachine = data.getCompound(TAG_MACHINE);
        savedMachine.putBoolean("running", false);
        savedMachine.remove("signals");
        savedMachine.remove("architecture");
        BlockItem.setBlockEntityData(stack, ModBlockEntities.ROBOT.get(), data);
        return stack;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG_RUNNING, isRunningForRendering());
        tag.putInt(TAG_LIGHT_COLOR, lightColor);
        tag.putInt(TAG_TIER, tier);
        tag.put(TAG_TOOL, getItem(TOOL_SLOT).saveOptional(registries));
        final CompoundTag animation = new CompoundTag();
        animation.putLong("start", animationStart);
        animation.putInt("ticks", animationTicks);
        if (moveFrom != null) animation.putLong("from", moveFrom.asLong());
        animation.putInt("turn", turnOffset);
        animation.putBoolean("swing", swingingTool);
        tag.put("oc:animation", animation);
        return tag;
    }

    @Override
    public void onDataPacket(final Connection connection, final ClientboundBlockEntityDataPacket packet, final HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider registries) {
        clientRunning = tag.getBoolean(TAG_RUNNING);
        lightColor = tag.getInt(TAG_LIGHT_COLOR) & 0xFFFFFF;
        tier = normalizeTier(tag.getInt(TAG_TIER));
        items.set(TOOL_SLOT, ItemStack.parseOptional(registries, tag.getCompound(TAG_TOOL)));
        final CompoundTag animation = tag.getCompound("oc:animation");
        animationStart = animation.getLong("start");
        animationTicks = animation.getInt("ticks");
        moveFrom = animation.contains("from") ? BlockPos.of(animation.getLong("from")) : null;
        turnOffset = animation.getInt("turn");
        swingingTool = animation.getBoolean("swing");
    }

    public Vec3 movementRenderOffset(final double gameTime) {
        if (moveFrom == null) return Vec3.ZERO;
        final double remaining = animationRemaining(gameTime);
        return new Vec3(moveFrom.getX() - worldPosition.getX(), moveFrom.getY() - worldPosition.getY(),
            moveFrom.getZ() - worldPosition.getZ()).scale(remaining);
    }

    public boolean movedFrom(final BlockPos position) {
        return position.equals(moveFrom);
    }

    public float turnRenderOffset(final double gameTime) {
        return (float) (turnOffset * animationRemaining(gameTime));
    }

    public float swingRenderOffset(final double gameTime) {
        if (!swingingTool || animationTicks <= 0) return 0F;
        final int cycles = Math.max(animationTicks / 10, 1);
        final int ticksPerCycle = animationTicks / cycles;
        final double remaining = animationRemaining(gameTime) * animationTicks / ticksPerCycle;
        return (float) (Math.sin((remaining - Math.floor(remaining)) * Math.PI) * 45D);
    }

    private double animationRemaining(final double gameTime) {
        return animationTicks <= 0 ? 0D : Math.clamp(1D - (gameTime - animationStart) / animationTicks, 0D, 1D);
    }

    private void startAnimation(final BlockPos from, final int turn, final double seconds, final boolean swing) {
        animationStart = level.getGameTime();
        animationTicks = Math.max(swing ? 5 : 1, (int) Math.min(Integer.MAX_VALUE - 2D, seconds * 20D)) + (from != null ? 2 : 0);
        moveFrom = from;
        turnOffset = turn;
        swingingTool = swing;
        final BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, 2);
    }

    @Override
    public Node node() {
        return robotNode;
    }

    public IEnergyStorage energyStorage(final Direction side) {
        return energyStorage;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.System,
            DeviceInfo.DeviceAttribute.Description, "Robot",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
            DeviceInfo.DeviceAttribute.Product, "Robot",
            DeviceInfo.DeviceAttribute.Capacity, Integer.toString(Math.max(0, getContainerSize()))
        );
    }

    @Override
    public EnumSet<StateAware.State> getCurrentState() {
        if (machine != null && machine.isRunning()) {
            return EnumSet.of(StateAware.State.IsWorking);
        }
        return EnumSet.noneOf(StateAware.State.class);
    }

    @Override
    public Node[] onAnalyze(final Player player, final Direction side, final float hitX, final float hitY, final float hitZ) {
        return robotNode == null ? new Node[]{machine.node()} : new Node[]{robotNode, machine.node()};
    }

    @Override
    public void onConnect(final Node node) {
        if (node == robotNode && robotRom != null) {
            ((li.cil.oc.api.network.Component) robotRom.node()).setVisibility(Visibility.Network);
            node.connect(robotRom.node());
        }
    }

    @Override
    public void onDisconnect(final Node node) {
        if (node == robotNode && robotRom != null) {
            robotRom.node().remove();
        }
    }

    @Override
    public void onMessage(final Message message) {
    }

    @Override
    public Iterable<ItemStack> internalComponents() {
        return hardwareItemsForMachine();
    }

    @Override
    public int componentSlot(final String address) {
        if (address == null) {
            return -1;
        }
        return componentSlots.getOrDefault(address, -1);
    }

    @Override
    public void onMachineConnect(final Node node) {
        if (node != null && node.address() != null && pendingComponentSlot >= 0) {
            componentSlots.put(node.address(), pendingComponentSlot);
        }
        pendingComponentSlot = -1;
        if (node != null && node.host() instanceof Keyboard keyboard) {
            keyboard.setUsableOverride((ignored, player) -> stillValid(player)
                && machine.canInteract(player.getGameProfile().getName()));
        }
        if (node == null || machine == null || machine.node() == null) {
            return;
        }
        for (final Node neighbor : machine.node().neighbors()) {
            if (node.host() instanceof ScreenItemEnvironment
                && (neighbor.host() instanceof Keyboard || neighbor.host() instanceof GraphicsCardEnvironment)
                || neighbor.host() instanceof ScreenItemEnvironment
                && (node.host() instanceof Keyboard || node.host() instanceof GraphicsCardEnvironment)) {
                node.connect(neighbor);
            }
        }
    }

    @Override
    public void onMachineDisconnect(final Node node) {
        if (node != null && node.address() != null) {
            componentSlots.remove(node.address());
        }
    }

    @Override
    public Level world() {
        return getLevel();
    }

    @Override
    public double xPosition() {
        return getBlockPos().getX() + 0.5D;
    }

    @Override
    public double yPosition() {
        return getBlockPos().getY() + 0.5D;
    }

    @Override
    public double zPosition() {
        return getBlockPos().getZ() + 0.5D;
    }

    @Override
    public void markChanged() {
        if (level != null && level.getServer() != null && !level.getServer().isSameThread()) {
            if (!pendingServerThreadChangeMark) {
                pendingServerThreadChangeMark = true;
                level.getServer().execute(this::markChangedOnServerThread);
            }
            return;
        }

        markChangedOnServerThread();
    }

    @Override
    public Direction facing() {
        final BlockState state = getBlockState();
        if (state.getBlock() instanceof RobotBlock && state.hasProperty(RobotBlock.FACING)) {
            return state.getValue(RobotBlock.FACING);
        }
        return Direction.NORTH;
    }

    @Override
    public Direction toGlobal(final Direction value) {
        return rotateHorizontal(value, horizontalSteps(facing()));
    }

    @Override
    public Direction toLocal(final Direction value) {
        return rotateHorizontal(value, (4 - horizontalSteps(facing())) % 4);
    }

    @Override
    public Container equipmentInventory() {
        return equipmentInventory;
    }

    @Override
    public Container mainInventory() {
        return this;
    }

    @Override
    public MultiTank tank() {
        return internalTanks;
    }

    @Override
    public int selectedSlot() {
        return selectedSlot;
    }

    @Override
    public void setSelectedSlot(final int index) {
        selectedSlot = Math.clamp(index, 0, CARGO_SLOT_COUNT - 1);
        setChanged();
    }

    @Override
    public int selectedTank() {
        return selectedTank;
    }

    @Override
    public void setSelectedTank(final int index) {
        selectedTank = Math.max(0, index);
        setChanged();
    }

    @Override
    public Player player() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        final Player player = FakePlayerFactory.get(serverLevel, robotProfile());
        player.moveTo(xPosition(), yPosition(), zPosition(), facing().toYRot(), 0F);
        return player;
    }

    @Override
    public String name() {
        return name;
    }

    @Callback(direct = true, doc = "function():string -- Gets robot name.")
    public Object[] name(final Context context, final Arguments arguments) {
        return new Object[]{name()};
    }

    @Override
    public void setName(final String name) {
        this.name = name == null || name.isBlank() ? "Robot" : name;
        setChanged();
    }

    @Callback(direct = true, doc = "function():number -- Gets robot light color.")
    public Object[] getLightColor(final Context context, final Arguments arguments) {
        return new Object[]{lightColor};
    }

    @Callback(doc = "function(value:number):number -- Sets robot light color and returns the new RGB value.")
    public Object[] setLightColor(final Context context, final Arguments arguments) {
        lightColor = arguments.checkInteger(0) & 0xFFFFFF;
        setChanged();
        if (context != null) context.pause(0.1D);
        return new Object[]{lightColor};
    }

    @Override
    public String ownerName() {
        return ownerName;
    }

    public void setOwnerName(final String ownerName) {
        this.ownerName = ownerName == null ? "" : ownerName;
        setChanged();
    }

    @Override
    public UUID ownerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(final UUID ownerUUID) {
        this.ownerUUID = ownerUUID == null ? NIL_UUID : ownerUUID;
        setChanged();
    }

    @Override
    public int componentCount() {
        return machine.componentCount();
    }

    @Override
    public Environment getComponentInSlot(final int index) {
        return null;
    }

    @Override
    public void synchronizeSlot(final int slot) {
        if (isHardwareSlot(slot)) {
            notifyHardwareChanged(machine);
            setChanged();
        }
    }

    @Override
    public boolean shouldAnimate() {
        return machine.isRunning() || machine.isPaused();
    }

    @Callback(direct = true, doc = "function():number -- Gets robot inventory size.")
    public Object[] inventorySize(final Context context, final Arguments arguments) {
        return new Object[]{CARGO_SLOT_COUNT};
    }

    @Callback(direct = true, doc = "function([slot:number]):number -- Gets or sets selected inventory slot.")
    public Object[] select(final Context context, final Arguments arguments) {
        if (arguments.count() > 0 && arguments.checkAny(0) != null) {
            setSelectedSlot(checkRobotSlot(arguments.checkInteger(0)));
        }
        return new Object[]{selectedSlot + 1};
    }

    @Callback(direct = true, doc = "function([slot:number]):number -- Gets item count in the selected or specified slot.")
    public Object[] count(final Context context, final Arguments arguments) {
        return new Object[]{selectedItem(callbackSlot(arguments, 0)).getCount()};
    }

    @Callback(direct = true, doc = "function([slot:number]):number -- Gets remaining stack space in the selected or specified slot.")
    public Object[] space(final Context context, final Arguments arguments) {
        final ItemStack stack = selectedItem(callbackSlot(arguments, 0));
        if (stack.isEmpty()) {
            return new Object[]{getMaxStackSize()};
        }
        return new Object[]{Math.max(0, Math.min(stack.getMaxStackSize(), getMaxStackSize()) - stack.getCount())};
    }

    @Callback(direct = true, doc = "function(slot:number):boolean -- Compares selected slot with specified slot.")
    public Object[] compareTo(final Context context, final Arguments arguments) {
        final ItemStack selected = selectedItem(selectedSlot);
        final ItemStack other = selectedItem(checkRobotSlot(arguments.checkInteger(0)));
        return new Object[]{!selected.isEmpty() && ItemStack.isSameItemSameComponents(selected, other)};
    }

    @Callback(doc = "function(slot:number[, count:number]):boolean -- Transfers items from selected slot to another slot.")
    public Object[] transferTo(final Context context, final Arguments arguments) {
        final int targetSlot = checkRobotSlot(arguments.checkInteger(0));
        if (targetSlot == selectedSlot) {
            return new Object[]{false};
        }
        final ItemStack source = selectedItem(selectedSlot);
        if (source.isEmpty()) {
            return new Object[]{false};
        }
        final ItemStack target = selectedItem(targetSlot);
        if (!target.isEmpty() && !ItemStack.isSameItemSameComponents(source, target)) {
            return new Object[]{false};
        }
        final int requested = Math.max(0, arguments.count() > 1 && arguments.checkAny(1) != null ? arguments.checkInteger(1) : source.getCount());
        final int limit = target.isEmpty() ? Math.min(source.getMaxStackSize(), getMaxStackSize()) : Math.min(target.getMaxStackSize(), getMaxStackSize());
        final int moved = Math.min(requested, Math.min(source.getCount(), limit - target.getCount()));
        if (moved <= 0) {
            return new Object[]{false};
        }
        if (target.isEmpty()) {
            final ItemStack transferred = source.copyWithCount(moved);
            setSelectedItem(targetSlot, transferred);
        } else {
            target.grow(moved);
        }
        source.shrink(moved);
        if (source.isEmpty()) {
            setSelectedItem(selectedSlot, ItemStack.EMPTY);
        }
        setChanged();
        return new Object[]{true};
    }

    @Callback(doc = "function(side:number):boolean,string -- Moves the robot.")
    public Object[] move(final Context context, final Arguments arguments) {
        final Direction direction = movementDirection(facing(), arguments.checkInteger(0));
        if (level != null && moveFrom != null && animationRemaining(level.getGameTime()) > 0D) {
            return new Object[]{null, "already moving"};
        }
        if (level == null || !level.isLoaded(worldPosition.relative(direction)) || !level.isEmptyBlock(worldPosition.relative(direction))) {
            final Object[] failure = moveRobot(direction);
            failure[0] = null;
            if (context != null) context.pause(0.4D);
            return failure;
        }
        final Connector connector = connectorNode();
        final double cost = ModSettings.ignorePower() ? 0D : ModSettings.robotMoveCost();
        if (cost > 0D && (connector == null || !connector.tryChangeBuffer(-cost))) {
            return new Object[]{null, "not enough energy"};
        }
        boolean moved = false;
        try {
            final Object[] result = moveRobot(direction);
            moved = Boolean.TRUE.equals(result[0]);
            if (!moved) result[0] = null;
            // This scheduler can resume sooner than upstream's; never resume within the movement animation.
            if (context != null) context.pause(moved ? Math.max(ModSettings.robotMoveDelay(), animationTicks / 20D) : 0.4D);
            return result;
        } finally {
            if (!moved && cost > 0D) connector.changeBuffer(cost);
        }
    }

    @Callback(doc = "function(clockwise:boolean):boolean -- Turns the robot.")
    public Object[] turn(final Context context, final Arguments arguments) {
        final boolean clockwise = arguments.checkBoolean(0);
        final Direction newFacing = turnedFacing(facing(), clockwise);
        if (level == null || !getBlockState().hasProperty(RobotBlock.FACING)) {
            return new Object[]{null, "no world"};
        }
        final Connector connector = connectorNode();
        final double cost = ModSettings.ignorePower() ? 0D : ModSettings.robotTurnCost();
        if (cost > 0D && (connector == null || !connector.tryChangeBuffer(-cost))) {
            return new Object[]{null, "not enough energy"};
        }
        if (!level.setBlock(worldPosition, getBlockState().setValue(RobotBlock.FACING, newFacing), 3)) {
            if (cost > 0D) connector.changeBuffer(cost);
            return new Object[]{null, "blocked"};
        }
        startAnimation(null, clockwise ? 90 : -90, ModSettings.robotTurnDelay(), false);
        if (context != null) context.pause(ModSettings.robotTurnDelay());
        setChanged();
        return new Object[]{true};
    }

    @Callback(doc = "function(side:number):boolean,string -- Detects block state on the specified side.")
    public Object[] detect(final Context context, final Arguments arguments) {
        if (level == null) {
            return new Object[]{false, "no world"};
        }
        final BlockPos target = targetPos(arguments.checkInteger(0));
        if (!level.isLoaded(target)) {
            return new Object[]{false, "target not loaded"};
        }
        final BlockState state = level.getBlockState(target);
        if (state.isAir()) {
            return new Object[]{false, "air"};
        }
        if (!state.getFluidState().isEmpty()) {
            return new Object[]{true, "liquid"};
        }
        if (state.canBeReplaced()) {
            return new Object[]{false, "replaceable"};
        }
        return new Object[]{true, "solid"};
    }

    @Callback(doc = "function(side:number):boolean -- Compares selected stack with block on the specified side.")
    public Object[] compare(final Context context, final Arguments arguments) {
        if (level == null) {
            return new Object[]{false};
        }
        final ItemStack selected = selectedItem(selectedSlot);
        if (selected.isEmpty()) {
            return new Object[]{false};
        }
        final BlockPos target = targetPos(arguments.checkInteger(0));
        if (!level.isLoaded(target)) {
            return new Object[]{false};
        }
        return new Object[]{selected.is(level.getBlockState(target).getBlock().asItem())};
    }

    @Callback(doc = "function(side:number[, count:number]):boolean,string -- Drops items from the selected slot.")
    public Object[] drop(final Context context, final Arguments arguments) {
        if (level == null) {
            return new Object[]{false, "no world"};
        }
        final ItemStack source = selectedItem(selectedSlot);
        if (source.isEmpty()) {
            return new Object[]{false, "empty"};
        }
        final int amount = Math.min(source.getCount(), Math.max(1, arguments.count() > 1 ? arguments.checkInteger(1) : source.getCount()));
        final ItemStack dropped = removeSelectedItem(selectedSlot, amount);
        if (dropped.isEmpty()) {
            return new Object[]{false, "empty"};
        }
        final BlockPos target = targetPos(arguments.checkInteger(0));
        final ItemEntity entity = new ItemEntity(level, target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, dropped);
        level.addFreshEntity(entity);
        setChanged();
        return new Object[]{true};
    }

    @Callback(doc = "function(side:number):boolean,string -- Sucks a nearby item stack into the robot inventory.")
    public Object[] suck(final Context context, final Arguments arguments) {
        if (level == null) {
            return new Object[]{false, "no world"};
        }
        final BlockPos target = targetPos(arguments.checkInteger(0));
        final AABB bounds = new AABB(target).inflate(0.5D);
        for (final ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, bounds)) {
            if (entity.isRemoved() || entity.getItem().isEmpty() || entity.hasPickUpDelay()) {
                continue;
            }
            final ItemStack stack = entity.getItem();
            final int originalCount = stack.getCount();
            final int remaining = insertIntoInventory(stack.copy());
            if (remaining >= originalCount) {
                continue;
            }
            stack.setCount(remaining);
            if (remaining <= 0) {
                entity.discard();
            } else {
                entity.setItem(stack);
            }
            setChanged();
            return new Object[]{true};
        }
        return new Object[]{false, "nothing to suck"};
    }

    @Callback(doc = "function(side:number):boolean,string -- Places the selected block item on the specified side.")
    public Object[] place(final Context context, final Arguments arguments) {
        if (level == null) {
            return new Object[]{false, "no world"};
        }
        final ItemStack source = selectedItem(selectedSlot);
        if (source.isEmpty()) {
            return new Object[]{false, "empty"};
        }
        if (!(source.getItem() instanceof BlockItem blockItem)) {
            return new Object[]{false, "not a block"};
        }
        final BlockPos target = targetPos(arguments.checkInteger(0));
        if (!level.isLoaded(target)) {
            return new Object[]{false, "target not loaded"};
        }
        final BlockState oldState = level.getBlockState(target);
        if (!oldState.isAir() && !oldState.canBeReplaced()) {
            return new Object[]{false, "blocked"};
        }
        final ItemStack placedStack = source.copyWithCount(1);
        final RobotPlaceBlockEvent.Pre pre = new RobotPlaceBlockEvent.Pre(this, placedStack, level, target);
        NeoForge.EVENT_BUS.post(pre);
        if (pre.isCanceled()) {
            return new Object[]{false, "blocked"};
        }
        final BlockState newState = blockItem.getBlock().defaultBlockState();
        if (!newState.canSurvive(level, target) || !level.setBlock(target, newState, 3)) {
            return new Object[]{false, "cannot place"};
        }
        source.shrink(1);
        if (source.isEmpty()) {
            setSelectedItem(selectedSlot, ItemStack.EMPTY);
        }
        setChanged();
        NeoForge.EVENT_BUS.post(new RobotPlaceBlockEvent.Post(this, placedStack, level, target));
        return new Object[]{true};
    }

    @Callback(doc = "function(side:number):boolean,string -- Breaks the block on the specified side.")
    public Object[] swing(final Context context, final Arguments arguments) {
        if (level == null) {
            return new Object[]{false, "no world"};
        }
        if (digTask != null) return new Object[]{false, "already swinging"};
        if (!machine.isRunning()) return new Object[]{false, "not running"};
        final BlockPos target = targetPos(arguments.checkInteger(0));
        if (!level.isLoaded(target)) {
            return new Object[]{false, "target not loaded"};
        }
        final BlockState state = level.getBlockState(target);
        if (state.isAir()) {
            return new Object[]{false, "air"};
        }
        final float hardness = state.getDestroySpeed(level, target);
        if (hardness < 0F) {
            return new Object[]{false, "unbreakable"};
        }
        if (!(player() instanceof net.minecraft.server.level.ServerPlayer player)) {
            return new Object[]{false, "no server"};
        }
        final ItemStack previousHand = player.getMainHandItem();
        final var previousGameMode = player.gameMode.getGameModeForPlayer();
        final boolean wasOnGround = player.onGround();
        final ItemStack before = getItem(TOOL_SLOT).copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, before.copy());
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        try {
            if (!state.canHarvestBlock(level, target, player)) {
                return new Object[]{false, "cannot harvest"};
            }
            // Upstream robot players mine as grounded players, including while hovering.
            player.setOnGround(true);
            final double strength = player.getDigSpeed(state, target);
            if (!(strength > 0D)) return new Object[]{false, "cannot break"};
            final double seconds = hardness * 1.5D / strength * ModSettings.robotHarvestRatio();
            if (!Double.isFinite(seconds)) return new Object[]{false, "cannot break"};
            final RobotBreakBlockEvent.Pre pre = new RobotBreakBlockEvent.Pre(this, level, target, Math.max(0.05D, seconds));
            NeoForge.EVENT_BUS.post(pre);
            if (pre.isCanceled()) {
                return new Object[]{false, "blocked"};
            }
            final double adjustedSeconds = Math.max(0.05D, pre.getBreakTime());
            if (!Double.isFinite(adjustedSeconds)) return new Object[]{false, "cannot break"};
            digTask = new DigTask(worldPosition.immutable(), target.immutable(), state, before,
                Math.max(1, (int) Math.min(Integer.MAX_VALUE, adjustedSeconds * 20D)));
            digTicks = 0;
            digStage = -1;
            if (!before.isEmpty()) startAnimation(null, 0, adjustedSeconds, true);
            if (context != null) context.pause(adjustedSeconds);
            return new Object[]{true, "block"};
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, previousHand);
            player.setGameMode(previousGameMode);
            player.setOnGround(wasOnGround);
        }
    }

    private void tickDig() {
        final DigTask task = digTask;
        if (task == null) return;
        if (!machine.isRunning() || isRemoved() || !worldPosition.equals(task.origin())
            || !level.isLoaded(task.target()) || !level.getBlockState(task.target()).equals(task.state())
            || !ItemStack.matches(getItem(TOOL_SLOT), task.tool())) {
            clearDig(true);
            return;
        }
        if (++digTicks < task.ticks()) {
            final int stage = (int) (10L * digTicks / task.ticks());
            if (stage != digStage) {
                digStage = stage;
                level.destroyBlockProgress(breakerId, task.target(), stage);
            }
            return;
        }
        clearDig(false);
        if (!(player() instanceof net.minecraft.server.level.ServerPlayer player)) return;
        final ItemStack previousHand = player.getMainHandItem();
        final var previousGameMode = player.gameMode.getGameModeForPlayer();
        final ItemStack before = getItem(TOOL_SLOT).copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, before.copy());
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        try {
            if (!task.state().canHarvestBlock(level, task.target(), player)) return;
            // Re-check ordinary block protection at completion, before producing loot.
            if (!player.gameMode.destroyBlock(task.target())) return;
            final ItemStack after = player.getMainHandItem().copy();
            if (!before.isEmpty() && !after.isEmpty() && before.is(after.getItem())) {
                final RobotUsedToolEvent.ComputeDamageRate damageRate = new RobotUsedToolEvent.ComputeDamageRate(this, before, after, ModSettings.robotItemDamageRate());
                NeoForge.EVENT_BUS.post(damageRate);
                NeoForge.EVENT_BUS.post(new RobotUsedToolEvent.ApplyDamageRate(this, before, after, damageRate.getDamageRate()));
            }
            setItem(TOOL_SLOT, after);
            // GameMode can report success even when a block refuses removal.
            if (!level.getBlockState(task.target()).equals(task.state())) {
                NeoForge.EVENT_BUS.post(new RobotBreakBlockEvent.Post(this, 0D));
            }
            setChanged();
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, previousHand);
            player.setGameMode(previousGameMode);
        }
    }

    private void clearDig(final boolean cancelled) {
        if (digTask != null && level != null && !level.isClientSide) {
            level.destroyBlockProgress(breakerId, digTask.target(), -1);
        }
        digTask = null;
        digStage = -1;
        if (cancelled && swingingTool) {
            swingingTool = false;
            animationTicks = 0;
            if (level != null && !level.isClientSide) {
                final BlockState state = getBlockState();
                level.sendBlockUpdated(worldPosition, state, state, 2);
            }
        }
    }

    @Callback(doc = "function(side:number):boolean,string -- Uses the selected item on the specified side.")
    public Object[] use(final Context context, final Arguments arguments) {
        if (level == null) {
            return new Object[]{false, "no world"};
        }
        final Player player = player();
        if (player == null) {
            return new Object[]{false, "no server"};
        }
        final ItemStack source = selectedItem(selectedSlot);
        if (source.isEmpty()) {
            return new Object[]{false, "empty"};
        }
        final Direction direction = movementDirection(facing(), arguments.checkInteger(0));
        final BlockPos target = worldPosition.relative(direction);
        final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target), direction.getOpposite(), target, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, source.copy());
        final InteractionResult result = player.getItemInHand(InteractionHand.MAIN_HAND).useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        if (result.consumesAction()) {
            setSelectedItem(selectedSlot, player.getItemInHand(InteractionHand.MAIN_HAND).copy());
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            setChanged();
            return new Object[]{true};
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return new Object[]{false, "failed"};
    }

    public static int slotCount(final int tier) {
        return containerSlotCount(tier) + upgradeSlotCount(tier) + componentSlotCount(tier);
    }

    public static int containerSlotCount(final int tier) {
        return CONTAINER_LAYOUTS[normalizeTier(tier)].length;
    }

    public static int upgradeSlotCount(final int tier) {
        return UPGRADE_LAYOUTS[normalizeTier(tier)].length;
    }

    public static int componentSlotCount(final int tier) {
        return COMPONENT_LAYOUTS[normalizeTier(tier)].length;
    }

    public static String slotType(final int tier, final int slot) {
        return slotAt(tier, slot).type();
    }

    public static int slotTier(final int tier, final int slot) {
        return slotAt(tier, slot).tier();
    }

    public static int mutableSlotCount() {
        return MUTABLE_SLOT_COUNT;
    }

    public static boolean isRuntimeMutableSlot(final int slot) {
        return slot >= 0 && slot < mutableSlotCount();
    }

    public static String mutableSlotType(final int slot) {
        if (slot == TOOL_SLOT) {
            return SLOT_TYPE_TOOL;
        }
        if (slot >= CONTAINER_RUNTIME_SLOT_START && slot < CONTAINER_RUNTIME_SLOT_START + CONTAINER_RUNTIME_SLOT_COUNT) {
            return Slot.Any;
        }
        if (slot >= CARGO_SLOT_START && slot < CARGO_SLOT_START + CARGO_SLOT_COUNT) {
            return Slot.Any;
        }
        return Slot.None;
    }

    public static int mutableSlotTier(final int slot) {
        return isRuntimeMutableSlot(slot) ? TIER_ANY : -1;
    }

    public static boolean mutableSlotAcceptsStack(final int tier, final int slot, final ItemStack stack) {
        if (!isRuntimeMutableSlot(slot) || stack.isEmpty()) {
            return false;
        }
        if (isAssemblerOnlyHardware(stack)) {
            return false; // Assembler-only hardware.
        }
        return true;
    }

    @Override
    public int getContainerSize() {
        return mutableSlotCount();
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            if (!items.get(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(final int slot) {
        return isValidSlot(slot) ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(final int slot, final int amount) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        final ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        final ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(final Player player) {
        return player != null && level != null && player.level() == level && !isRemoved()
            && level.getBlockEntity(worldPosition) == this
            && player.distanceToSqr(xPosition(), yPosition(), zPosition()) <= 64D;
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return mutableSlotAcceptsStack(tier, slot, stack);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < items.size(); slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(final Direction side) {
        final int[] slots = new int[getContainerSize()];
        for (int slot = 0; slot < slots.length; slot++) {
            slots[slot] = slot;
        }
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(final int slot, final ItemStack stack, final Direction direction) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(final int slot, final ItemStack stack, final Direction direction) {
        return isValidSlot(slot);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            Network.joinOrCreateNetwork(this);
            connectMachineNode();
        }
    }

    @Override
    public void onChunkUnloaded() {
        clearDig(true);
        super.onChunkUnloaded();
        removeMachineNode();
    }

    @Override
    public void setRemoved() {
        clearDig(true);
        super.setRemoved();
        if (!relocating) {
            removeMachineNode();
        }
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tier = normalizeTier(tag.getInt(TAG_TIER));
        selectedSlot = tag.getInt(TAG_SELECTED_SLOT);
        selectedTank = tag.getInt(TAG_SELECTED_TANK);
        lightColor = tag.contains(TAG_LIGHT_COLOR) ? tag.getInt(TAG_LIGHT_COLOR) : 0xF23030;
        name = tag.getString(TAG_NAME).isBlank() ? "Robot" : tag.getString(TAG_NAME);
        ownerName = tag.getString(TAG_OWNER_NAME);
        ownerUUID = tag.hasUUID(TAG_OWNER_UUID) ? tag.getUUID(TAG_OWNER_UUID) : NIL_UUID;
        if (robotNode != null) {
            robotNode.load(tag.getCompound(TAG_ROBOT_NODE));
        }
        if (robotRom != null) {
            robotRom.load(tag.getCompound(TAG_ROBOT_ROM));
        }
        items = NonNullList.withSize(MUTABLE_SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        hardwareItems = NonNullList.withSize(MAX_HARDWARE_SLOT_COUNT, ItemStack.EMPTY);
        if (tag.contains(TAG_HARDWARE, Tag.TAG_LIST)) {
            loadHardwareItems(tag.getList(TAG_HARDWARE, Tag.TAG_COMPOUND), hardwareItems);
        }
        notifyHardwareChanged(machine);
        machine.load(tag.getCompound(TAG_MACHINE));
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_TIER, tier);
        tag.putInt(TAG_SELECTED_SLOT, selectedSlot);
        tag.putInt(TAG_SELECTED_TANK, selectedTank);
        tag.putInt(TAG_LIGHT_COLOR, lightColor);
        tag.putString(TAG_NAME, name);
        tag.putString(TAG_OWNER_NAME, ownerName);
        tag.putUUID(TAG_OWNER_UUID, ownerUUID);
        if (robotNode != null) {
            final CompoundTag robotNodeTag = new CompoundTag();
            robotNode.save(robotNodeTag);
            tag.put(TAG_ROBOT_NODE, robotNodeTag);
        }
        final CompoundTag machineTag = new CompoundTag();
        if (robotRom != null) {
            final CompoundTag romTag = new CompoundTag();
            robotRom.save(romTag);
            tag.put(TAG_ROBOT_ROM, romTag);
        }
        machine.save(machineTag);
        tag.put(TAG_MACHINE, machineTag);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.put(TAG_HARDWARE, saveHardwareItems(hardwareItems));
    }

    private void tickServer() {
        tickDig();
        if (machine.canUpdate()) {
            machine.update();
        }
        syncVisualState();
    }

    private void syncVisualState() {
        final boolean running = isRunningForRendering();
        final ItemStack tool = getItem(TOOL_SLOT);
        if (running == lastSyncedRunning && lightColor == lastSyncedLightColor && tier == lastSyncedTier
            && ItemStack.matches(tool, lastSyncedTool)) {
            return;
        }
        lastSyncedRunning = running;
        lastSyncedLightColor = lightColor;
        lastSyncedTier = tier;
        lastSyncedTool = tool.copy();
        final BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, 2);
    }

    private void removeMachineNode() {
        if (machine.node() != null) {
            machine.node().remove();
        }
        if (robotNode != null) {
            robotNode.remove();
        }
    }

    private boolean canStartMachine() {
        return canStartMachineFromHardware();
    }

    public int missingHardwareRequirements() {
        boolean hasCpu = false;
        boolean hasMemory = false;
        boolean hasEeprom = false;
        for (int slot = 0; slot < slotCount(tier); slot++) {
            final String expected = slotType(tier, slot);
            if (!slotAcceptsStack(tier, slot, hardwareItems.get(slot))) {
                continue;
            }
            if (Slot.CPU.equals(expected)) {
                hasCpu = true;
            } else if (Slot.Memory.equals(expected)) {
                hasMemory = true;
            } else if (SLOT_TYPE_EEPROM.equals(expected)) {
                hasEeprom = true;
            }
        }
        int missing = 0;
        if (!hasCpu) {
            missing |= RobotMenu.MISSING_CPU;
        }
        if (!hasMemory) {
            missing |= RobotMenu.MISSING_MEMORY;
        }
        if (!hasEeprom) {
            missing |= RobotMenu.MISSING_EEPROM;
        }
        return missing;
    }

    public boolean hasScreenHardware() {
        return terminalScreen() != null;
    }

    public ScreenItemEnvironment terminalScreen() {
        if (machine != null && machine.node() != null) {
            for (final Node node : machine.node().neighbors()) {
                if (node.host() instanceof ScreenItemEnvironment screen) {
                    return screen;
                }
            }
        }
        return null;
    }

    private boolean canStartMachineFromHardware() {
        return missingHardwareRequirements() == 0;
    }

    private String missingRequirementsError() {
        final int missing = RobotMenu.missingRequirementsFor(this);
        final List<String> components = new ArrayList<>(3);
        if ((missing & RobotMenu.MISSING_CPU) != 0) {
            components.add("CPU");
        }
        if ((missing & RobotMenu.MISSING_MEMORY) != 0) {
            components.add("memory");
        }
        if ((missing & RobotMenu.MISSING_EEPROM) != 0) {
            components.add("EEPROM");
        }
        return components.isEmpty() ? "missing required components" : "missing " + String.join(", ", components);
    }

    private int nextComponentSlot(final int start) {
        for (int slot = start; slot < slotCount(tier); slot++) {
            if (slotAcceptsStack(tier, slot, hardwareItems.get(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private boolean hasCpuStack() {
        final int slot = cpuSlot(tier);
        return slot >= 0 && slot < hardwareItems.size() && !hardwareItems.get(slot).isEmpty();
    }

    private void markChangedOnServerThread() {
        pendingServerThreadChangeMark = false;
        super.setChanged();
    }

    private Object[] moveRobot(final Direction direction) {
        if (level == null) {
            return new Object[]{false, "no world"};
        }
        final BlockPos targetPos = worldPosition.relative(direction);
        if (!level.isLoaded(targetPos)) {
            return new Object[]{false, "target not loaded"};
        }
        if (!level.isEmptyBlock(targetPos)) {
            return new Object[]{false, "blocked"};
        }
        final RobotMoveEvent.Pre pre = new RobotMoveEvent.Pre(this, direction);
        NeoForge.EVENT_BUS.post(pre);
        if (pre.isCanceled() || !level.isEmptyBlock(targetPos)
            || level.getBlockEntity(worldPosition) != this || !level.getWorldBorder().isWithinBounds(targetPos)) {
            return new Object[]{false, "blocked"};
        }
        final BlockPos sourcePos = worldPosition;
        final BlockState state = getBlockState();
        final BlockState previousTarget = level.getBlockState(targetPos);
        // Stage the destination before touching the live robot or its inventory.
        if (!level.setBlock(targetPos, state, 2)) {
            return new Object[]{false, "blocked"};
        }
        if (!(level.getBlockEntity(targetPos) instanceof RobotBlockEntity)) {
            level.setBlock(targetPos, previousTarget, 3);
            return new Object[]{false, "blocked"};
        }
        boolean moved = false;
        relocating = true;
        try {
            level.removeBlockEntity(sourcePos);
            if (!level.setBlock(sourcePos, li.cil.oc.common.ModBlocks.ROBOT_AFTERIMAGE.get().defaultBlockState(), 2)) {
                return new Object[]{false, "blocked"};
            }
            ((BlockEntityPositionAccessor) this).neoopencomputers$setWorldPosition(targetPos.immutable());
            level.setBlockEntity(this);
            moved = level.getBlockEntity(targetPos) == this;
            if (!moved) {
                return new Object[]{false, "blocked"};
            }
        } finally {
            if (!moved) {
                ((BlockEntityPositionAccessor) this).neoopencomputers$setWorldPosition(sourcePos);
                level.setBlock(sourcePos, state, 2);
                level.setBlockEntity(this);
                level.setBlock(targetPos, previousTarget, 3);
            }
            relocating = false;
        }
        // Keep the internal computer/ROM graph alive; only world adjacency changes.
        final List<Node> oldNeighbors = new ArrayList<>();
        robotNode.neighbors().forEach(oldNeighbors::add);
        for (final Node neighbor : oldNeighbors) {
            if (neighbor != machine.node() && (robotRom == null || neighbor != robotRom.node())) {
                robotNode.disconnect(neighbor);
            }
        }
        Network.joinOrCreateNetwork(this);
        level.updateNeighborsAt(sourcePos, state.getBlock());
        level.updateNeighborsAt(targetPos, state.getBlock());
        startAnimation(sourcePos, 0, ModSettings.robotMoveDelay(), false);
        setChanged();
        NeoForge.EVENT_BUS.post(new RobotMoveEvent.Post(this, direction));
        return new Object[]{true};
    }

    private BlockPos targetPos(final int side) {
        return worldPosition.relative(movementDirection(facing(), side));
    }

    private int insertIntoInventory(final ItemStack stack) {
        int remaining = stack.getCount();
        final int size = CARGO_SLOT_COUNT;
        final int startSlot = Math.clamp(selectedSlot, 0, CARGO_SLOT_COUNT - 1);
        for (int offset = 0; offset < size && remaining > 0; offset++) {
            final int selected = (startSlot + offset) % size;
            final int slot = CARGO_SLOT_START + selected;
            final ItemStack existing = getItem(slot);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, stack)) {
                continue;
            }
            final int limit = Math.min(existing.getMaxStackSize(), getMaxStackSize());
            final int inserted = Math.min(remaining, limit - existing.getCount());
            if (inserted > 0) {
                existing.grow(inserted);
                remaining -= inserted;
            }
        }
        for (int offset = 0; offset < size && remaining > 0; offset++) {
            final int selected = (startSlot + offset) % size;
            final int slot = CARGO_SLOT_START + selected;
            if (!getItem(slot).isEmpty()) {
                continue;
            }
            final int inserted = Math.min(remaining, Math.min(stack.getMaxStackSize(), getMaxStackSize()));
            final ItemStack insertedStack = stack.copyWithCount(inserted);
            items.set(slot, insertedStack);
            remaining -= inserted;
        }
        return remaining;
    }

    private void connectMachineNode() {
        if (robotNode == null) {
            robotNode = createRobotNode();
        }
        if (robotNode == null || machine == null || machine.node() == null) {
            return;
        }
        if (robotNode.network() == null) {
            Network.joinNewNetwork(robotNode);
        }
        if (machine.node().network() == null) {
            Network.joinNewNetwork(machine.node());
        }
        machine.node().connect(robotNode);
    }

    private Node createRobotNode() {
        final var builder = Network.newNode(this, Visibility.Network);
        return builder == null ? null : builder.withComponent("robot", Visibility.Neighbors).create();
    }

    private Connector connectorNode() {
        return machine.node() instanceof Connector connector ? connector : null;
    }

    private GameProfile robotProfile() {
        final UUID uuid = NIL_UUID.equals(ownerUUID)
            ? UUID.nameUUIDFromBytes(("neoopencomputers:robot:" + worldPosition.asLong()).getBytes(StandardCharsets.UTF_8))
            : ownerUUID;
        final String profileName = ownerName == null || ownerName.isBlank() ? "[OC Robot]" : ownerName;
        return new GameProfile(uuid, profileName.length() > 16 ? profileName.substring(0, 16) : profileName);
    }

    private List<IFluidTank> internalFluidTanks() {
        if (machine == null || machine.node() == null) {
            return List.of();
        }

        final List<InternalTank> tanks = new ArrayList<>();
        for (final Node node : machine.node().neighbors()) {
            if (node == null || node.address() == null || !(node.host() instanceof IFluidTank tank)) {
                continue;
            }
            final int slot = componentSlot(node.address());
            if (!isHardwareSlot(slot) || !Slot.Upgrade.equals(slotType(tier, slot))) {
                continue;
            }
            tanks.add(new InternalTank(slot, tank));
        }

        tanks.sort(Comparator.comparingInt(InternalTank::slot));
        final List<IFluidTank> result = new ArrayList<>(tanks.size());
        for (final InternalTank tank : tanks) {
            result.add(tank.tank());
        }
        return result;
    }

    private boolean isValidSlot(final int slot) {
        return slot >= 0 && slot < getContainerSize();
    }

    private boolean isHardwareSlot(final int slot) {
        return slot >= 0 && slot < slotCount(tier);
    }

    private int callbackSlot(final Arguments arguments, final int index) {
        return arguments.count() > index && arguments.checkAny(index) != null ? checkRobotSlot(arguments.checkInteger(index)) : selectedSlot;
    }

    private int checkRobotSlot(final int slot) {
        final int zeroBased = slot - 1;
        if (zeroBased < 0 || zeroBased >= CARGO_SLOT_COUNT) {
            throw new IndexOutOfBoundsException("slot");
        }
        return zeroBased;
    }

    private ItemStack selectedItem(final int selectedSlot) {
        return getItem(CARGO_SLOT_START + selectedSlot);
    }

    private void setSelectedItem(final int selectedSlot, final ItemStack stack) {
        setItem(CARGO_SLOT_START + selectedSlot, stack);
    }

    private ItemStack removeSelectedItem(final int selectedSlot, final int amount) {
        return removeItem(CARGO_SLOT_START + selectedSlot, amount);
    }

    private Iterable<ItemStack> hardwareItemsForMachine() {
        return () -> new Iterator<>() {
            private int nextSlot = nextComponentSlot(0);

            @Override
            public boolean hasNext() {
                return nextSlot >= 0;
            }

            @Override
            public ItemStack next() {
                if (nextSlot < 0) {
                    throw new NoSuchElementException();
                }
                final int slot = nextSlot;
                nextSlot = nextComponentSlot(slot + 1);
                pendingComponentSlot = slot;
                return hardwareItems.get(slot);
            }
        };
    }

    private static void notifyHardwareChanged(final Machine machine) {
        machine.onHostChanged();
    }

    private static void notifyItemRemoved(final Machine machine, final int tier, final int slot) {
        if (Slot.CPU.equals(slotType(tier, slot))) {
            machine.stop();
        }
    }

    private static int cpuSlot(final int tier) {
        for (int slot = 0; slot < slotCount(tier); slot++) {
            if (Slot.CPU.equals(slotType(tier, slot))) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean slotAcceptsStack(final int tier, final int slot, final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (Slot.Container.equals(slotType(tier, slot))) {
            return true;
        }
        final DriverItem driver = Driver.driverFor(stack, Robot.class);
        return driver != null
            && slotType(tier, slot).equals(driver.slot(stack))
            && driver.tier(stack) <= slotTier(tier, slot);
    }

    private static boolean isAssemblerOnlyHardware(final ItemStack stack) {
        final DriverItem driver = Driver.driverFor(stack, Robot.class);
        if (driver == null) {
            return false;
        }
        final String slot = driver.slot(stack);
        return Slot.Container.equals(slot)
            || Slot.Upgrade.equals(slot)
            || Slot.Card.equals(slot)
            || Slot.CPU.equals(slot)
            || Slot.Memory.equals(slot)
            || Slot.HDD.equals(slot)
            || SLOT_TYPE_EEPROM.equals(slot);
    }

    public static ListTag saveHardwareItems(final NonNullList<ItemStack> hardwareItems) {
        final ListTag itemTags = new ListTag();
        for (int slot = 0; slot < hardwareItems.size(); slot++) {
            final ItemStack stack = hardwareItems.get(slot);
            if (!stack.isEmpty()) {
                final CompoundTag stackTag = ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, stack)
                    .result()
                    .filter(CompoundTag.class::isInstance)
                    .map(CompoundTag.class::cast)
                    .orElseGet(CompoundTag::new);
                stackTag.putByte("Slot", (byte) slot);
                itemTags.add(stackTag);
            }
        }
        return itemTags;
    }

    public static void loadHardwareItems(final ListTag itemTags, final NonNullList<ItemStack> hardwareItems) {
        for (int index = 0; index < itemTags.size(); index++) {
            final CompoundTag stackTag = itemTags.getCompound(index);
            final int slot = stackTag.getByte("Slot") & 255;
            if (slot < 0 || slot >= hardwareItems.size()) {
                continue;
            }
            final ItemStack stack = ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, stackTag)
                .result()
                .orElse(ItemStack.EMPTY);
            hardwareItems.set(slot, stack);
        }
    }

    public static Direction movementDirection(final Direction facing, final int side) {
        return rotateHorizontal(Direction.from3DDataValue(side), horizontalSteps(facing));
    }

    public static Direction turnedFacing(final Direction facing, final boolean clockwise) {
        return clockwise ? facing.getClockWise() : facing.getCounterClockWise();
    }

    private static RobotSlot slotAt(final int tier, final int slot) {
        if (slot < 0) {
            return RobotSlot.NONE;
        }
        final RobotSlot[] containers = CONTAINER_LAYOUTS[normalizeTier(tier)];
        if (slot < containers.length) {
            return containers[slot];
        }
        int relative = slot - containers.length;
        final RobotSlot[] upgrades = UPGRADE_LAYOUTS[normalizeTier(tier)];
        if (relative < upgrades.length) {
            return upgrades[relative];
        }
        relative -= upgrades.length;
        final RobotSlot[] components = COMPONENT_LAYOUTS[normalizeTier(tier)];
        if (relative < components.length) {
            return components[relative];
        }
        return RobotSlot.NONE;
    }

    private static int normalizeTier(final int tier) {
        return Math.max(0, Math.min(2, tier));
    }

    private static Direction rotateHorizontal(final Direction value, final int steps) {
        if (value.getAxis().isVertical()) {
            return value;
        }
        Direction result = value;
        for (int index = 0; index < steps; index++) {
            result = result.getClockWise();
        }
        return result;
    }

    private static int horizontalSteps(final Direction facing) {
        return switch (facing) {
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        };
    }

    private record RobotSlot(String type, int tier) {
        private static final RobotSlot NONE = new RobotSlot(Slot.None, -1);
    }

    private record InternalTank(int slot, IFluidTank tank) {
    }
}
