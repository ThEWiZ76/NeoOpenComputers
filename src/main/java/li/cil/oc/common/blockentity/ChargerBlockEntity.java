package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.ForgeEnergyStorageView;
import li.cil.oc.common.ItemCharges;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.menu.ChargerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.EnumSet;
import java.util.Map;

public class ChargerBlockEntity extends BlockEntity implements Environment, SidedEnvironment, EnvironmentHost, DeviceInfo, StateAware, Container, MenuProvider {
    public static final int SLOT_CHARGEABLE = 0;
    public static final int CONTAINER_SIZE = 1;

    private static final String TAG_NODE = "oc:node";
    private static final String TAG_CHARGE_SPEED = "oc:chargeSpeed";
    private static final String TAG_HAS_POWER = "oc:hasPower";
    private static final String TAG_INVERT_SIGNAL = "oc:invertSignal";
    private static final String TAG_VISUAL_CHARGE_SPEED = "oc:visualChargeSpeed";
    private static final String TAG_VISUAL_HAS_POWER = "oc:visualHasPower";

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final Connector node;
    private final IEnergyStorage energyStorage;
    private ManagedEnvironment tabletFilesystem;
    private double chargeSpeed;
    private double clientChargeSpeed;
    private boolean hasPower;
    private boolean clientHasPower;
    private boolean invertSignal;

    public ChargerBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.CHARGER.get(), pos, blockState);
        OpenComputersApi.initialize();
        node = Network.newNode(this, Visibility.None)
            .withConnector(connectorBufferSize())
            .create();
        energyStorage = new ForgeEnergyStorageView(() -> node, ChargerBlockEntity::energyThroughput);
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public Node sidedNode(final Direction side) {
        return side == null ? null : node;
    }

    @Override
    public boolean canConnect(final Direction side) {
        return side != null;
    }

    @Override
    public void onConnect(final Node node) {
        if (node == this.node) {
            connectTabletFilesystem();
        }
    }

    @Override
    public void onDisconnect(final Node node) {
    }

    @Override
    public void onMessage(final Message message) {
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo();
    }

    @Override
    public Level world() {
        return getLevel();
    }

    @Override
    public double xPosition() {
        return worldPosition.getX() + 0.5D;
    }

    @Override
    public double yPosition() {
        return worldPosition.getY() + 0.5D;
    }

    @Override
    public double zPosition() {
        return worldPosition.getZ() + 0.5D;
    }

    @Override
    public void markChanged() {
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.neoopencomputers.charger");
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory playerInventory, final Player player) {
        return new ChargerMenu(containerId, playerInventory, this);
    }

    @Override
    public EnumSet<State> getCurrentState() {
        if (!ItemCharges.canCharge(items.get(SLOT_CHARGEABLE))) {
            return EnumSet.noneOf(State.class);
        }
        return EnumSet.of(hasPower ? State.IsWorking : State.CanWork);
    }

    public IEnergyStorage energyStorage(final Direction side) {
        return side == null ? null : energyStorage;
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.get(SLOT_CHARGEABLE).isEmpty();
    }

    @Override
    public ItemStack getItem(final int slot) {
        return slot == SLOT_CHARGEABLE ? items.get(SLOT_CHARGEABLE) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(final int slot, final int amount) {
        if (slot == SLOT_CHARGEABLE) {
            saveTabletFilesystem();
        }
        final ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
            refreshTabletFilesystem();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        if (slot == SLOT_CHARGEABLE) {
            saveTabletFilesystem();
        }
        final ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (!removed.isEmpty()) {
            refreshTabletFilesystem();
        }
        return removed;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (slot != SLOT_CHARGEABLE) {
            return;
        }
        saveTabletFilesystem();
        items.set(SLOT_CHARGEABLE, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
        refreshTabletFilesystem();
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return slot == SLOT_CHARGEABLE && ItemCharges.canCharge(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(final Player player) {
        return !isRemoved();
    }

    @Override
    public void clearContent() {
        saveTabletFilesystem();
        items.set(SLOT_CHARGEABLE, ItemStack.EMPTY);
        setChanged();
        refreshTabletFilesystem();
    }

    public void setChargeSpeed(final double value) {
        final double clamped = Mth.clamp(value, 0D, 1D);
        if (Double.compare(chargeSpeed, clamped) == 0) {
            return;
        }
        chargeSpeed = clamped;
        setChanged();
        syncVisualState();
    }

    public double chargeSpeed() {
        return chargeSpeed;
    }

    public double visualChargeSpeed() {
        return level != null && level.isClientSide ? clientChargeSpeed : chargeSpeed;
    }

    public boolean isVisuallyPowered() {
        return level != null && level.isClientSide ? clientHasPower : hasPower;
    }

    public void setInvertSignal(final boolean value) {
        invertSignal = value;
        setChanged();
    }

    public boolean invertSignal() {
        return invertSignal;
    }

    public void toggleInvertSignal() {
        invertSignal = !invertSignal;
        setChargeSpeed(1D - chargeSpeed);
    }

    public void updateChargeSpeedFromRedstone(final int signal) {
        setChargeSpeed(chargeSpeedForSignal(signal, invertSignal));
    }

    public boolean runChargeCycle() {
        if (chargeSpeed <= 0D) {
            setHasPower(false);
            return false;
        }

        final double internalCharge = ModSettings.chargerChargeRateTablet() * chargeSpeed * Math.max(1, ModSettings.mfuTickFrequency());
        final double externalCharge = ModSettings.chargerChargeRate() * chargeSpeed * Math.max(1, ModSettings.mfuTickFrequency());
        if (internalCharge <= 0D && externalCharge <= 0D) {
            setHasPower(false);
            return false;
        }

        final boolean charged =
            chargeNearbyNanomachineControllers(externalCharge) |
            chargeStack(items.get(SLOT_CHARGEABLE), internalCharge) |
            chargeNearbyPlayerEquipment(internalCharge);
        setHasPower(charged);
        if (charged) {
            setChanged();
        }
        return charged;
    }

    private boolean chargeStack(final ItemStack stack, final double charge) {
        if (!ItemCharges.canCharge(stack) || charge <= 0D) {
            return false;
        }

        final double available = ModSettings.ignorePower() ? charge : charge + node.changeBuffer(-charge);
        if (available <= 0D) {
            return false;
        }

        final double surplus = ItemCharges.charge(stack, available);
        final double accepted = Math.max(0D, available - surplus);
        if (!ModSettings.ignorePower() && surplus > 0D) {
            node.changeBuffer(surplus);
        }
        return accepted > 0D;
    }

    private boolean chargeController(final li.cil.oc.api.nanomachines.Controller controller, final double charge) {
        if (controller == null || charge <= 0D) {
            return false;
        }

        final double available = ModSettings.ignorePower() ? charge : charge + node.changeBuffer(-charge);
        if (available <= 0D) {
            return false;
        }

        final double surplus = controller.changeBuffer(available);
        final double accepted = Math.max(0D, available - surplus);
        if (!ModSettings.ignorePower() && surplus > 0D) {
            node.changeBuffer(surplus);
        }
        return accepted > 0D;
    }

    private boolean chargeNearbyNanomachineControllers(final double charge) {
        if (level == null || level.isClientSide) {
            return false;
        }

        boolean charged = false;
        final AABB bounds = new AABB(worldPosition).inflate(1D);
        for (final Player player : level.getEntitiesOfClass(Player.class, bounds, Player::isAlive)) {
            if (li.cil.oc.api.Nanomachines.hasController(player)) {
                charged |= chargeController(li.cil.oc.api.Nanomachines.getController(player), charge);
            }
        }
        return charged;
    }

    private boolean chargeNearbyPlayerEquipment(final double charge) {
        if (level == null || level.isClientSide) {
            return false;
        }

        boolean charged = false;
        final AABB bounds = new AABB(worldPosition).inflate(1D);
        for (final Player player : level.getEntitiesOfClass(Player.class, bounds, Player::isAlive)) {
            for (final ItemStack stack : player.getInventory().items) {
                charged |= chargeStack(stack, charge);
            }
        }
        return charged;
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        node.load(tag.getCompound(TAG_NODE));
        chargeSpeed = Mth.clamp(tag.getDouble(TAG_CHARGE_SPEED), 0D, 1D);
        hasPower = tag.getBoolean(TAG_HAS_POWER);
        invertSignal = tag.getBoolean(TAG_INVERT_SIGNAL);
        refreshTabletFilesystem();
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (node.address() == null) {
            Network.joinNewNetwork(node);
        }
        final CompoundTag nodeTag = new CompoundTag();
        node.save(nodeTag);
        tag.put(TAG_NODE, nodeTag);
        saveTabletFilesystem();
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putDouble(TAG_CHARGE_SPEED, chargeSpeed);
        tag.putBoolean(TAG_HAS_POWER, hasPower);
        tag.putBoolean(TAG_INVERT_SIGNAL, invertSignal);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        tag.putDouble(TAG_VISUAL_CHARGE_SPEED, chargeSpeed);
        tag.putBoolean(TAG_VISUAL_HAS_POWER, hasPower);
        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider registries) {
        if (tag.contains(TAG_VISUAL_CHARGE_SPEED)) {
            clientChargeSpeed = tag.getDouble(TAG_VISUAL_CHARGE_SPEED);
        }
        if (tag.contains(TAG_VISUAL_HAS_POWER)) {
            clientHasPower = tag.getBoolean(TAG_VISUAL_HAS_POWER);
        }
    }

    @Override
    public void onDataPacket(final Connection net, final ClientboundBlockEntityDataPacket packet, final HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        removeNode();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        removeNode();
    }

    public void removeNode() {
        removeTabletFilesystem();
        node.remove();
    }

    private void refreshTabletFilesystem() {
        removeTabletFilesystem();
        final ItemStack stack = items.get(SLOT_CHARGEABLE);
        final DriverItem driver = Driver.driverFor(stack, getClass());
        if (driver == null || !Slot.Tablet.equals(driver.slot(stack))) {
            return;
        }
        tabletFilesystem = driver.createEnvironment(stack, this);
        connectTabletFilesystem();
    }

    private void connectTabletFilesystem() {
        if (node() == null || tabletFilesystem == null || tabletFilesystem.node() == null) {
            return;
        }
        if (node().network() == null) {
            Network.joinNewNetwork(node());
        }
        if (tabletFilesystem.node() instanceof li.cil.oc.api.network.Component component) {
            component.setVisibility(Visibility.Network);
        }
        node().connect(tabletFilesystem.node());
    }

    private void saveTabletFilesystem() {
        if (tabletFilesystem == null) {
            return;
        }
        final ItemStack stack = items.get(SLOT_CHARGEABLE);
        final DriverItem driver = Driver.driverFor(stack, getClass());
        if (driver == null || !Slot.Tablet.equals(driver.slot(stack))) {
            return;
        }
        tabletFilesystem.save(driver.dataTag(stack));
    }

    private void removeTabletFilesystem() {
        if (tabletFilesystem != null && tabletFilesystem.node() != null) {
            tabletFilesystem.node().remove();
        }
        tabletFilesystem = null;
    }

    public static double connectorBufferSize() {
        return ModSettings.converterBuffer();
    }

    public static double energyThroughput() {
        return ModSettings.chargerRate();
    }

    public static double chargeSpeedForSignal(final int signal, final boolean inverted) {
        final int clamped = Mth.clamp(signal, 0, 15);
        return (inverted ? 15 - clamped : clamped) / 15D;
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final ChargerBlockEntity charger) {
        if (level.getGameTime() % Math.max(1, ModSettings.mfuTickFrequency()) == 0) {
            charger.runChargeCycle();
        }
    }

    private void setHasPower(final boolean value) {
        if (hasPower == value) {
            return;
        }
        hasPower = value;
        setChanged();
        syncVisualState();
    }

    private void syncVisualState() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static Map<String, String> deviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Charger",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
            DeviceInfo.DeviceAttribute.Product, "PowerUpper");
    }
}
