package li.cil.oc.common.component;

import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.event.RobotUsedToolEvent;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.item.HoverUpgradeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;

public final class RobotCommonHandler {
    private RobotCommonHandler() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(RobotCommonHandler::onRobotMovePre);
        NeoForge.EVENT_BUS.addListener(RobotCommonHandler::onRobotApplyDamageRate);
    }

    static void onRobotMovePre(final RobotMoveEvent.Pre event) {
        if (!(event.agent instanceof Robot robot)) {
            return;
        }
        final int maxFlyingHeight = maxFlyingHeight(robot);
        if (maxFlyingHeight < 0) {
            return;
        }
        final Level level = robot.world();
        if (level == null) {
            return;
        }
        final BlockPos startPos = BlockPos.containing(robot.xPosition(), robot.yPosition(), robot.zPosition());
        final BlockPos targetPos = startPos.relative(event.direction);
        final boolean validMove = event.direction == Direction.DOWN
            || maxFlyingHeight >= level.getHeight()
            || hasAdjacentBlock(level, startPos)
            || hasAdjacentBlock(level, targetPos)
            || isWithinFlyingHeight(level, startPos, maxFlyingHeight);
        if (!validMove) {
            event.setCanceled(true);
        }
    }

    static void onRobotApplyDamageRate(final RobotUsedToolEvent.ApplyDamageRate event) {
        if (!(event.agent instanceof Robot) || event.toolBeforeUse == null || event.toolAfterUse == null) {
            return;
        }
        final ItemStack toolAfterUse = event.toolAfterUse;
        if (!toolAfterUse.isDamageableItem()) {
            return;
        }
        final int damage = toolAfterUse.getDamageValue() - event.toolBeforeUse.getDamageValue();
        if (damage <= 0) {
            return;
        }
        final double actualDamage = damage * event.getDamageRate();
        final int roundedDamage = roundedDamage(actualDamage, event.agent.player());
        final int repairedDamage = damage - roundedDamage;
        toolAfterUse.setDamageValue(toolAfterUse.getDamageValue() - repairedDamage);
    }

    private static int roundedDamage(final double actualDamage, final Player player) {
        final int floor = (int) Math.floor(actualDamage);
        final int ceil = (int) Math.ceil(actualDamage);
        if (floor == ceil) {
            return floor;
        }
        final double value = player == null ? 0D : player.getRandom().nextDouble();
        return value > 0.5D ? floor : ceil;
    }

    private static int maxFlyingHeight(final Robot robot) {
        int result = ModSettings.limitFlightHeight();
        for (int slot = 0; slot < robot.equipmentInventory().getContainerSize(); slot++) {
            result = Math.max(result, hoverFlightHeight(robot.equipmentInventory().getItem(slot)));
        }
        final int firstComponentSlot = robot.mainInventory().getContainerSize() + robot.equipmentInventory().getContainerSize();
        for (int slot = 0; slot < robot.componentCount(); slot++) {
            result = Math.max(result, hoverFlightHeight(robot.getItem(firstComponentSlot + slot)));
        }
        return result;
    }

    private static int hoverFlightHeight(final ItemStack stack) {
        if (stack != null && stack.getItem() instanceof HoverUpgradeItem hoverUpgrade) {
            return ModSettings.upgradeFlightHeight(hoverUpgrade.tier(stack));
        }
        return -1;
    }

    private static boolean hasAdjacentBlock(final Level level, final BlockPos pos) {
        for (final Direction side : Direction.values()) {
            final BlockPos neighbor = pos.relative(side);
            if (level.getBlockState(neighbor).isFaceSturdy(level, neighbor, side.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWithinFlyingHeight(final Level level, final BlockPos pos, final int maxFlyingHeight) {
        for (int offset = 1; offset <= maxFlyingHeight; offset++) {
            if (!level.isEmptyBlock(pos.below(offset))) {
                return true;
            }
        }
        return false;
    }
}
