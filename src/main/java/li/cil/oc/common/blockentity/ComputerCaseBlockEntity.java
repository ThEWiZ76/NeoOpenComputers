package li.cil.oc.common.blockentity;

import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class ComputerCaseBlockEntity extends BlockEntity implements MachineHost {
    private static final String TAG_MACHINE = "oc:machine";

    private final Machine machine;

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
        machine.load(tag.getCompound(TAG_MACHINE));
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
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
