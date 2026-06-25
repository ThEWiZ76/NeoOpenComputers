package li.cil.oc.common.blockentity;

import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.block.PrintBlock;
import li.cil.oc.common.item.data.PrintData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PrintBlockEntity extends BlockEntity {
    private static final String TAG_DATA = "data";
    private static final String TAG_STATE = "state";
    private static final AABB UNIT_BOUNDS = new AABB(0D, 0D, 0D, 1D, 1D, 1D);

    private PrintData data = new PrintData();
    private boolean activeState;
    private AABB boundsOff = UNIT_BOUNDS;
    private AABB boundsOn = UNIT_BOUNDS;
    private VoxelShape shapeOff = Shapes.block();
    private VoxelShape shapeOn = Shapes.block();

    public PrintBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.PRINT.get(), pos, blockState);
    }

    public PrintData data() {
        return data;
    }

    public void loadFromStack(final ItemStack stack) {
        data = new PrintData(stack);
        activeState = false;
        updateBounds();
        notifyUpdated();
    }

    public ItemStack createItemStack() {
        return data.createItemStack();
    }

    public boolean activate() {
        if (!data.hasActiveState()) {
            return false;
        }
        if (!activeState || !data.isButtonMode()) {
            toggleState();
            return true;
        }
        return false;
    }

    public void releaseButtonState() {
        if (activeState && data.isButtonMode()) {
            toggleState();
        }
    }

    public boolean isActiveState() {
        return activeState;
    }

    public int redstoneSignal() {
        return data.emitRedstone(activeState) ? data.redstoneLevel() : 0;
    }

    public int lightLevel() {
        return data.lightLevel();
    }

    public AABB bounds() {
        updateBounds();
        return activeState ? boundsOn : boundsOff;
    }

    public VoxelShape shape() {
        updateBounds();
        return activeState ? shapeOn : shapeOff;
    }

    public VoxelShape collisionShape() {
        updateBounds();
        if (activeState ? data.isNoclipOn() : data.isNoclipOff()) {
            return Shapes.empty();
        }
        return shape();
    }

    public boolean isSideSolid(final Direction side) {
        final Iterable<PrintData.Shape> shapes = activeState ? data.stateOn() : data.stateOff();
        for (PrintData.Shape shape : shapes) {
            final AABB bounds = rotateTowardsFacing(shape.bounds());
            final boolean fullX = bounds.minX == 0D && bounds.maxX == 1D;
            final boolean fullY = bounds.minY == 0D && bounds.maxY == 1D;
            final boolean fullZ = bounds.minZ == 0D && bounds.maxZ == 1D;
            if (switch (side) {
                case DOWN -> bounds.minY == 0D && fullX && fullZ;
                case UP -> bounds.maxY == 1D && fullX && fullZ;
                case NORTH -> bounds.minZ == 0D && fullX && fullY;
                case SOUTH -> bounds.maxZ == 1D && fullX && fullY;
                case WEST -> bounds.minX == 0D && fullY && fullZ;
                case EAST -> bounds.maxX == 1D && fullY && fullZ;
            }) {
                return true;
            }
        }
        return false;
    }

    public void load(final CompoundTag tag) {
        data = new PrintData();
        data.load(tag.getCompound(TAG_DATA));
        activeState = tag.getBoolean(TAG_STATE);
        updateBounds();
    }

    public void save(final CompoundTag tag) {
        final CompoundTag dataTag = new CompoundTag();
        data.save(dataTag);
        tag.put(TAG_DATA, dataTag);
        tag.putBoolean(TAG_STATE, activeState);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        load(tag);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        save(tag);
    }

    private void toggleState() {
        activeState = !activeState;
        notifyUpdated();
    }

    private void updateBounds() {
        boundsOff = unionRotatedBounds(data.stateOff());
        boundsOn = unionRotatedBounds(data.stateOn());
        shapeOff = buildShape(data.stateOff());
        shapeOn = buildShape(data.stateOn());
    }

    private AABB unionRotatedBounds(final Iterable<PrintData.Shape> shapes) {
        AABB result = null;
        for (PrintData.Shape shape : shapes) {
            final AABB rotated = rotateTowardsFacing(shape.bounds());
            result = result == null ? rotated : result.minmax(rotated);
        }
        return result == null || volume(result) == 0D ? UNIT_BOUNDS : result;
    }

    private VoxelShape buildShape(final Iterable<PrintData.Shape> shapes) {
        VoxelShape result = Shapes.empty();
        for (PrintData.Shape shape : shapes) {
            result = Shapes.or(result, Shapes.create(rotateTowardsFacing(shape.bounds())));
        }
        return result.isEmpty() ? Shapes.block() : result;
    }

    private AABB rotateTowardsFacing(final AABB bounds) {
        return switch (facing()) {
            case EAST -> new AABB(bounds.minZ, bounds.minY, 1D - bounds.maxX, bounds.maxZ, bounds.maxY, 1D - bounds.minX);
            case NORTH -> new AABB(1D - bounds.maxX, bounds.minY, 1D - bounds.maxZ, 1D - bounds.minX, bounds.maxY, 1D - bounds.minZ);
            case WEST -> new AABB(1D - bounds.maxZ, bounds.minY, bounds.minX, 1D - bounds.minZ, bounds.maxY, bounds.maxX);
            default -> bounds;
        };
    }

    private Direction facing() {
        final BlockState state = getBlockState();
        return state.hasProperty(PrintBlock.FACING) ? state.getValue(PrintBlock.FACING) : Direction.SOUTH;
    }

    private static double volume(final AABB bounds) {
        return (bounds.maxX - bounds.minX) * (bounds.maxY - bounds.minY) * (bounds.maxZ - bounds.minZ);
    }

    private void notifyUpdated() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            for (Direction direction : Direction.values()) {
                level.updateNeighborsAt(worldPosition.relative(direction), getBlockState().getBlock());
            }
        }
    }
}
