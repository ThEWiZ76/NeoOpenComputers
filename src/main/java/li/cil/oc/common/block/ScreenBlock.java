package li.cil.oc.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import li.cil.oc.common.menu.TerminalMenu;
import li.cil.oc.common.network.TerminalNetworking;
import li.cil.oc.common.network.TerminalScreenSnapshotPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
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

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

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
        notifyConnectedScreensForClientUpdate(level, pos, state);
    }

    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block block, final BlockPos fromPos, final boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        BlockNetworkConnector.joinIfServer(level, pos);
        notifyConnectedScreensForClientUpdate(level, pos, state);
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
            final ScreenBlockEntity origin = screen.originScreen();
            final boolean playerIsSneaking = player != null && player.isShiftKeyDown();
            if (shouldOpenPhysicalTerminal(origin.hasKeyboard(player), playerIsSneaking, origin.isTouchModeInverted())) {
                return openPhysicalTerminal(origin, player);
            }
            final ScreenHitMapper.ScreenClick click = screen.renderBlockWidth() > 1 || screen.renderBlockHeight() > 1
                ? ScreenHitMapper.screenCoordinates(
                    state,
                    pos,
                    hitResult,
                    origin.renderWidth(),
                    origin.renderHeight(),
                    screen.renderBlockWidth(),
                    screen.renderBlockHeight(),
                    screen.localBlockX(),
                    screen.localBlockY())
                : ScreenHitMapper.screenCoordinates(state, pos, hitResult, origin.renderWidth(), origin.renderHeight());
            if (click == null) {
                return InteractionResult.PASS;
            }
            ScreenClickHandler.clickScreen(origin, click, player);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(
        final ItemStack stack,
        final BlockState state,
        final Level level,
        final BlockPos pos,
        final Player player,
        final InteractionHand hand,
        final BlockHitResult hitResult) {
        if (!(stack.getItem() instanceof DyeItem dyeItem) || !(level.getBlockEntity(pos) instanceof ScreenBlockEntity screen)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            screen.setRenderColor(dyeItem.getDyeColor());
            if (player != null) {
                player.swing(hand);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    public static InteractionResult openPhysicalTerminal(final ScreenBlockEntity screen, final Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        final var openedContainerId = serverPlayer.openMenu(new SimpleMenuProvider(
            (containerId, playerInventory, menuPlayer) -> new TerminalMenu(containerId, playerInventory, screen.terminalSnapshot(), screen),
            Component.translatable(screen.getBlockState().getBlock().getDescriptionId())));
        openedContainerId.ifPresent(containerId -> TerminalNetworking.sendToPlayerIfSupported(
            serverPlayer,
            new TerminalScreenSnapshotPayload(containerId, screen.terminalSnapshot())));
        return InteractionResult.CONSUME;
    }

    public static boolean shouldOpenPhysicalTerminal(final boolean hasKeyboard, final boolean playerIsSneaking, final boolean touchModeInverted) {
        return hasKeyboard && playerIsSneaking == touchModeInverted;
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Direction lookDirection = context.getNearestLookingDirection();
        final Direction pitch = lookDirection.getAxis().isVertical() ? lookDirection.getOpposite() : Direction.NORTH;
        final Direction yaw = context.getHorizontalDirection().getOpposite();
        final BlockState fallback = defaultBlockState()
            .setValue(PITCH, pitch)
            .setValue(YAW, yaw);
        return inheritConnectedScreenState(context, fallback);
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

    private static BlockState inheritConnectedScreenState(final BlockPlaceContext context, final BlockState fallback) {
        final BlockPos pos = context.getClickedPos();
        if (!context.replacingClickedOnBlock()) {
            final BlockPos clickedPos = pos.relative(context.getClickedFace().getOpposite());
            final BlockState clickedState = context.getLevel().getBlockState(clickedPos);
            final BlockState inherited = inheritConnectedScreenState(fallback, clickedState, pos, clickedPos);
            if (inherited != fallback) {
                return inherited;
            }
        }
        for (final Direction direction : Direction.values()) {
            final BlockPos neighborPos = pos.relative(direction);
            final BlockState neighborState = context.getLevel().getBlockState(neighborPos);
            final BlockState inherited = inheritConnectedScreenState(fallback, neighborState, pos, neighborPos);
            if (inherited != fallback) {
                return inherited;
            }
        }
        return fallback;
    }

    public static BlockState inheritConnectedScreenState(
        final BlockState fallback,
        final BlockState neighborState,
        final BlockPos placedPos,
        final BlockPos neighborPos) {
        if (!(fallback.getBlock() instanceof ScreenBlock screenBlock)
            || !(neighborState.getBlock() instanceof ScreenBlock neighborScreenBlock)
            || screenBlock.tier() != neighborScreenBlock.tier()) {
            return fallback;
        }

        final Direction direction = Direction.fromDelta(
            Integer.compare(placedPos.getX(), neighborPos.getX()),
            Integer.compare(placedPos.getY(), neighborPos.getY()),
            Integer.compare(placedPos.getZ(), neighborPos.getZ()));
        if (direction == null || placedPos.distManhattan(neighborPos) != 1) {
            return fallback;
        }

        final Direction right = localRight(neighborState);
        final Direction up = up(neighborState);
        if (direction == right || direction == right.getOpposite() || direction == up || direction == up.getOpposite()) {
            return fallback
                .setValue(PITCH, pitch(neighborState))
                .setValue(YAW, yaw(neighborState));
        }
        return fallback;
    }

    public static Direction localRight(final BlockState state) {
        return localRight(yaw(state));
    }

    public static Direction localRight(final Direction yaw) {
        return yaw == null ? Direction.WEST : yaw.getCounterClockWise();
    }

    private static void notifyConnectedScreensForClientUpdate(final Level level, final BlockPos pos, final BlockState state) {
        if (level == null || level.isClientSide || !(state.getBlock() instanceof ScreenBlock screenBlock)) {
            return;
        }

        final Direction pitch = pitch(state);
        final Direction yaw = yaw(state);
        final Direction right = localRight(state);
        final Direction up = up(state);
        final ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        final Set<BlockPos> visited = new HashSet<>();
        pending.add(pos);
        while (!pending.isEmpty()) {
            final BlockPos current = pending.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            final BlockState currentState = level.getBlockState(current);
            if (!(currentState.getBlock() instanceof ScreenBlock currentScreen)
                || currentScreen.tier() != screenBlock.tier()
                || pitch(currentState) != pitch
                || yaw(currentState) != yaw) {
                continue;
            }
            level.sendBlockUpdated(current, currentState, currentState, Block.UPDATE_CLIENTS);
            pending.add(current.relative(right));
            pending.add(current.relative(right.getOpposite()));
            pending.add(current.relative(up));
            pending.add(current.relative(up.getOpposite()));
        }
    }
}
