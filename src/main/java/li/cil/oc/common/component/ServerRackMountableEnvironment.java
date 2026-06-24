package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.Driver;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.internal.Server;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.menu.ServerRackMenu;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public final class ServerRackMountableEnvironment extends AbstractManagedEnvironment implements Server, Container, DeviceInfo, Analyzable {
    public static final int MISSING_CPU = 1;
    public static final int MISSING_MEMORY = 2;
    public static final int MISSING_EEPROM = 4;

    private static final String TAG_KIND = "kind";
    private static final String TAG_MACHINE = "machine";
    private static final String TAG_TIER = "tier";
    public static final String SLOT_TYPE_EEPROM = "eeprom";
    private static final int TIER_ANY = Integer.MAX_VALUE;
    private static final ServerSlot[][] SLOT_LAYOUTS = {
        {
            new ServerSlot(Slot.Card, 1),
            new ServerSlot(Slot.Card, 1),
            new ServerSlot(Slot.CPU, 1),
            new ServerSlot(Slot.ComponentBus, 1),
            new ServerSlot(Slot.Memory, 1),
            new ServerSlot(Slot.Memory, 1),
            new ServerSlot(Slot.HDD, 1),
            new ServerSlot(Slot.HDD, 1),
            new ServerSlot(SLOT_TYPE_EEPROM, TIER_ANY)
        },
        {
            new ServerSlot(Slot.Card, 2),
            new ServerSlot(Slot.Card, 1),
            new ServerSlot(Slot.CPU, 2),
            new ServerSlot(Slot.ComponentBus, 2),
            new ServerSlot(Slot.ComponentBus, 2),
            new ServerSlot(Slot.Memory, 2),
            new ServerSlot(Slot.Memory, 2),
            new ServerSlot(Slot.Memory, 2),
            new ServerSlot(Slot.HDD, 2),
            new ServerSlot(Slot.HDD, 2),
            new ServerSlot(Slot.HDD, 2),
            new ServerSlot(Slot.Card, 1),
            new ServerSlot(SLOT_TYPE_EEPROM, TIER_ANY)
        },
        {
            new ServerSlot(Slot.Card, 2),
            new ServerSlot(Slot.Card, 2),
            new ServerSlot(Slot.CPU, 2),
            new ServerSlot(Slot.ComponentBus, 2),
            new ServerSlot(Slot.ComponentBus, 2),
            new ServerSlot(Slot.ComponentBus, 2),
            new ServerSlot(Slot.Memory, 2),
            new ServerSlot(Slot.Memory, 2),
            new ServerSlot(Slot.Memory, 2),
            new ServerSlot(Slot.Memory, 2),
            new ServerSlot(Slot.HDD, 2),
            new ServerSlot(Slot.HDD, 2),
            new ServerSlot(Slot.HDD, 2),
            new ServerSlot(Slot.HDD, 2),
            new ServerSlot(Slot.Card, 1),
            new ServerSlot(Slot.Card, 1),
            new ServerSlot(SLOT_TYPE_EEPROM, TIER_ANY)
        }
    };

    private final Rack rack;
    private final int slot;
    private final int tier;
    private final Player itemOwner;
    private final CompoundTag itemData;
    private final Machine machine;
    private final NonNullList<ItemStack> items;
    private final Map<String, Integer> componentSlots = new HashMap<>();
    private final List<RackBusConnectable> busConnectables = new ArrayList<>();
    private int pendingComponentSlot = -1;
    private boolean wasWorking;

    public ServerRackMountableEnvironment(final Rack rack, final int slot, final int tier) {
        this(rack, slot, tier, null, null);
    }

    public ServerRackMountableEnvironment(final Player itemOwner, final int tier, final CompoundTag itemData) {
        this(null, -1, tier, itemOwner, itemData);
    }

    private ServerRackMountableEnvironment(final Rack rack, final int slot, final int tier, final Player itemOwner, final CompoundTag itemData) {
        OpenComputersApi.initialize();
        this.rack = rack;
        this.slot = slot;
        this.tier = Math.max(0, Math.min(2, tier));
        this.itemOwner = itemOwner;
        this.itemData = itemData;
        this.items = NonNullList.withSize(slotCount(this.tier), ItemStack.EMPTY);
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.create());
        }
        machine = li.cil.oc.api.Machine.create(this);
        if (itemData != null) {
            load(itemData);
        }
    }

    @Override
    public CompoundTag getData() {
        final CompoundTag data = new CompoundTag();
        data.putString(TAG_KIND, "server");
        data.putInt(TAG_TIER, tier);
        return data;
    }

    @Override
    public Rack rack() {
        return rack;
    }

    @Override
    public int slot() {
        return slot;
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
                final int result = nextSlot;
                nextSlot = nextComponentSlot(result + 1);
                pendingComponentSlot = result;
                return items.get(result);
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
        if (node != null && node.host() instanceof RackBusConnectable connectable && !busConnectables.contains(connectable)) {
            busConnectables.add(connectable);
        }
        pendingComponentSlot = -1;
    }

    @Override
    public void onMachineDisconnect(final Node node) {
        if (node != null && node.address() != null) {
            componentSlots.remove(node.address());
        }
        if (node != null && node.host() instanceof RackBusConnectable connectable) {
            busConnectables.remove(connectable);
        }
    }

    @Override
    public Level world() {
        if (rack != null) {
            return rack.world();
        }
        return itemOwner == null ? null : itemOwner.level();
    }

    @Override
    public double xPosition() {
        if (rack != null) {
            return rack.xPosition();
        }
        return itemOwner == null ? 0D : itemOwner.getX();
    }

    @Override
    public double yPosition() {
        if (rack != null) {
            return rack.yPosition();
        }
        return itemOwner == null ? 0D : itemOwner.getY();
    }

    @Override
    public double zPosition() {
        if (rack != null) {
            return rack.zPosition();
        }
        return itemOwner == null ? 0D : itemOwner.getZ();
    }

    @Override
    public void markChanged() {
        if (rack != null && slot >= 0) {
            rack.markChanged(slot);
        } else if (itemData != null) {
            save(itemData);
        }
    }

    @Override
    public int getConnectableCount() {
        return busConnectables.size();
    }

    @Override
    public RackBusConnectable getConnectableAt(final int index) {
        return busConnectables.get(index);
    }

    @Override
    public boolean onActivate(final Player player, final InteractionHand hand, final ItemStack heldItem, final float hitX, final float hitY) {
        if (player == null) {
            return false;
        }
        if (!player.isShiftKeyDown()) {
            player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, menuPlayer) -> new ServerRackMenu(containerId, playerInventory, this),
                ServerRackMenu.serverTitle()));
            return true;
        }
        if (!machine.isRunning() && !machine.isPaused() && stillValid(player) && canStartMachine()) {
            final boolean changed = machine.start();
            if (changed) {
                updateWorkingState();
                markChanged();
            }
        }
        return true;
    }

    public boolean controlPower(final int action) {
        final boolean changed;
        if (action == li.cil.oc.common.network.RackControlPayload.STOP || (action == li.cil.oc.common.network.RackControlPayload.TOGGLE && (machine.isRunning() || machine.isPaused()))) {
            changed = machine.stop();
        } else if (action == li.cil.oc.common.network.RackControlPayload.START || action == li.cil.oc.common.network.RackControlPayload.TOGGLE) {
            if (!canStartMachine()) {
                machine.crash("missing required components");
                return false;
            }
            changed = machine.start();
        } else {
            return false;
        }
        if (changed) {
            updateWorkingState();
            markChanged();
        }
        return changed;
    }

    @Override
    public EnumSet<StateAware.State> getCurrentState() {
        if (machine.isRunning() || machine.isPaused()) {
            return EnumSet.of(StateAware.State.IsWorking);
        }
        if (canStartMachine()) {
            return EnumSet.of(StateAware.State.CanWork);
        }
        return EnumSet.of(StateAware.State.None);
    }

    public int missingRequiredComponents() {
        boolean hasCpu = false;
        boolean hasMemory = false;
        boolean hasEeprom = false;
        for (int index = 0; index < items.size(); index++) {
            if (!canPlaceItem(index, items.get(index))) {
                continue;
            }
            final String type = slotType(index);
            if (Slot.CPU.equals(type)) {
                hasCpu = true;
            } else if (Slot.Memory.equals(type)) {
                hasMemory = true;
            } else if (SLOT_TYPE_EEPROM.equals(type)) {
                hasEeprom = true;
            }
        }

        int missing = 0;
        if (!hasCpu) {
            missing |= MISSING_CPU;
        }
        if (!hasMemory) {
            missing |= MISSING_MEMORY;
        }
        if (!hasEeprom) {
            missing |= MISSING_EEPROM;
        }
        return missing;
    }

    @Override
    public boolean canUpdate() {
        return machine.canUpdate();
    }

    @Override
    public void update() {
        machine.update();
        if (updateWorkingState()) {
            markChanged();
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.System,
            DeviceInfo.DeviceAttribute.Description, "Server",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
            DeviceInfo.DeviceAttribute.Product, "Blader",
            DeviceInfo.DeviceAttribute.Capacity, Integer.toString(getContainerSize())
        );
    }

    @Override
    public Node[] onAnalyze(final Player player, final Direction side, final float hitX, final float hitY, final float hitZ) {
        if (machine == null || machine.node() == null) {
            return new Node[0];
        }
        final LinkedHashSet<Node> nodes = new LinkedHashSet<>();
        final Node machineNode = machine.node();
        nodes.add(machineNode);
        if (machineNode.network() != null) {
            for (final Node reachable : machineNode.reachableNodes()) {
                if (reachable instanceof Component component && component.canBeSeenFrom(machineNode)) {
                    nodes.add(reachable);
                }
            }
        }
        return nodes.toArray(Node[]::new);
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        ContainerHelper.loadAllItems(nbt, items, world() == null ? null : world().registryAccess());
        if (machine != null && nbt.contains(TAG_MACHINE)) {
            machine.load(nbt.getCompound(TAG_MACHINE));
        }
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        if (world() != null) {
            ContainerHelper.saveAllItems(nbt, items, world().registryAccess());
        }
        if (machine != null) {
            final CompoundTag machineTag = new CompoundTag();
            machine.save(machineTag);
            nbt.put(TAG_MACHINE, machineTag);
        }
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (final ItemStack stack : items) {
            if (!stack.isEmpty()) {
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
            markChanged();
            notifyHardwareChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        return isValidSlot(slot) ? ContainerHelper.takeItem(items, slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (!isValidSlot(slot) || (!stack.isEmpty() && !canPlaceItem(slot, stack))) {
            return;
        }
        final ItemStack stored = stack.copy();
        if (!stored.isEmpty() && stored.getCount() > getMaxStackSize()) {
            stored.setCount(getMaxStackSize());
        }
        items.set(slot, stored);
        markChanged();
        notifyHardwareChanged();
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        if (!isValidSlot(slot) || stack.isEmpty()) {
            return false;
        }
        final DriverItem driver = Driver.driverFor(stack, getClass());
        return driver != null && slotType(slot).equals(driver.slot(stack)) && driver.tier(stack) <= slotTier(slot);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(final Player player) {
        if (rack instanceof Container container) {
            return container.stillValid(player) && rack.getMountable(slot) == this;
        }
        return itemOwner != null && player == itemOwner;
    }

    @Override
    public void setChanged() {
        markChanged();
    }

    @Override
    public void clearContent() {
        for (int index = 0; index < items.size(); index++) {
            items.set(index, ItemStack.EMPTY);
        }
        markChanged();
        notifyHardwareChanged();
    }

    private int nextComponentSlot(final int start) {
        for (int index = start; index < items.size(); index++) {
            if (canPlaceItem(index, items.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private boolean isValidSlot(final int slot) {
        return slot >= 0 && slot < items.size();
    }

    public String slotTypeName(final int slot) {
        return slotTypeName(tier, slot);
    }

    public int slotTierLimit(final int slot) {
        return slotTierLimit(tier, slot);
    }

    public static int maxSlotCount() {
        return slotLayout(SLOT_LAYOUTS.length - 1).length;
    }

    public static int slotCountForTier(final int tier) {
        return slotCount(tier);
    }

    public static String slotTypeName(final int tier, final int slot) {
        final ServerSlot[] layout = slotLayout(tier);
        return slot >= 0 && slot < layout.length ? layout[slot].type() : Slot.None;
    }

    public static int slotTierLimit(final int tier, final int slot) {
        final ServerSlot[] layout = slotLayout(tier);
        return slot >= 0 && slot < layout.length ? layout[slot].tier() : -1;
    }

    private String slotType(final int slot) {
        return slotTypeName(slot);
    }

    private int slotTier(final int slot) {
        return slotTierLimit(slot);
    }

    private static int slotCount(final int tier) {
        return slotLayout(tier).length;
    }

    private static ServerSlot[] slotLayout(final int tier) {
        return SLOT_LAYOUTS[Math.clamp(tier, 0, SLOT_LAYOUTS.length - 1)];
    }

    private void notifyHardwareChanged() {
        busConnectables.clear();
        if (machine != null) {
            machine.onHostChanged();
        }
        updateWorkingState();
    }

    private boolean canStartMachine() {
        return missingRequiredComponents() == 0;
    }

    private boolean updateWorkingState() {
        final boolean working = machine.isRunning() || machine.isPaused();
        if (wasWorking == working) {
            return false;
        }
        wasWorking = working;
        return true;
    }

    private record ServerSlot(String type, int tier) {
    }
}
