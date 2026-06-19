package li.cil.oc.common.blockentity;

import li.cil.oc.api.internal.Case;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.block.ComputerCaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class ComputerCaseBlockEntity extends BlockEntity implements Case {
    private static final String TAG_COLOR = "oc:color";
    private static final String TAG_MACHINE = "oc:machine";

    private final Machine machine;
    private int color;

    public ComputerCaseBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.COMPUTER_CASE.get(), pos, blockState);
        OpenComputersApi.initialize();
        machine = li.cil.oc.api.Machine.create(this);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final ComputerCaseBlockEntity blockEntity) {
        blockEntity.tickServer();
    }

    @Override
    public Machine machine() {
        return machine;
    }

    @Override
    public Node node() {
        return machine.node();
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
        return List.of();
    }

    @Override
    public int componentSlot(final String address) {
        return -1;
    }

    @Override
    public void onMachineConnect(final Node node) {
    }

    @Override
    public void onMachineDisconnect(final Node node) {
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
    public int getColor() {
        return color;
    }

    @Override
    public void setColor(final int value) {
        color = value;
        setChanged();
    }

    @Override
    public boolean controlsConnectivity() {
        return false;
    }

    @Override
    public Direction facing() {
        final BlockState state = getBlockState();
        if (state.getBlock() instanceof ComputerCaseBlock && state.hasProperty(ComputerCaseBlock.FACING)) {
            return state.getValue(ComputerCaseBlock.FACING);
        }
        return Direction.NORTH;
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
    public int tier() {
        return 0;
    }

    @Override
    public int getContainerSize() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getItem(final int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(final int slot, final int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
    }

    @Override
    public boolean stillValid(final Player player) {
        return !isRemoved();
    }

    @Override
    public void clearContent() {
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
        color = tag.getInt(TAG_COLOR);
        machine.load(tag.getCompound(TAG_MACHINE));
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_COLOR, color);
        final CompoundTag machineTag = new CompoundTag();
        machine.save(machineTag);
        tag.put(TAG_MACHINE, machineTag);
    }

    private void tickServer() {
    }

    private void removeMachineNode() {
        if (machine.node() != null) {
            machine.node().remove();
        }
    }
}
