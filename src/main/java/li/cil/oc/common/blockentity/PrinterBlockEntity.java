package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.item.data.PrintData;
import li.cil.oc.common.menu.PrinterMenu;
import li.cil.oc.common.util.AssemblerWork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.Map;

public class PrinterBlockEntity extends BlockEntity implements ManagedEnvironment, SidedEnvironment, EnvironmentHost, Container, DeviceInfo, StateAware, MenuProvider {
    public static final int SLOT_MATERIAL = 0;
    public static final int SLOT_INK = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int CONTAINER_SIZE = 3;

    private static final String COMPONENT_NAME = "printer3d";
    private static final String TAG_NODE = "node";
    private static final String TAG_ITEMS = "items";
    private static final String TAG_DATA = "data";
    private static final String TAG_ACTIVE = "active";
    private static final String TAG_LIMIT = "limit";
    private static final String TAG_MATERIAL = "amountMaterial";
    private static final String TAG_INK = "amountInk";
    private static final String TAG_OUTPUT = "output";
    private static final String TAG_TOTAL_ENERGY = "total";
    private static final String TAG_REMAINING_ENERGY = "remaining";
    public static final int MAX_MATERIAL = 256_000;
    public static final int MAX_INK = 100_000;
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Printer,
        DeviceInfo.DeviceAttribute.Description, "3D Printer",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
        DeviceInfo.DeviceAttribute.Product, "Omni-Materializer T6.1"
    );

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private Node node;
    private PrintData data = new PrintData();
    private boolean active;
    private int limit;
    private int amountMaterial;
    private int amountInk;
    private ItemStack pendingOutput = ItemStack.EMPTY;
    private double totalRequiredEnergy;
    private double requiredEnergy;

    public PrinterBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.PRINTER.get(), pos, blockState);
        OpenComputersApi.initialize();
        node = createNode(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.neoopencomputers.printer");
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory playerInventory, final Player player) {
        return new PrinterMenu(containerId, playerInventory, this);
    }

    @Override
    public Node node() {
        if (node == null) {
            node = createNode(this);
        }
        return node;
    }

    @Override
    public Node sidedNode(final Direction side) {
        return canConnect(side) ? node() : null;
    }

    @Override
    public boolean canConnect(final Direction side) {
        return side != Direction.UP;
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
        setChanged();
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        tickPrinting();
    }

    @Callback(doc = "function() -- Resets the configuration of the printer and stops accepting new jobs.")
    public Object[] reset(final Context context, final Arguments args) {
        data = new PrintData();
        active = false;
        setChanged();
        return null;
    }

    @Callback(doc = "function(value:string) -- Set a label for the block being printed.")
    public Object[] setLabel(final Context context, final Arguments args) {
        data.setLabel(clampedOptional(args.optString(0, null), 24));
        active = false;
        setChanged();
        return null;
    }

    @Callback(doc = "function():string -- Get the current label for the block being printed.")
    public Object[] getLabel(final Context context, final Arguments args) {
        return new Object[]{data.label()};
    }

    @Callback(doc = "function(value:string) -- Set a tooltip for the block being printed.")
    public Object[] setTooltip(final Context context, final Arguments args) {
        data.setTooltip(clampedOptional(args.optString(0, null), 128));
        active = false;
        setChanged();
        return null;
    }

    @Callback(doc = "function():string -- Get the current tooltip for the block being printed.")
    public Object[] getTooltip(final Context context, final Arguments args) {
        return new Object[]{data.tooltip()};
    }

    @Callback(doc = "function(value:number) -- Set what light level the printed block should have.")
    public Object[] setLightLevel(final Context context, final Arguments args) {
        data.setLightLevel(clamp(args.checkInteger(0), 0, ModSettings.printerMaxBaseLightLevel()));
        active = false;
        setChanged();
        return null;
    }

    @Callback(doc = "function():number -- Get which light level the printed block should have.")
    public Object[] getLightLevel(final Context context, final Arguments args) {
        return new Object[]{data.lightLevel()};
    }

    @Callback(doc = "function(value:boolean or number) -- Set whether the printed block should emit redstone when active.")
    public Object[] setRedstoneEmitter(final Context context, final Arguments args) {
        data.setRedstoneLevel(args.isBoolean(0) ? (args.checkBoolean(0) ? 15 : 0) : clamp(args.checkInteger(0), 0, 15));
        active = false;
        setChanged();
        return null;
    }

    @Callback(doc = "function():boolean, number -- Get whether the printed block should emit redstone when active.")
    public Object[] isRedstoneEmitter(final Context context, final Arguments args) {
        return new Object[]{data.emitRedstone(), data.redstoneLevel()};
    }

    @Callback(doc = "function(value:boolean) -- Set whether the printed block should automatically return to its off state.")
    public Object[] setButtonMode(final Context context, final Arguments args) {
        data.setButtonMode(args.checkBoolean(0));
        active = false;
        setChanged();
        return null;
    }

    @Callback(doc = "function():boolean -- Get whether the printed block should automatically return to its off state.")
    public Object[] isButtonMode(final Context context, final Arguments args) {
        return new Object[]{data.isButtonMode()};
    }

    @Callback(doc = "function(collideOff:boolean, collideOn:boolean) -- Set whether the printed block should be collidable or not.")
    public Object[] setCollidable(final Context context, final Arguments args) {
        final boolean collideOff = args.checkBoolean(0);
        final boolean collideOn = args.checkBoolean(1);
        data.setNoclipOff(!collideOff);
        data.setNoclipOn(!collideOn);
        setChanged();
        return null;
    }

    @Callback(doc = "function():boolean, boolean -- Get whether the printed block should be collidable or not.")
    public Object[] isCollidable(final Context context, final Arguments args) {
        return new Object[]{!data.isNoclipOff(), !data.isNoclipOn()};
    }

    @Callback(doc = "function(minX:number, minY:number, minZ:number, maxX:number, maxY:number, maxZ:number, texture:string[, state:boolean=false][,tint:number]) -- Adds a shape to the printer configuration.")
    public Object[] addShape(final Context context, final Arguments args) {
        if (data.stateOff().size() > ModSettings.printerMaxShapes() || data.stateOn().size() > ModSettings.printerMaxShapes()) {
            return new Object[]{null, "model too complex"};
        }

        final double minX = clamp(args.checkInteger(0), 0, 16) / 16D;
        final double minY = clamp(args.checkInteger(1), 0, 16) / 16D;
        final double minZ = (16 - clamp(args.checkInteger(2), 0, 16)) / 16D;
        final double maxX = clamp(args.checkInteger(3), 0, 16) / 16D;
        final double maxY = clamp(args.checkInteger(4), 0, 16) / 16D;
        final double maxZ = (16 - clamp(args.checkInteger(5), 0, 16)) / 16D;
        final String rawTexture = args.checkString(6);
        final String texture = rawTexture.length() <= 64 ? rawTexture : rawTexture.substring(0, 64);
        final boolean state = args.count() > 7 && args.checkAny(7) != null && args.isBoolean(7) && args.checkBoolean(7);
        final Integer tint;
        if (args.count() > 7 && args.checkAny(7) != null && args.isInteger(7)) {
            tint = Integer.valueOf(args.checkInteger(7));
        } else if (args.count() > 8 && args.checkAny(8) != null && args.isInteger(8)) {
            tint = Integer.valueOf(args.checkInteger(8));
        } else {
            tint = null;
        }

        if (minX == maxX || minY == maxY || minZ == maxZ) {
            throw new IllegalArgumentException("empty block");
        }

        final PrintData.Shape shape = new PrintData.Shape(new AABB(
            Math.min(minX, maxX),
            Math.min(minY, maxY),
            Math.min(minZ, maxZ),
            Math.max(maxX, minX),
            Math.max(maxY, minY),
            Math.max(maxZ, minZ)), texture, tint);
        if (state) {
            data.addStateOn(shape);
        } else {
            data.addStateOff(shape);
        }
        active = false;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return new Object[]{true};
    }

    @Callback(doc = "function():number -- Get the number of shapes in the current configuration.")
    public Object[] getShapeCount(final Context context, final Arguments args) {
        return new Object[]{data.stateOff().size(), data.stateOn().size()};
    }

    @Callback(doc = "function():number -- Get the maximum allowed number of shapes.")
    public Object[] getMaxShapeCount(final Context context, final Arguments args) {
        return new Object[]{ModSettings.printerMaxShapes()};
    }

    @Callback(doc = "function([count:number]):boolean -- Commit and begin printing the current configuration.")
    public Object[] commit(final Context context, final Arguments args) {
        if (!canPrint()) {
            return new Object[]{null, "model invalid"};
        }
        limit = Math.max(0, Math.min((int) args.optDouble(0, 1D), Integer.MAX_VALUE));
        active = limit > 0;
        setChanged();
        return new Object[]{true};
    }

    @Callback(doc = "function():string, number or boolean -- The current state of the printer.")
    public Object[] status(final Context context, final Arguments args) {
        if (isPrinting()) {
            return new Object[]{"busy", progress()};
        }
        return new Object[]{"idle", canPrint()};
    }

    @Callback(doc = "function():number -- The estimated remaining print time in seconds.")
    public Object[] timeRemaining(final Context context, final Arguments args) {
        return new Object[]{(int) (requiredEnergy / ModSettings.assemblerTickAmount() / 20D)};
    }

    public boolean canPrint() {
        return !data.stateOff().isEmpty()
            && data.stateOff().size() <= ModSettings.printerMaxShapes()
            && data.stateOn().size() <= ModSettings.printerMaxShapes();
    }

    public boolean isActive() {
        return active;
    }

    public boolean isPrinting() {
        return !pendingOutput.isEmpty();
    }

    @Override
    public EnumSet<StateAware.State> getCurrentState() {
        if (isPrinting()) {
            return EnumSet.of(StateAware.State.IsWorking);
        }
        if (canPrint()) {
            return EnumSet.of(StateAware.State.CanWork);
        }
        return EnumSet.noneOf(StateAware.State.class);
    }

    public double progress() {
        if (totalRequiredEnergy <= 0D) {
            return 100D;
        }
        return (1D - requiredEnergy / totalRequiredEnergy) * 100D;
    }

    public int amountMaterial() {
        return amountMaterial;
    }

    public int amountInk() {
        return amountInk;
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final PrinterBlockEntity printer) {
        printer.tickPrinting();
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
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
        final ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        return ContainerHelper.takeItem(items, slot);
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
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (slot == SLOT_MATERIAL) {
            return PrintData.materialValue(stack) > 0 && MAX_MATERIAL - amountMaterial >= PrintData.materialValue(stack);
        }
        if (slot == SLOT_INK) {
            return PrintData.inkValue(stack) > 0 && MAX_INK - amountInk >= PrintData.inkValue(stack);
        }
        return false;
    }

    @Override
    public boolean stillValid(final Player player) {
        return !isRemoved();
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < items.size(); slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
    }

    @Override
    public void load(final CompoundTag nbt) {
        if (nbt.contains(TAG_NODE) && node() != null) {
            node().load(nbt.getCompound(TAG_NODE));
        }
        data.load(nbt.getCompound(TAG_DATA));
        active = nbt.getBoolean(TAG_ACTIVE);
        limit = nbt.getInt(TAG_LIMIT);
        amountMaterial = nbt.getInt(TAG_MATERIAL);
        amountInk = nbt.getInt(TAG_INK);
        totalRequiredEnergy = nbt.getDouble(TAG_TOTAL_ENERGY);
        requiredEnergy = nbt.getDouble(TAG_REMAINING_ENERGY);
    }

    @Override
    public void save(final CompoundTag nbt) {
        if (node() != null) {
            final CompoundTag nodeTag = new CompoundTag();
            if (node().address() == null) {
                Network.joinNewNetwork(node());
                node().save(nodeTag);
                node().remove();
                node = createNode(this);
            } else {
                node().save(nodeTag);
            }
            nbt.put(TAG_NODE, nodeTag);
        }
        final CompoundTag dataTag = new CompoundTag();
        data.save(dataTag);
        nbt.put(TAG_DATA, dataTag);
        nbt.putBoolean(TAG_ACTIVE, active);
        nbt.putInt(TAG_LIMIT, limit);
        nbt.putInt(TAG_MATERIAL, amountMaterial);
        nbt.putInt(TAG_INK, amountInk);
        nbt.putDouble(TAG_TOTAL_ENERGY, totalRequiredEnergy);
        nbt.putDouble(TAG_REMAINING_ENERGY, requiredEnergy);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag.getCompound(TAG_ITEMS), items, registries);
        pendingOutput = tag.contains(TAG_OUTPUT)
            ? ItemStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get(TAG_OUTPUT)).result().orElse(ItemStack.EMPTY)
            : ItemStack.EMPTY;
        load(tag);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        final CompoundTag itemsTag = new CompoundTag();
        ContainerHelper.saveAllItems(itemsTag, items, registries);
        tag.put(TAG_ITEMS, itemsTag);
        if (!pendingOutput.isEmpty()) {
            ItemStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), pendingOutput)
                .result()
                .ifPresent(outputTag -> tag.put(TAG_OUTPUT, outputTag));
        }
        save(tag);
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

    private boolean isValidSlot(final int slot) {
        return slot >= 0 && slot < items.size();
    }

    private static String clampedOptional(final String value, final int maxLength) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Node createNode(final ManagedEnvironment host) {
        final var builder = Network.newNode(host, Visibility.Network);
        return builder == null ? null : builder.withComponent(COMPONENT_NAME, Visibility.Network).withConnector(ModSettings.converterBuffer()).create();
    }

    private void tickPrinting() {
        if (level != null && level.isClientSide) {
            return;
        }
        startNextJobIfPossible();
        progressPendingOutput();
        consumeMaterialInput();
        consumeInkInput();
    }

    private void startNextJobIfPossible() {
        if (!active || !pendingOutput.isEmpty() || !canMergeOutput()) {
            return;
        }
        final var costs = PrintData.computeCosts(data);
        if (costs.isEmpty()) {
            active = false;
            data = new PrintData();
            setChanged();
            return;
        }
        final int materialRequired = costs.get().material();
        final int inkRequired = costs.get().ink();
        totalRequiredEnergy = ModSettings.printCost();
        requiredEnergy = totalRequiredEnergy;
        if (amountMaterial >= materialRequired && amountInk >= inkRequired) {
            amountMaterial -= materialRequired;
            amountInk -= inkRequired;
            limit -= 1;
            pendingOutput = data.createItemStack();
            if (limit < 1) {
                active = false;
            }
            setChanged();
        }
    }

    private void progressPendingOutput() {
        if (pendingOutput.isEmpty()) {
            return;
        }
        if (requiredEnergy > 0D) {
            if (!(node() instanceof ComponentConnector connector)) {
                return;
            }
            final double want = Math.max(1D, Math.min(requiredEnergy, ModSettings.printerTickAmount()));
            final double remainingDelta = connector.changeBuffer(-want);
            final double consumed = AssemblerWork.energyConsumed(want, remainingDelta);
            if (consumed <= 0D) {
                return;
            }
            requiredEnergy = Math.max(0D, requiredEnergy - consumed);
        }
        if (requiredEnergy <= 0D) {
            releasePendingOutput();
        }
        setChanged();
    }

    private void releasePendingOutput() {
        final ItemStack result = items.get(SLOT_OUTPUT);
        if (result.isEmpty()) {
            items.set(SLOT_OUTPUT, pendingOutput.copy());
        } else if (ItemStack.isSameItemSameComponents(result, pendingOutput) && result.getCount() < result.getMaxStackSize()) {
            result.grow(1);
        } else {
            return;
        }
        pendingOutput = ItemStack.EMPTY;
        totalRequiredEnergy = 0D;
        requiredEnergy = 0D;
    }

    private boolean canMergeOutput() {
        final ItemStack result = items.get(SLOT_OUTPUT);
        final ItemStack output = data.createItemStack();
        return result.isEmpty() || ItemStack.isSameItemSameComponents(result, output);
    }

    private void consumeMaterialInput() {
        final int value = PrintData.materialValue(items.get(SLOT_MATERIAL));
        if (value > 0 && MAX_MATERIAL - amountMaterial >= value) {
            final ItemStack removed = removeItem(SLOT_MATERIAL, 1);
            if (!removed.isEmpty()) {
                amountMaterial += value;
                setChanged();
            }
        }
    }

    private void consumeInkInput() {
        final int value = PrintData.inkValue(items.get(SLOT_INK));
        if (value > 0 && MAX_INK - amountInk >= value) {
            final ItemStack removed = removeItem(SLOT_INK, 1);
            if (!removed.isEmpty()) {
                amountInk += value;
                setChanged();
            }
        }
    }

    private void removeNode() {
        if (node != null) {
            node.remove();
        }
    }
}
