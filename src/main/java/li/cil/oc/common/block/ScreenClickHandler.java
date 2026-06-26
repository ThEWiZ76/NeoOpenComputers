package li.cil.oc.common.block;

import li.cil.oc.common.blockentity.ScreenBlockEntity;
import net.minecraft.world.entity.player.Player;

final class ScreenClickHandler {
    static void clickScreen(final ScreenBlockEntity screen, final ScreenHitMapper.ScreenClick click, final Player player) {
        screen.mouseDown(click.x(), click.y(), 0, player);
    }

    private ScreenClickHandler() {
    }
}
