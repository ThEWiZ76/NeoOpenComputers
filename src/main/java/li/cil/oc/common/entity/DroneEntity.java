package li.cil.oc.common.entity;

import com.mojang.authlib.GameProfile;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.internal.Drone;
import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.common.ModEntities;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.menu.DroneMenu;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.IMenuProviderExtension;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.IFluidTank;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

public class DroneEntity extends Entity implements Drone, Environment, Container, MenuProvider, IMenuProviderExtension {
    private static final String TAG_TIER = "oc:tier";
    private static final String TAG_MACHINE = "oc:machine";
    private static final String TAG_DRONE_NODE = "oc:droneNode";
    private static final String TAG_SELECTED_SLOT = "oc:selectedSlot";
    private static final String TAG_SELECTED_TANK = "oc:selectedTank";
    private static final String TAG_STATUS_TEXT = "oc:statusText";
    private static final String TAG_LIGHT_COLOR = "oc:lightColor";
    private static final String TAG_TARGET_X = "oc:targetX";
    private static final String TAG_TARGET_Y = "oc:targetY";
    private static final String TAG_TARGET_Z = "oc:targetZ";
    private static final String TAG_ACCELERATION = "oc:acceleration";
    private static final String TAG_NAME = "oc:name";
    private static final String TAG_OWNER_NAME = "oc:ownerName";
    private static final String TAG_OWNER_UUID = "oc:ownerUUID";
    private static final UUID NIL_UUID = new UUID(0L, 0L);
    private static final int TIER_ANY = Integer.MAX_VALUE;
    private static final String SLOT_TYPE_EEPROM = "eeprom";
    private static final double MAX_ACCELERATION = 0.1D;
    private static final double MAX_VELOCITY = 0.4D;
    private static final DroneSlot[][] UPGRADE_LAYOUTS = {
        {new DroneSlot(Slot.Upgrade, 1), new DroneSlot(Slot.Upgrade, 0)},
        {new DroneSlot(Slot.Upgrade, 2), new DroneSlot(Slot.Upgrade, 1), new DroneSlot(Slot.Upgrade, 0)},
        {
            new DroneSlot(Slot.Upgrade, 2),
            new DroneSlot(Slot.Upgrade, 2),
            new DroneSlot(Slot.Upgrade, 2),
            new DroneSlot(Slot.Upgrade, 2),
            new DroneSlot(Slot.Upgrade, 2),
            new DroneSlot(Slot.Upgrade, 2),
            new DroneSlot(Slot.Upgrade, 2),
            new DroneSlot(Slot.Upgrade, 2),
            new DroneSlot(Slot.Upgrade, 2)
        }
    };
    private static final DroneSlot[][] COMPONENT_LAYOUTS = {
        {
            new DroneSlot(Slot.CPU, 0),
            new DroneSlot(Slot.Memory, 0),
            new DroneSlot(SLOT_TYPE_EEPROM, TIER_ANY),
            new DroneSlot(Slot.Card, 1),
            new DroneSlot(Slot.Card, 0)
        },
        {
            new DroneSlot(Slot.CPU, 0),
            new DroneSlot(Slot.Memory, 0),
            new DroneSlot(Slot.Memory, 0),
            new DroneSlot(SLOT_TYPE_EEPROM, TIER_ANY),
            new DroneSlot(Slot.Card, 1),
            new DroneSlot(Slot.Card, 1)
        },
        {
            new DroneSlot(Slot.CPU, 2),
            new DroneSlot(Slot.Memory, 2),
            new DroneSlot(Slot.Memory, 2),
            new DroneSlot(SLOT_TYPE_EEPROM, TIER_ANY),
            new DroneSlot(Slot.Card, 2),
            new DroneSlot(Slot.Card, 2),
            new DroneSlot(Slot.Card, 2)
        }
    };
    private static final int MAX_SLOT_COUNT = slotCount(2);

    private final Machine machine;
    private final Container equipmentInventory = new SimpleContainer(1);
    private final Container mainInventory = new SimpleContainer(0);
    private final MultiTank tank = new MultiTank() {
        @Override
        public int tankCount() {
            return 0;
        }

        @Override
        public IFluidTank getFluidTank(final int index) {
            return null;
        }
    };
    private final Map<String, Integer> componentSlots = new HashMap<>();
    private NonNullList<ItemStack> items = NonNullList.withSize(MAX_SLOT_COUNT, ItemStack.EMPTY);
    private Node droneNode;
    private int pendingComponentSlot = -1;
    private int tier;
    private int selectedSlot;
    private int selectedTank;
    private String statusText = "";
    private int lightColor = 0x66DD55;
    private Vec3 target = Vec3.ZERO;
    private double acceleration = MAX_ACCELERATION;
    private String name = "Drone";
    private String ownerName = "";
    private UUID ownerUUID = NIL_UUID;

    public DroneEntity(final EntityType<DroneEntity> entityType, final Level level) {
        super(entityType, level);
        OpenComputersApi.initialize();
        machine = li.cil.oc.api.Machine.create(this);
        droneNode = createDroneNode();
        setInvulnerable(false);
    }

    public DroneEntity(final Level level) {
        this(ModEntities.DRONE.get(), level);
    }

    @Override
    protected void defineSynchedData(final net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand) {
        if (!level().isClientSide) {
            player.openMenu(this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && machine.canUpdate()) {
            machine.update();
        }
        if (machine.isRunning()) {
            final Vec3 offset = target.subtract(position());
            if (offset.lengthSqr() > 0.000025D) {
                final Vec3 step = offset.normalize().scale(Math.min(MAX_VELOCITY, Math.min(acceleration, offset.length())));
                setDeltaMovement(step);
                move(net.minecraft.world.entity.MoverType.SELF, step);
            } else {
                setDeltaMovement(Vec3.ZERO);
            }
        }
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
        target = position();
        return machine.start();
    }

    public void loadFromItemStack(final ItemStack stack, final Player player) {
        if (stack.getItem() instanceof li.cil.oc.common.item.DroneItem item) {
            tier = normalizeTier(item.tier(stack));
            clearContent();
            for (final ItemStack component : item.componentStacks(stack)) {
                placeLoadedComponent(component.copy());
            }
            machine.onHostChanged();
        }
        if (player != null) {
            ownerName = player.getGameProfile().getName();
            ownerUUID = player.getUUID();
        }
        target = position();
        connectMachineNode();
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public Machine machine() {
        return machine;
    }

    @Override
    public Node node() {
        return droneNode;
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
        return address == null ? -1 : componentSlots.getOrDefault(address, -1);
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
    public void onConnect(final Node node) {
    }

    @Override
    public void onDisconnect(final Node node) {
    }

    @Override
    public void onMessage(final Message message) {
    }

    @Override
    public Level world() {
        return level();
    }

    @Override
    public double xPosition() {
        return getX();
    }

    @Override
    public double yPosition() {
        return getY();
    }

    @Override
    public double zPosition() {
        return getZ();
    }

    @Override
    public void markChanged() {
        setChanged();
    }

    @Override
    public void setChanged() {
    }

    @Override
    public Container equipmentInventory() {
        return equipmentInventory;
    }

    @Override
    public Container mainInventory() {
        return mainInventory;
    }

    @Override
    public MultiTank tank() {
        return tank;
    }

    @Override
    public int selectedSlot() {
        return selectedSlot;
    }

    @Override
    public void setSelectedSlot(final int index) {
        selectedSlot = Math.clamp(index, 0, Math.max(0, getContainerSize() - 1));
    }

    @Override
    public int selectedTank() {
        return selectedTank;
    }

    @Override
    public void setSelectedTank(final int index) {
        selectedTank = Math.max(0, index);
    }

    @Override
    public Player player() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        final Player player = FakePlayerFactory.get(serverLevel, droneProfile());
        player.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        return player;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name == null || name.isBlank() ? "Drone" : name;
    }

    @Override
    public String ownerName() {
        return ownerName;
    }

    @Override
    public UUID ownerUUID() {
        return ownerUUID;
    }

    @Override
    public Direction facing() {
        return Direction.SOUTH;
    }

    @Override
    public Direction toGlobal(final Direction value) {
        return value;
    }

    @Override
    public Direction toLocal(final Direction value) {
        return value;
    }

    @Override
    public Vec3 getTarget() {
        return target;
    }

    @Override
    public void setTarget(final Vec3 value) {
        target = value == null ? position() : roundedTarget(value);
    }

    @Override
    public Vec3 getVelocity() {
        return getDeltaMovement();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.neoopencomputers.drone.title");
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory playerInventory, final Player player) {
        return new DroneMenu(containerId, playerInventory, this);
    }

    @Override
    public void writeClientSideData(final AbstractContainerMenu menu, final RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(tier);
    }

    @Callback(direct = true, doc = "function():string -- Get the status text currently being displayed in the GUI.")
    public Object[] getStatusText(final Context context, final Arguments arguments) {
        return new Object[]{statusText};
    }

    @Callback(doc = "function(value:string):string -- Set the status text to display in the GUI, returns new value.")
    public Object[] setStatusText(final Context context, final Arguments arguments) {
        statusText = clampStatus(arguments.checkString(0));
        return new Object[]{statusText};
    }

    @Callback(direct = true, doc = "function():number -- Get the current color of the flap lights.")
    public Object[] getLightColor(final Context context, final Arguments arguments) {
        return new Object[]{lightColor};
    }

    @Callback(doc = "function(value:number):number -- Set the flap light RGB color.")
    public Object[] setLightColor(final Context context, final Arguments arguments) {
        lightColor = arguments.checkInteger(0) & 0xFFFFFF;
        return new Object[]{lightColor};
    }

    @Callback(doc = "function(dx:number, dy:number, dz:number) -- Change the target position by offset.")
    public Object[] move(final Context context, final Arguments arguments) {
        setTarget(target.add(arguments.checkDouble(0), arguments.checkDouble(1), arguments.checkDouble(2)));
        return null;
    }

    @Callback(direct = true, doc = "function():number -- Get distance to target.")
    public Object[] getOffset(final Context context, final Arguments arguments) {
        return new Object[]{position().distanceTo(target)};
    }

    @Callback(direct = true, doc = "function():number -- Get current velocity in m/s.")
    public Object[] getVelocity(final Context context, final Arguments arguments) {
        return new Object[]{getDeltaMovement().length() * 20D};
    }

    @Callback(direct = true, doc = "function():number -- Get maximum velocity in m/s.")
    public Object[] getMaxVelocity(final Context context, final Arguments arguments) {
        return new Object[]{MAX_VELOCITY * 20D};
    }

    @Callback(direct = true, doc = "function():number -- Get current acceleration.")
    public Object[] getAcceleration(final Context context, final Arguments arguments) {
        return new Object[]{acceleration * 20D};
    }

    @Callback(doc = "function(value:number):number -- Set acceleration.")
    public Object[] setAcceleration(final Context context, final Arguments arguments) {
        acceleration = Math.clamp(arguments.checkDouble(0) / 20D, 0D, MAX_ACCELERATION);
        return new Object[]{acceleration * 20D};
    }

    public static int slotCount(final int tier) {
        return upgradeSlotCount(tier) + componentSlotCount(tier);
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
            machine.onHostChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        return isValidSlot(slot) ? ContainerHelper.takeItem(items, slot) : ItemStack.EMPTY;
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
        machine.onHostChanged();
    }

    @Override
    public boolean stillValid(final Player player) {
        return player != null && !isRemoved() && player.distanceToSqr(this) < 64D;
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return slotAcceptsStack(tier, slot, stack);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < items.size(); slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        machine.onHostChanged();
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        tier = normalizeTier(tag.getInt(TAG_TIER));
        selectedSlot = tag.getInt(TAG_SELECTED_SLOT);
        selectedTank = tag.getInt(TAG_SELECTED_TANK);
        statusText = tag.getString(TAG_STATUS_TEXT);
        lightColor = tag.contains(TAG_LIGHT_COLOR) ? tag.getInt(TAG_LIGHT_COLOR) : 0x66DD55;
        target = new Vec3(tag.getDouble(TAG_TARGET_X), tag.getDouble(TAG_TARGET_Y), tag.getDouble(TAG_TARGET_Z));
        acceleration = tag.contains(TAG_ACCELERATION) ? tag.getDouble(TAG_ACCELERATION) : MAX_ACCELERATION;
        name = tag.getString(TAG_NAME).isBlank() ? "Drone" : tag.getString(TAG_NAME);
        ownerName = tag.getString(TAG_OWNER_NAME);
        ownerUUID = tag.hasUUID(TAG_OWNER_UUID) ? tag.getUUID(TAG_OWNER_UUID) : NIL_UUID;
        if (droneNode != null) {
            droneNode.load(tag.getCompound(TAG_DRONE_NODE));
        }
        ContainerHelper.loadAllItems(tag, items, level().registryAccess());
        machine.load(tag.getCompound(TAG_MACHINE));
        connectMachineNode();
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        tag.putInt(TAG_TIER, tier);
        tag.putInt(TAG_SELECTED_SLOT, selectedSlot);
        tag.putInt(TAG_SELECTED_TANK, selectedTank);
        tag.putString(TAG_STATUS_TEXT, statusText);
        tag.putInt(TAG_LIGHT_COLOR, lightColor);
        tag.putDouble(TAG_TARGET_X, target.x);
        tag.putDouble(TAG_TARGET_Y, target.y);
        tag.putDouble(TAG_TARGET_Z, target.z);
        tag.putDouble(TAG_ACCELERATION, acceleration);
        tag.putString(TAG_NAME, name);
        tag.putString(TAG_OWNER_NAME, ownerName);
        tag.putUUID(TAG_OWNER_UUID, ownerUUID);
        if (droneNode != null) {
            final CompoundTag droneNodeTag = new CompoundTag();
            droneNode.save(droneNodeTag);
            tag.put(TAG_DRONE_NODE, droneNodeTag);
        }
        final CompoundTag machineTag = new CompoundTag();
        machine.save(machineTag);
        tag.put(TAG_MACHINE, machineTag);
        ContainerHelper.saveAllItems(tag, items, level().registryAccess());
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        removeMachineNode();
    }

    private void placeLoadedComponent(final ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        for (int slot = 0; slot < getContainerSize(); slot++) {
            if (items.get(slot).isEmpty() && slotAcceptsStack(tier, slot, stack)) {
                items.set(slot, stack);
                return;
            }
        }
    }

    private boolean canStartMachine() {
        boolean hasCpu = false;
        boolean hasMemory = false;
        boolean hasEeprom = false;
        for (int slot = 0; slot < getContainerSize(); slot++) {
            final ItemStack stack = items.get(slot);
            final String expected = slotType(tier, slot);
            if (stack.isEmpty() || !slotAcceptsStack(tier, slot, stack)) {
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

    private void connectMachineNode() {
        if (droneNode == null) {
            droneNode = createDroneNode();
        }
        if (droneNode == null || machine.node() == null) {
            return;
        }
        if (droneNode.network() == null) {
            Network.joinNewNetwork(droneNode);
        }
        if (machine.node().network() == null) {
            Network.joinNewNetwork(machine.node());
        }
        machine.node().connect(droneNode);
    }

    private void removeMachineNode() {
        if (machine.node() != null) {
            machine.node().remove();
        }
        if (droneNode != null) {
            droneNode.remove();
        }
    }

    private Node createDroneNode() {
        final var builder = Network.newNode(this, Visibility.Network);
        return builder == null ? null : builder.withComponent("drone", Visibility.Neighbors).create();
    }

    private GameProfile droneProfile() {
        final UUID uuid = NIL_UUID.equals(ownerUUID)
            ? UUID.nameUUIDFromBytes(("neoopencomputers:drone:" + getUUID()).getBytes(StandardCharsets.UTF_8))
            : ownerUUID;
        final String profileName = ownerName == null || ownerName.isBlank() ? "[OC Drone]" : ownerName;
        return new GameProfile(uuid, profileName.length() > 16 ? profileName.substring(0, 16) : profileName);
    }

    private boolean isValidSlot(final int slot) {
        return slot >= 0 && slot < getContainerSize();
    }

    private static boolean slotAcceptsStack(final int tier, final int slot, final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        final DriverItem driver = Driver.driverFor(stack, Drone.class);
        return driver != null
            && slotType(tier, slot).equals(driver.slot(stack))
            && driver.tier(stack) <= slotTier(tier, slot);
    }

    private static DroneSlot slotAt(final int tier, final int slot) {
        if (slot < 0) {
            return DroneSlot.NONE;
        }
        final DroneSlot[] upgrades = UPGRADE_LAYOUTS[normalizeTier(tier)];
        if (slot < upgrades.length) {
            return upgrades[slot];
        }
        final int componentSlot = slot - upgrades.length;
        final DroneSlot[] components = COMPONENT_LAYOUTS[normalizeTier(tier)];
        return componentSlot >= 0 && componentSlot < components.length ? components[componentSlot] : DroneSlot.NONE;
    }

    private static int normalizeTier(final int tier) {
        return Math.max(0, Math.min(2, tier));
    }

    private static Vec3 roundedTarget(final Vec3 value) {
        return new Vec3(Math.round(value.x * 4D) / 4D, Math.round(value.y * 4D) / 4D, Math.round(value.z * 4D) / 4D);
    }

    private static String clampStatus(final String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.lines().map(line -> line.length() > 10 ? line.substring(0, 10) : line).findFirst().orElse("");
    }

    private record DroneSlot(String type, int tier) {
        private static final DroneSlot NONE = new DroneSlot(Slot.None, -1);
    }
}
