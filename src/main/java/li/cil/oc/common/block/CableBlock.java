package li.cil.oc.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.DyeColors;
import li.cil.oc.common.blockentity.CableBlockEntity;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;

@SuppressWarnings("deprecation")
public class CableBlock extends Block implements EntityBlock {
    public static final MapCodec<CableBlock> CODEC = simpleCodec(CableBlock::new);
    public static final Map<Direction, EnumProperty<Connection>> CONNECTIONS = connectionProperties();
    private static final VoxelShape[] SHAPES = shapes();

    public enum Connection implements StringRepresentable {
        NONE, CABLE, DEVICE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public CableBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        var state = stateDefinition.any();
        for (final var property : CONNECTIONS.values()) {
            state = state.setValue(property, Connection.NONE);
        }
        registerDefaultState(state);
    }

    private static Map<Direction, EnumProperty<Connection>> connectionProperties() {
        final var properties = new EnumMap<Direction, EnumProperty<Connection>>(Direction.class);
        for (final var side : Direction.values()) {
            properties.put(side, EnumProperty.create(side.getName(), Connection.class));
        }
        return Map.copyOf(properties);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        for (final var property : CONNECTIONS.values()) builder.add(property);
    }

    private static VoxelShape[] shapes() {
        final var center = Block.box(6, 6, 6, 10, 10, 10);
        final VoxelShape[] arms = {
            Block.box(6, 0, 6, 10, 6, 10), Block.box(6, 10, 6, 10, 16, 10),
            Block.box(6, 6, 0, 10, 10, 6), Block.box(6, 6, 10, 10, 10, 16),
            Block.box(0, 6, 6, 6, 10, 10), Block.box(10, 6, 6, 16, 10, 10)
        };
        final var result = new VoxelShape[64];
        for (int mask = 0; mask < result.length; mask++) {
            var shape = center;
            for (final var side : Direction.values()) {
                if ((mask & (1 << side.ordinal())) != 0) shape = Shapes.or(shape, arms[side.ordinal()]);
            }
            result[mask] = shape;
        }
        return result;
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        int mask = 0;
        for (final var side : Direction.values()) {
            if (state.getValue(CONNECTIONS.get(side)) != Connection.NONE) mask |= 1 << side.ordinal();
        }
        return SHAPES[mask];
    }

    public static void refreshConnections(final Level level, final BlockPos pos) {
        if (level.isClientSide) return;
        refreshConnectionState(level, pos);
        for (final var side : Direction.values()) refreshConnectionState(level, pos.relative(side));
    }

    private static void refreshConnectionState(final Level level, final BlockPos pos) {
        if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof CableBlockEntity cable) || cable.isRemoved()) return;
        final var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CableBlock)) return;
        var updated = state;
        for (final var side : Direction.values()) {
            Connection connection = Connection.NONE;
            final var neighborPos = pos.relative(side);
            if (level.hasChunkAt(neighborPos) && cable.canConnect(side)) {
                final var neighbor = level.getBlockEntity(neighborPos);
                if (neighbor != null && !neighbor.isRemoved()) {
                    final boolean connects = neighbor instanceof SidedEnvironment sided
                        ? sided.canConnect(side.getOpposite()) && sided.sidedNode(side.getOpposite()) != null
                        : neighbor instanceof Environment environment && environment.node() != null;
                    if (connects) connection = neighbor instanceof CableBlockEntity ? Connection.CABLE : Connection.DEVICE;
                }
            }
            updated = updated.setValue(CONNECTIONS.get(side), connection);
        }
        if (updated != state) level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new CableBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, final LivingEntity placer, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            cable.setColor(CableBlockEntity.itemColor(stack));
        }
    }

    @Override
    public ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state) {
        return level.getBlockEntity(pos) instanceof CableBlockEntity cable ? cable.createItemStack() : new ItemStack(this);
    }

    @Override
    protected List<ItemStack> getDrops(final BlockState state, final LootParams.Builder params) {
        final var drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof CableBlockEntity cable) {
            for (final var stack : drops) {
                if (stack.is(asItem())) {
                    stack.applyComponents(cable.createItemStack().getComponentsPatch());
                }
            }
        }
        return drops;
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level, final BlockPos pos,
                                             final Player player, final InteractionHand hand, final BlockHitResult hit) {
        final var dye = DyeColors.colorOf(stack);
        if (dye == null || !(level.getBlockEntity(pos) instanceof CableBlockEntity cable)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            cable.setColor(dye.getTextureDiffuseColor());
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(state.getBlock())) {
            BlockNetworkConnector.joinIfServer(level, pos);
            refreshConnections(level, pos);
        }
    }

    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block block, final BlockPos fromPos, final boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        BlockNetworkConnector.joinIfServer(level, pos);
        refreshConnections(level, pos);
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            cable.removeNode();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
