package li.cil.oc.common.component;

import li.cil.oc.api.event.RobotUsedToolEvent;
import li.cil.oc.api.internal.Robot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

public final class RobotCommonHandler {
    private RobotCommonHandler() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(RobotCommonHandler::onRobotApplyDamageRate);
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
}
