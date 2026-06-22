package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.api.prefab.AbstractProvider;
import li.cil.oc.common.ModSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class NanomachineDisintegrationProvider extends AbstractProvider {
    private static final String PROVIDER_ID = "c4e7e3c2-8069-4fbb-b08e-74b1bddcdfe7";

    public NanomachineDisintegrationProvider() {
        super(PROVIDER_ID);
    }

    @Override
    public Iterable<Behavior> createBehaviors(final Player player) {
        return java.util.List.of(new DisintegrationBehavior(player));
    }

    @Override
    protected void writeBehaviorToNBT(final Behavior behavior, final CompoundTag nbt) {
    }

    @Override
    protected Behavior readBehaviorFromNBT(final Player player, final CompoundTag nbt) {
        return new DisintegrationBehavior(player);
    }

    private static final class DisintegrationBehavior implements Behavior {
        private final Player player;
        private final Map<BlockPos, SlowBreakInfo> breaking = new HashMap<>();

        private DisintegrationBehavior(final Player player) {
            this.player = player;
        }

        @Override
        public String getNameHint() {
            return "";
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable(final DisableReason reason) {
            clearProgress();
        }

        @Override
        public void update() {
            if (player == null || player.level().isClientSide() || player instanceof FakePlayer) {
                return;
            }
            final Controller controller = Nanomachines.getController(player);
            if (controller == null) {
                clearProgress();
                return;
            }
            final double range = ModSettings.nanomachinesDisintegrationRange() * controller.getInputCount(this);
            if (range <= 0D) {
                clearProgress();
                return;
            }
            scanBlocks(range);
        }

        private void scanBlocks(final double range) {
            final Level level = player.level();
            final long now = level.getGameTime();
            final int radius = (int) Math.ceil(range);
            final BlockPos origin = player.blockPosition();
            final Map<BlockPos, SlowBreakInfo> next = new HashMap<>();
            final Set<BlockPos> completed = new HashSet<>();
            for (int x = -radius; x <= radius; x++) {
                for (int y = 0; y <= radius * 2; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        final BlockPos pos = origin.offset(x, y, z);
                        final SlowBreakInfo existing = breaking.get(pos);
                        if (existing != null) {
                            if (existing.matches(level, player)) {
                                if (existing.isComplete(now)) {
                                    existing.finish(level, player);
                                    completed.add(pos);
                                } else {
                                    existing.updateProgress(level, now);
                                    next.put(pos, existing);
                                }
                            }
                        } else {
                            final SlowBreakInfo started = tryStart(level, pos, now);
                            if (started != null) {
                                next.put(pos, started);
                            }
                        }
                    }
                }
            }
            for (final Map.Entry<BlockPos, SlowBreakInfo> entry : breaking.entrySet()) {
                final BlockPos pos = entry.getKey();
                if (!next.containsKey(pos) && !completed.contains(pos) && entry.getValue().isComplete(now)) {
                    entry.getValue().finish(level, player);
                    completed.add(pos);
                }
            }
            for (final BlockPos pos : breaking.keySet()) {
                if (!next.containsKey(pos) && !completed.contains(pos)) {
                    level.destroyBlockProgress(pos.hashCode(), pos, -1);
                }
            }
            breaking.clear();
            breaking.putAll(next);
        }

        private SlowBreakInfo tryStart(final Level level, final BlockPos pos, final long now) {
            final BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(level, pos) < 0F || !canInteract(level, pos)) {
                return null;
            }
            final float progress = state.getDestroyProgress(player, level, pos);
            if (progress <= 0F) {
                return null;
            }
            final int timeToBreak = (int) (1F / progress);
            if (timeToBreak >= 20 * 30) {
                return null;
            }
            level.destroyBlockProgress(pos.hashCode(), pos, 0);
            return new SlowBreakInfo(now, now + timeToBreak, pos, player.getMainHandItem().copy(), state);
        }

        private boolean canInteract(final Level level, final BlockPos pos) {
            final PlayerInteractEvent.LeftClickBlock event = CommonHooks.onLeftClickBlock(
                player,
                pos,
                player.getDirection() == null ? Direction.NORTH : player.getDirection(),
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
            return !event.isCanceled()
                && event.getUseBlock() != TriState.FALSE
                && event.getUseItem() != TriState.FALSE
                && level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, Direction.UP, player.getMainHandItem());
        }

        private void clearProgress() {
            if (player == null) {
                breaking.clear();
                return;
            }
            final Level level = player.level();
            for (final BlockPos pos : breaking.keySet()) {
                level.destroyBlockProgress(pos.hashCode(), pos, -1);
            }
            breaking.clear();
        }
    }

    private record SlowBreakInfo(long timeStarted, long timeBroken, BlockPos pos, ItemStack originalTool, BlockState originalState) {
        private boolean matches(final Level level, final Player player) {
            return level.getBlockState(pos).equals(originalState) && matchingTool(player.getMainHandItem(), originalTool);
        }

        private boolean isComplete(final long now) {
            return timeBroken < now;
        }

        private void updateProgress(final Level level, final long now) {
            final long timeTotal = timeBroken - timeStarted;
            if (timeTotal > 0) {
                final long timeTaken = now - timeStarted;
                level.destroyBlockProgress(pos.hashCode(), pos, (int) (10L * timeTaken / timeTotal));
            }
        }

        private void finish(final Level level, final Player player) {
            level.destroyBlockProgress(pos.hashCode(), pos, -1);
            if (!level.getBlockState(pos).equals(originalState)) {
                return;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.gameMode.destroyBlock(pos);
            } else {
                level.destroyBlock(pos, true, player);
            }
        }

        private static boolean matchingTool(final ItemStack current, final ItemStack original) {
            if (current.isEmpty() && original.isEmpty()) {
                return true;
            }
            if (!ItemStack.isSameItemSameComponents(current, original)) {
                return false;
            }
            return current.isDamageableItem() || current.getDamageValue() == original.getDamageValue();
        }
    }
}
