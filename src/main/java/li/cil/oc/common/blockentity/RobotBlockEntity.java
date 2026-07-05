package li.cil.oc.common.blockentity;

import li.cil.oc.api.Driver;
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
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.ForgeEnergyStorageView;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.block.RobotBlock;
import li.cil.oc.common.menu.RobotMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.IFluidTank;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

public class RobotBlockEntity extends BlockEntity implements Robot, Container, WorldlyContainer, MenuProvider, IMenuProviderExtension, DeviceInfo, StateAware, Analyzable {
    public static final String TAG_TIER = "oc:tier";
    private static final String TAG_MACHINE = "oc:machine";
    private static final String TAG_ROBOT_NODE = "oc:robotNode";
    private static final String TAG_SELECTED_SLOT = "oc:selectedSlot";
    private static final String TAG_SELECTED_TANK = "oc:selectedTank";
    private static final String TAG_NAME = "oc:name";
    private static final String TAG_LIGHT_COLOR = "oc:lightColor";
    private static final String TAG_OWNER_NAME = "oc:ownerName";
    private static final String TAG_OWNER_UUID = "oc:ownerUUID";
    private static final int TIER_ANY = Integer.MAX_VALUE;
    private static final String SLOT_TYPE_EEPROM = "eeprom";
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
    private static final int MAX_SLOT_COUNT = slotCount(2);
    private static final MultiTank EMPTY_TANK = new MultiTank() {
        @Override
        public int tankCount() {
            return 0;
        }

        @Override
        public IFluidTank getFluidTank(final int index) {
            return null;
        }
    };

    private final Machine machine;
    private final IEnergyStorage energyStorage = new ForgeEnergyStorageView(this::connectorNode, this::energyThroughput);
    private NonNullList<ItemStack> items = NonNullList.withSize(MAX_SLOT_COUNT, ItemStack.EMPTY);
    private final Container equipmentInventory = new SimpleContainer(1);
    private final Map<String, Integer> componentSlots = new HashMap<>();
    private Node robotNode;
    private int pendingComponentSlot = -1;
    private volatile boolean pendingServerThreadChangeMark;
    private int tier;
    private int selectedSlot;
    private int selectedTank;
    private int lightColor;
    private String name = "Robot";
    private String ownerName = "";
    private UUID ownerUUID = new UUID(0L, 0L);

    public RobotBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.ROBOT.get(), pos, blockState);
        OpenComputersApi.initialize();
        machine = li.cil.oc.api.Machine.create(this);
        robotNode = createRobotNode();
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final RobotBlockEntity blockEntity) {
        blockEntity.tickServer();
    }

    public boolean toggleMachine() {
        if (machine.isRunning() || machine.isPaused()) {
            return machine.stop();
        }
        if (!canStartMachine()) {
            machine.crash("missing required components");
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
    }

    @Override
    public Machine machine() {
        return machine;
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
    }

    @Override
    public void onDisconnect(final Node node) {
    }

    @Override
    public void onMessage(final Message message) {
    }

    @Override
    public Iterable<ItemStack> internalComponents() {
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
                return items.get(slot);
            }
        };
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
        return EMPTY_TANK;
    }

    @Override
    public int selectedSlot() {
        return selectedSlot;
    }

    @Override
    public void setSelectedSlot(final int index) {
        selectedSlot = Math.clamp(index, 0, Math.max(0, getContainerSize() - 1));
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
        return null;
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

    @Callback(doc = "function(value:number):boolean -- Sets robot light color.")
    public Object[] setLightColor(final Context context, final Arguments arguments) {
        lightColor = arguments.checkInteger(0) & 0xFFFFFF;
        setChanged();
        return new Object[]{true};
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
        this.ownerUUID = ownerUUID == null ? new UUID(0L, 0L) : ownerUUID;
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
        if (isValidSlot(slot)) {
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
        return new Object[]{getContainerSize()};
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
        return new Object[]{getItem(callbackSlot(arguments, 0)).getCount()};
    }

    @Callback(direct = true, doc = "function([slot:number]):number -- Gets remaining stack space in the selected or specified slot.")
    public Object[] space(final Context context, final Arguments arguments) {
        final ItemStack stack = getItem(callbackSlot(arguments, 0));
        if (stack.isEmpty()) {
            return new Object[]{getMaxStackSize()};
        }
        return new Object[]{Math.max(0, Math.min(stack.getMaxStackSize(), getMaxStackSize()) - stack.getCount())};
    }

    @Callback(direct = true, doc = "function(slot:number):boolean -- Compares selected slot with specified slot.")
    public Object[] compareTo(final Context context, final Arguments arguments) {
        final ItemStack selected = getItem(selectedSlot);
        final ItemStack other = getItem(checkRobotSlot(arguments.checkInteger(0)));
        return new Object[]{!selected.isEmpty() && ItemStack.isSameItemSameComponents(selected, other)};
    }

    @Callback(doc = "function(slot:number[, count:number]):boolean -- Transfers items from selected slot to another slot.")
    public Object[] transferTo(final Context context, final Arguments arguments) {
        final int targetSlot = checkRobotSlot(arguments.checkInteger(0));
        if (targetSlot == selectedSlot) {
            return new Object[]{false};
        }
        final ItemStack source = getItem(selectedSlot);
        if (source.isEmpty()) {
            return new Object[]{false};
        }
        final ItemStack target = getItem(targetSlot);
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
            items.set(targetSlot, transferred);
        } else {
            target.grow(moved);
        }
        source.shrink(moved);
        if (source.isEmpty()) {
            items.set(selectedSlot, ItemStack.EMPTY);
        }
        setChanged();
        return new Object[]{true};
    }

    @Callback(doc = "function(side:number):boolean,string -- Moves the robot.")
    public Object[] move(final Context context, final Arguments arguments) {
        final Direction direction = movementDirection(facing(), arguments.checkInteger(0));
        return moveRobot(direction);
    }

    @Callback(doc = "function(clockwise:boolean):boolean -- Turns the robot.")
    public Object[] turn(final Context context, final Arguments arguments) {
        final Direction newFacing = turnedFacing(facing(), arguments.checkBoolean(0));
        if (level != null && getBlockState().hasProperty(RobotBlock.FACING)) {
            level.setBlock(worldPosition, getBlockState().setValue(RobotBlock.FACING, newFacing), 3);
        }
        setChanged();
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function(side:number):boolean,string -- Detects block state on the specified side.")
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

    @Callback(direct = true, doc = "function(side:number):boolean -- Compares selected stack with block on the specified side.")
    public Object[] compare(final Context context, final Arguments arguments) {
        if (level == null) {
            return new Object[]{false};
        }
        final ItemStack selected = getItem(selectedSlot);
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
        final ItemStack source = getItem(selectedSlot);
        if (source.isEmpty()) {
            return new Object[]{false, "empty"};
        }
        final int amount = Math.min(source.getCount(), Math.max(1, arguments.count() > 1 ? arguments.checkInteger(1) : source.getCount()));
        final ItemStack dropped = removeItem(selectedSlot, amount);
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
        final ItemStack source = getItem(selectedSlot);
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
            items.set(selectedSlot, ItemStack.EMPTY);
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
        final RobotBreakBlockEvent.Pre pre = new RobotBreakBlockEvent.Pre(this, level, target, Math.max(0.05D, hardness));
        NeoForge.EVENT_BUS.post(pre);
        if (pre.isCanceled()) {
            return new Object[]{false, "blocked"};
        }
        final ItemStack before = getItem(selectedSlot).copy();
        final ItemStack after = getItem(selectedSlot).copy();
        final RobotUsedToolEvent.ComputeDamageRate damageRate = new RobotUsedToolEvent.ComputeDamageRate(this, before, after, 1D);
        NeoForge.EVENT_BUS.post(damageRate);
        if (!level.destroyBlock(target, true)) {
            return new Object[]{false, "cannot break"};
        }
        NeoForge.EVENT_BUS.post(new RobotUsedToolEvent.ApplyDamageRate(this, before, after, damageRate.getDamageRate()));
        NeoForge.EVENT_BUS.post(new RobotBreakBlockEvent.Post(this, 0D));
        setChanged();
        return new Object[]{true};
    }

    @Callback(doc = "function(side:number):boolean,string -- Uses the selected item on the specified side.")
    public Object[] use(final Context context, final Arguments arguments) {
        if (level == null) {
            return new Object[]{false, "no world"};
        }
        final Player player = player();
        if (player == null) {
            return new Object[]{false, "no player"};
        }
        final ItemStack source = getItem(selectedSlot);
        if (source.isEmpty()) {
            return new Object[]{false, "empty"};
        }
        final Direction direction = movementDirection(facing(), arguments.checkInteger(0));
        final BlockPos target = worldPosition.relative(direction);
        final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target), direction.getOpposite(), target, false);
        final InteractionResult result = source.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        if (result.consumesAction()) {
            setChanged();
            return new Object[]{true};
        }
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

    @Override
    public int getContainerSize() {
        return slotCount(tier);
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
            notifyHardwareChanged(machine);
            notifyItemRemoved(machine, tier, slot);
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
            notifyHardwareChanged(machine);
            notifyItemRemoved(machine, tier, slot);
        }
        return removed;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }
        final ItemStack previous = items.get(slot).copy();
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
        notifyHardwareChanged(machine);
        if (!previous.isEmpty() && !ItemStack.matches(previous, items.get(slot))) {
            notifyItemRemoved(machine, tier, slot);
        }
    }

    @Override
    public boolean stillValid(final Player player) {
        return !isRemoved();
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return slotAcceptsStack(tier, slot, stack);
    }

    @Override
    public void clearContent() {
        final boolean hadCpu = hasCpuStack();
        for (int slot = 0; slot < items.size(); slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
        notifyHardwareChanged(machine);
        if (hadCpu) {
            notifyItemRemoved(machine, tier, cpuSlot(tier));
        }
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
        super.onChunkUnloaded();
        removeMachineNode();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        removeMachineNode();
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tier = normalizeTier(tag.getInt(TAG_TIER));
        selectedSlot = tag.getInt(TAG_SELECTED_SLOT);
        selectedTank = tag.getInt(TAG_SELECTED_TANK);
        lightColor = tag.getInt(TAG_LIGHT_COLOR);
        name = tag.getString(TAG_NAME).isBlank() ? "Robot" : tag.getString(TAG_NAME);
        ownerName = tag.getString(TAG_OWNER_NAME);
        ownerUUID = tag.hasUUID(TAG_OWNER_UUID) ? tag.getUUID(TAG_OWNER_UUID) : new UUID(0L, 0L);
        if (robotNode != null) {
            robotNode.load(tag.getCompound(TAG_ROBOT_NODE));
        }
        ContainerHelper.loadAllItems(tag, items, registries);
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
        machine.save(machineTag);
        tag.put(TAG_MACHINE, machineTag);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    private void tickServer() {
        if (machine.canUpdate()) {
            machine.update();
        }
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
        boolean hasCpu = false;
        boolean hasMemory = false;
        boolean hasEeprom = false;
        for (int slot = 0; slot < getContainerSize(); slot++) {
            final String expected = slotType(tier, slot);
            if (!slotAcceptsStack(tier, slot, items.get(slot))) {
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
        return hasCpu && hasMemory && hasEeprom;
    }

    private int nextComponentSlot(final int start) {
        for (int slot = start; slot < getContainerSize(); slot++) {
            if (slotAcceptsStack(tier, slot, items.get(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private boolean hasCpuStack() {
        final int slot = cpuSlot(tier);
        return slot >= 0 && slot < items.size() && !items.get(slot).isEmpty();
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
        if (pre.isCanceled()) {
            return new Object[]{false, "blocked"};
        }
        final CompoundTag saved = saveWithFullMetadata(level.registryAccess());
        saved.putInt("x", targetPos.getX());
        saved.putInt("y", targetPos.getY());
        saved.putInt("z", targetPos.getZ());
        final BlockState state = getBlockState();
        level.removeBlockEntity(worldPosition);
        level.setBlock(targetPos, state, 3);
        RobotBlockEntity eventRobot = this;
        if (level.getBlockEntity(targetPos) instanceof RobotBlockEntity movedRobot) {
            movedRobot.loadWithComponents(saved, level.registryAccess());
            movedRobot.connectMachineNode();
            eventRobot = movedRobot;
        }
        level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), 3);
        NeoForge.EVENT_BUS.post(new RobotMoveEvent.Post(eventRobot, direction));
        return new Object[]{true};
    }

    private BlockPos targetPos(final int side) {
        return worldPosition.relative(movementDirection(facing(), side));
    }

    private int insertIntoInventory(final ItemStack stack) {
        int remaining = stack.getCount();
        final int size = getContainerSize();
        final int startSlot = isValidSlot(selectedSlot) ? selectedSlot : 0;
        for (int offset = 0; offset < size && remaining > 0; offset++) {
            final int slot = (startSlot + offset) % size;
            if (!isContainerSlot(slot)) {
                continue;
            }
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
            final int slot = (startSlot + offset) % size;
            if (!isContainerSlot(slot) || !getItem(slot).isEmpty()) {
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

    private boolean isValidSlot(final int slot) {
        return slot >= 0 && slot < getContainerSize();
    }

    private boolean isContainerSlot(final int slot) {
        return Slot.Container.equals(slotType(tier, slot));
    }

    private int callbackSlot(final Arguments arguments, final int index) {
        return arguments.count() > index && arguments.checkAny(index) != null ? checkRobotSlot(arguments.checkInteger(index)) : selectedSlot;
    }

    private int checkRobotSlot(final int slot) {
        final int zeroBased = slot - 1;
        if (!isValidSlot(zeroBased)) {
            throw new IndexOutOfBoundsException("slot");
        }
        return zeroBased;
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
            case SOUTH -> 2;
            case EAST -> 3;
            default -> 0;
        };
    }

    private record RobotSlot(String type, int tier) {
        private static final RobotSlot NONE = new RobotSlot(Slot.None, -1);
    }
}
