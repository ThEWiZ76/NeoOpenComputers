package li.cil.oc.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import li.cil.oc.common.menu.TerminalMenu;
import li.cil.oc.common.network.TerminalScreenSnapshotPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

@SuppressWarnings("deprecation")
public class ScreenBlock extends Block implements EntityBlock {
    public static final MapCodec<ScreenBlock> CODEC = simpleCodec(ScreenBlock::new);
    public static final DirectionProperty PITCH = DirectionProperty.create("pitch", Direction.NORTH, Direction.UP, Direction.DOWN);
    public static final DirectionProperty YAW = DirectionProperty.create("yaw", Direction.Plane.HORIZONTAL);

    private final int tier;

    public ScreenBlock(final BlockBehaviour.Properties properties) {
        this(properties, 0);
    }

    public ScreenBlock(final BlockBehaviour.Properties properties, final int tier) {
        super(properties);
        this.tier = Math.clamp(tier, 0, 2);
        registerDefaultState(stateDefinition.any()
            .setValue(PITCH, Direction.NORTH)
            .setValue(YAW, Direction.NORTH));
    }

    public int tier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new ScreenBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.SCREEN.get()) {
            return null;
        }
        return (tickerLevel, pos, blockState, blockEntity) -> ((ScreenBlockEntity) blockEntity).update();
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        BlockNetworkConnector.joinIfServer(level, pos);
    }

    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block block, final BlockPos fromPos, final boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        BlockNetworkConnector.joinIfServer(level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(
        final BlockState state,
        final Level level,
        final BlockPos pos,
        final Player player,
        final BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof ScreenBlockEntity screen) {
            if (screen.hasKeyboard(player) && !player.isShiftKeyDown()) {
                return openPhysicalTerminal(screen, player);
            }
            final ScreenHitMapper.ScreenClick click = ScreenHitMapper.screenCoordinates(state, pos, hitResult, screen.renderWidth(), screen.renderHeight());
            if (click == null) {
                return InteractionResult.PASS;
            }
            ScreenClickHandler.clickScreen(screen, click, player);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    public static InteractionResult openPhysicalTerminal(final ScreenBlockEntity screen, final Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        final var openedContainerId = serverPlayer.openMenu(new SimpleMenuProvider(
            (containerId, playerInventory, menuPlayer) -> new TerminalMenu(containerId, playerInventory, screen.terminalSnapshot(), screen),
            Component.translatable(screen.getBlockState().getBlock().getDescriptionId())));
        openedContainerId.ifPresent(containerId -> PacketDistributor.sendToPlayer(
            serverPlayer,
            new TerminalScreenSnapshotPayload(containerId, screen.terminalSnapshot())));
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Direction lookDirection = context.getNearestLookingDirection();
        final Direction pitch = lookDirection.getAxis().isVertical() ? lookDirection : Direction.NORTH;
        final Direction yaw = context.getHorizontalDirection();
        return defaultBlockState()
            .setValue(PITCH, pitch)
            .setValue(YAW, yaw);
    }

    @Override
    protected BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(YAW, rotation.rotate(state.getValue(YAW)));
    }

    @Override
    protected BlockState mirror(final BlockState state, final Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(YAW)));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PITCH, YAW);
    }

    public static Direction facing(final BlockState state) {
        final Direction pitch = pitch(state);
        return pitch.getAxis().isVertical() ? pitch : yaw(state);
    }

    public static Direction up(final BlockState state) {
        final Direction pitch = pitch(state);
        return pitch.getAxis().isVertical() ? yaw(state) : Direction.UP;
    }

    public static Direction pitch(final BlockState state) {
        return state != null && state.hasProperty(PITCH) ? state.getValue(PITCH) : Direction.NORTH;
    }

    public static Direction yaw(final BlockState state) {
        return state != null && state.hasProperty(YAW) ? state.getValue(YAW) : Direction.NORTH;
    }
}
