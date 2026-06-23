package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.menu.DisassemblerMenu;
import li.cil.oc.common.template.DisassemblerTemplates;
import li.cil.oc.common.util.DisassemblerWork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Map;

public class DisassemblerBlockEntity extends BlockEntity implements Environment, SidedEnvironment, Container, MenuProvider, DeviceInfo {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT_START = 1;
    public static final int OUTPUT_SLOT_COUNT = 9;
    public static final int CONTAINER_SIZE = 10;
    public static final double CONNECTOR_BUFFER_SIZE = 50D;
    private static final String TAG_NODE = "oc:node";
    private static final String TAG_QUEUE = "oc:queue";
    private static final String TAG_BUFFER = "oc:buffer";
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
        DeviceInfo.DeviceAttribute.Description, "Disassembler",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "Break.3R-100"
    );

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final ArrayDeque<ItemStack> queuedOutputs = new ArrayDeque<>();
    private Node node;
    private double disassemblyBuffer;

    public DisassemblerBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.DISASSEMBLER.get(), pos, blockState);
        OpenComputersApi.initialize();
        node = createNode(this);
    }

    public boolean canInsert(final ItemStack stack) {
        return !stack.isEmpty() && items.get(SLOT_INPUT).isEmpty() && DisassemblerTemplates.select(stack).isPresent();
    }

    public boolean canDisassemble() {
        return outputsFit(DisassemblerTemplates.disassemble(items.get(SLOT_INPUT)));
    }

    public boolean disassemble() {
        if (!canDisassemble()) {
            return false;
        }
        final ItemStack input = items.get(SLOT_INPUT);
        final ItemStack[] outputs = DisassemblerTemplates.disassemble(input);
        if (outputs == null || outputs.length == 0) {
            return false;
        }
        queuedOutputs.clear();
        for (final ItemStack output : outputs) {
            if (output == null || output.isEmpty()) {
                continue;
            }
            final ItemStack stack = output.copy();
            while (!stack.isEmpty()) {
                queuedOutputs.add(stack.split(1));
            }
        }
        if (queuedOutputs.isEmpty()) {
            return false;
        }
        items.set(SLOT_INPUT, ItemStack.EMPTY);
        disassemblyBuffer = 0D;
        setChanged();
        return true;
    }

    public boolean disassemble(final RandomSource random, final double breakChance) {
        final ItemStack input = items.get(SLOT_INPUT);
        final ItemStack[] outputs = survivingOutputs(DisassemblerTemplates.disassemble(input), random, breakChance);
        if (!outputsFit(outputs) && outputs.length > 0 && !hasAdjacentInventory()) {
            return false;
        }
        items.set(SLOT_INPUT, ItemStack.EMPTY);
        routeOutputs(outputs);
        setChanged();
        return true;
    }

    public static void serverTick(final net.minecraft.world.level.Level level, final BlockPos pos, final BlockState state, final DisassemblerBlockEntity disassembler) {
        disassembler.tickDisassembly();
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

    public boolean containsOutput(final Item item) {
        for (int slot = SLOT_OUTPUT_START; slot < CONTAINER_SIZE; slot++) {
            if (items.get(slot).is(item)) {
                return true;
            }
        }
        return false;
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
        return 1;
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return slot == SLOT_INPUT && canInsert(stack);
    }

    @Override
    public boolean stillValid(final Player player) {
        return !isRemoved();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.neoopencomputers.disassembler");
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory playerInventory, final Player player) {
        return new DisassemblerMenu(containerId, playerInventory, this);
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
    }

    public static double defaultBreakChance() {
        return ModSettings.disassemblerBreakChance();
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        if (tag.contains(TAG_NODE)) {
            node().load(tag.getCompound(TAG_NODE));
        }
        queuedOutputs.clear();
        final ListTag queue = tag.getList(TAG_QUEUE, CompoundTag.TAG_COMPOUND);
        for (int index = 0; index < queue.size(); index++) {
            ItemStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), queue.get(index))
                .result()
                .filter(stack -> !stack.isEmpty())
                .ifPresent(queuedOutputs::add);
        }
        disassemblyBuffer = tag.getDouble(TAG_BUFFER);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        if (node() != null) {
            final CompoundTag nodeTag = new CompoundTag();
            if (node().address() == null) {
                Network.joinNewNetwork(node());
            }
            node().save(nodeTag);
            tag.put(TAG_NODE, nodeTag);
        }
        final ListTag queue = new ListTag();
        for (final ItemStack stack : queuedOutputs) {
            ItemStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stack)
                .result()
                .ifPresent(queue::add);
        }
        tag.put(TAG_QUEUE, queue);
        tag.putDouble(TAG_BUFFER, disassemblyBuffer);
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

    private void tickDisassembly() {
        if (queuedOutputs.isEmpty() || !(node() instanceof li.cil.oc.api.network.Connector connector)) {
            return;
        }

        final double want = DisassemblerWork.energyToApply(disassemblyBuffer);
        if (want > 0D) {
            final double remainingDelta = connector.changeBuffer(-want);
            final double consumed = Math.clamp(want + remainingDelta, 0D, want);
            if (consumed <= 0D) {
                return;
            }
            disassemblyBuffer += consumed;
        }

        final RandomSource random = level == null ? RandomSource.create() : level.random;
        while (!queuedOutputs.isEmpty() && DisassemblerWork.canReleaseOutput(disassemblyBuffer)) {
            disassemblyBuffer = DisassemblerWork.remainingBufferAfterRelease(disassemblyBuffer);
            final ItemStack output = queuedOutputs.removeFirst();
            if (random.nextDouble() >= defaultBreakChance()) {
                routeOutputs(new ItemStack[]{output});
            }
        }
        if (queuedOutputs.isEmpty()) {
            disassemblyBuffer = 0D;
        }
        setChanged();
    }

    private static ItemStack[] survivingOutputs(final ItemStack[] outputs, final RandomSource random, final double breakChance) {
        if (outputs == null || outputs.length == 0) {
            return new ItemStack[0];
        }
        final java.util.ArrayList<ItemStack> surviving = new java.util.ArrayList<>();
        final double chance = Math.max(0D, Math.min(1D, breakChance));
        for (ItemStack output : outputs) {
            if (output == null || output.isEmpty()) {
                continue;
            }
            final ItemStack stack = output.copy();
            int survivors = 0;
            for (int count = 0; count < stack.getCount(); count++) {
                if (random.nextDouble() >= chance) {
                    survivors++;
                }
            }
            if (survivors > 0) {
                stack.setCount(survivors);
                surviving.add(stack);
            }
        }
        return surviving.toArray(ItemStack[]::new);
    }

    private void routeOutputs(final ItemStack[] outputs) {
        for (ItemStack output : outputs) {
            ItemStack remaining = output.copy();
            remaining = insertIntoAdjacentInventory(remaining);
            if (!remaining.isEmpty()) {
                remaining = insertIntoInternalOutput(remaining);
            }
            if (!remaining.isEmpty() && level != null) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D, remaining);
            }
        }
    }

    private boolean hasAdjacentInventory() {
        if (level == null) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(direction)) instanceof Container) {
                return true;
            }
        }
        return false;
    }

    private ItemStack insertIntoAdjacentInventory(final ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return stack;
        }
        ItemStack remaining = stack;
        for (Direction direction : Direction.values()) {
            final BlockEntity blockEntity = level.getBlockEntity(worldPosition.relative(direction));
            if (blockEntity instanceof Container container) {
                remaining = insertInto(container, remaining);
                if (remaining.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return remaining;
    }

    private ItemStack insertIntoInternalOutput(final ItemStack stack) {
        ItemStack remaining = stack;
        for (int slot = SLOT_OUTPUT_START; slot < CONTAINER_SIZE; slot++) {
            if (items.get(slot).isEmpty()) {
                items.set(slot, remaining.copy());
                return ItemStack.EMPTY;
            }
        }
        return remaining;
    }

    private static ItemStack insertInto(final Container container, final ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }
            final ItemStack existing = container.getItem(slot);
            if (existing.isEmpty()) {
                final ItemStack inserted = remaining.copy();
                inserted.setCount(Math.min(remaining.getCount(), Math.min(inserted.getMaxStackSize(), container.getMaxStackSize())));
                if (container.canPlaceItem(slot, inserted)) {
                    container.setItem(slot, inserted);
                    remaining.shrink(inserted.getCount());
                }
            } else if (ItemStack.isSameItemSameComponents(existing, remaining) && existing.getCount() < Math.min(existing.getMaxStackSize(), container.getMaxStackSize())) {
                final int transferable = Math.min(remaining.getCount(), Math.min(existing.getMaxStackSize(), container.getMaxStackSize()) - existing.getCount());
                existing.grow(transferable);
                container.setItem(slot, existing);
                remaining.shrink(transferable);
            }
        }
        return remaining;
    }

    private boolean outputsFit(final ItemStack[] outputs) {
        if (outputs == null || outputs.length == 0 || outputs.length > OUTPUT_SLOT_COUNT) {
            return false;
        }
        for (int slot = SLOT_OUTPUT_START; slot < CONTAINER_SIZE; slot++) {
            if (!items.get(slot).isEmpty()) {
                return false;
            }
        }
        for (ItemStack output : outputs) {
            if (output == null || output.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidSlot(final int slot) {
        return slot >= 0 && slot < CONTAINER_SIZE;
    }

    private static Node createNode(final Environment host) {
        return Network.newNode(host, Visibility.None)
            .withConnector(CONNECTOR_BUFFER_SIZE)
            .create();
    }

    private void removeNode() {
        if (node != null) {
            node.remove();
        }
    }
}
